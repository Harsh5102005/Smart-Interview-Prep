package com.interview.ai.service;

import com.interview.ai.entities.Interview;
import com.interview.ai.entities.Question;
import com.interview.ai.repository.InterviewRepository;
import com.interview.ai.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AIService aiService;

    public Question generateQuestion(Long interviewId) {

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow();

        String role = interview.getRole();
        String difficulty = normalizeDifficulty(interview.getDifficulty());

        if (role == null || role.isBlank()) {
            role = "Java developer";
        }

        List<Question> previousQuestions = questionRepository.findByInterviewIdOrderByIdAsc(interviewId);
        Set<String> previousQuestionTexts = previousQuestions.stream()
                .map(Question::getQuestionText)
                .map(this::normalize)
                .collect(Collectors.toSet());

        String aiQuestion = "";
        for (int attempt = 1; attempt <= 3; attempt++) {
            aiQuestion = cleanText(aiService.callAI(buildPrompt(role, difficulty, previousQuestions, attempt)));
            if (!aiQuestion.isBlank() && !previousQuestionTexts.contains(normalize(aiQuestion))) {
                break;
            }
            aiQuestion = "";
        }

        if (aiQuestion.isBlank()) {
            aiQuestion = fallbackQuestion(previousQuestions.size(), difficulty, role);
        }

        Question question = new Question();
        question.setQuestionText(aiQuestion);
        question.setInterview(interview);

        return questionRepository.save(question);
    }

    private String buildPrompt(String role, String difficulty, List<Question> previousQuestions, int attempt) {
        String previous = previousQuestions.isEmpty()
                ? "None"
                : previousQuestions.stream()
                .map(Question::getQuestionText)
                .map(text -> "- " + text)
                .collect(Collectors.joining("\n"));

        String questionType = questionType(previousQuestions.size(), attempt);

        return "Generate ONE unique " + difficulty + " level interview question for the exact role: " + role + ".\n" +
                "The question must be about " + role + " skills only. Do not switch to Java unless the role itself asks for Java.\n" +
                difficultyRules(difficulty) + "\n" +
                "Question type for this request: " + questionType + ".\n" +
                questionTypeRules(questionType) + "\n" +
                "Do not repeat or closely rephrase any previous question.\n" +
                "Previous questions:\n" + previous + "\n" +
                "Use a different topic from the selected role than the previous questions when possible.\n" +
                "Question number: " + (previousQuestions.size() + 1) + "\n" +
                "Variation attempt: " + attempt + "\n" +
                "Do NOT include headings, difficulty labels, numbering, markdown, or formatting like **.\n" +
                "Return only the question in plain text.";
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "medium";
        }

        String normalized = difficulty.toLowerCase().trim();
        return switch (normalized) {
            case "easy", "medium", "hard", "advanced" -> normalized;
            default -> "medium";
        };
    }

    private String difficultyRules(String difficulty) {
        return switch (difficulty) {
            case "easy" -> "Difficulty rules: ask a beginner-friendly question about one basic concept from the selected role. " +
                    "Avoid algorithms, system design, low-level internals, advanced performance tuning, complex framework internals, and code-writing tasks. " +
                    "The question should be answerable in 2-4 simple sentences.";
            case "hard" -> "Difficulty rules: ask a senior-level question that requires tradeoffs, edge cases, or deeper role-specific reasoning. " +
                    "It may involve design, performance, debugging, architecture, or testing when relevant to the role, but keep it answerable verbally.";
            case "advanced" -> "Difficulty rules: ask an expert-level question involving role-specific internals, production tradeoffs, architecture, failure analysis, or advanced debugging.";
            default -> "Difficulty rules: ask an intermediate question. It can include practical examples, but should not require writing a full program.";
        };
    }

    private String questionType(int previousCount, int attempt) {
        List<String> types = List.of(
                "theory/concept",
                "practical explanation",
                "comparison",
                "scenario reasoning"
        );
        return types.get((previousCount + attempt - 1) % types.size());
    }

    private String questionTypeRules(String questionType) {
        return switch (questionType) {
            case "theory/concept" -> "Ask what a concept means, why it exists, or how it works. Do not ask the candidate to write code or implement a method.";
            case "comparison" -> "Ask the candidate to compare two related concepts from the selected role. Do not ask for code.";
            case "scenario reasoning" -> "Ask what the candidate would choose or explain in a small realistic situation. Do not ask for a full implementation.";
            default -> "Ask for a practical verbal explanation. A small example is okay, but do not ask for a complete code solution.";
        };
    }

    private String cleanText(String text) {
        if (text == null) return "";

        return text
                .replaceAll("\\*\\*", "")
                .replaceAll("(?i)^question\\s*\\d*\\s*[:.)-]\\s*", "")
                .replaceAll("(?i)null difficulty level[:\\-]*", "")
                .trim();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String fallbackQuestion(int previousCount, String difficulty, String role) {
        List<String> topics = fallbackTopics(difficulty);

        String topic = topics.get(previousCount % topics.size());
        return "For a " + role + ", " + topic;
    }

    private List<String> fallbackTopics(String difficulty) {
        return switch (difficulty) {
            case "easy" -> List.of(
                    "what is one basic concept every beginner in this role should understand?",
                    "what is the purpose of a common tool or language feature used in this role?",
                    "how would you explain a simple workflow used in this role?",
                    "what is the difference between two beginner-level concepts in this role?",
                    "why is clear structure important when working in this role?",
                    "what is a common beginner mistake in this role?"
            );
            case "hard" -> List.of(
                    "what tradeoffs would you consider when solving a complex problem in this role?",
                    "how would you debug a production issue related to this role?",
                    "how would you choose between two valid approaches in this role?",
                    "how would you keep work maintainable when requirements become complex?",
                    "what risks would you look for before shipping a role-specific change?"
            );
            case "advanced" -> List.of(
                    "how would you diagnose a difficult production issue in this role?",
                    "what architecture tradeoffs matter most for advanced work in this role?",
                    "how would you design a resilient solution for a failure-prone workflow?",
                    "how would you evaluate performance bottlenecks in this role?",
                    "what advanced mistakes can create long-term maintenance problems in this role?"
            );
            default -> List.of(
                    "explain an important concept used in this role with a practical example.",
                    "what is the difference between two commonly confused concepts in this role?",
                    "how would you approach a typical task in this role?",
                    "what quality checks matter before completing work in this role?",
                    "how would you explain a common tool or pattern used in this role?"
            );
        };
    }
}
