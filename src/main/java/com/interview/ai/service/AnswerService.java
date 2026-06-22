package com.interview.ai.service;

import com.interview.ai.entities.Answer;
import com.interview.ai.entities.Question;
import com.interview.ai.repository.AnswerRepository;
import com.interview.ai.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AIService aiService;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for", "from",
            "how", "in", "is", "it", "of", "on", "or", "the", "to", "what", "when", "where", "why",
            "with", "you", "your", "explain", "define", "describe", "difference", "between"
    );
    private static final Pattern UNCERTAIN_ANSWER_PATTERN = Pattern.compile(
            "\\b(i do not know|i dont know|dont know|do not know|no idea|not sure|cannot answer|cant answer|skip|pass)\\b"
    );

    public Answer submitAnswer(Long questionId, String answerText) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow();

        String submittedAnswer = answerText == null ? "" : answerText.trim();

        if (isNonAnswer(submittedAnswer)) {
            return saveAnswer(question, submittedAnswer, 0,
                    "The answer does not provide enough information to evaluate. Please explain the concept in your own words.");
        }

        if (isQuestionRepeated(question.getQuestionText(), submittedAnswer)) {
            return saveAnswer(question, submittedAnswer, 0,
                    "Repeating the question is not an answer. Explain the concept or reasoning in your own words.");
        }

        if (isLikelyUnrelated(question.getQuestionText(), submittedAnswer)) {
            return saveAnswer(question, submittedAnswer, 0,
                    "The answer appears unrelated to the question. Address the topic asked before submitting.");
        }

        String role = "the selected role";
        if (question.getInterview() != null && question.getInterview().getRole() != null && !question.getInterview().getRole().isBlank()) {
            role = question.getInterview().getRole().trim();
        }

        String prompt = """
                You are grading an interview candidate for this role: %s.

                Grade only the candidate answer. Do not answer the question yourself.
                Be fair and moderately lenient, like a supportive interviewer evaluating a learner.
                Give credit for correct meaning even if wording is not formal or textbook-perfect.
                Grade according to the role and question topic, not as a Java-only interview unless the role or question is about Java.
                Do not over-penalize missing minor details when the main idea is clear.
                Penalize vague answers, memorized buzzwords without explanation, major factual errors, and unrelated content.
                If the candidate answer is blank, says they do not know, repeats the question, or is unrelated, score must be 0.

                Use this rubric:
                0 = no answer, unrelated, repeated question, or "I don't know"
                1-3 = mostly incorrect, extremely vague, or only random keywords
                4-5 = some correct idea is present, but explanation is weak or incomplete
                6-7 = generally correct explanation with a few missing details
                8-9 = strong answer with clear reasoning and only minor gaps
                10 = excellent, complete, accurate, and easy to understand

                Important scoring guidance:
                If the answer explains the main concept correctly in plain language, score at least 6.
                If the answer is relevant, detailed, and mostly correct, score at least 7.
                If the answer includes a good example or practical reasoning, prefer 8 or higher unless there is a major error.

                Return only valid JSON in this exact shape:
                {"score":0,"feedback":"short, specific feedback"}

                Question: %s
                Candidate answer: %s
                """.formatted(role, question.getQuestionText(), submittedAnswer);

        String aiResponse = aiService.callAI(prompt);

        Evaluation evaluation = parseEvaluation(aiResponse);
        evaluation = applyLenientFloor(question.getQuestionText(), submittedAnswer, evaluation);

        return saveAnswer(question, submittedAnswer, evaluation.score(), evaluation.feedback());
    }

    private Answer saveAnswer(Question question, String submittedAnswer, int score, String feedback) {
        Answer answer = answerRepository.findByQuestionId(question.getId())
                .orElseGet(Answer::new);
        answer.setQuestion(question);
        answer.setAnswerText(submittedAnswer);
        answer.setScore(Math.max(0, Math.min(10, score)));
        answer.setFeedback(feedback == null || feedback.isBlank()
                ? "Unable to evaluate the answer. Please try again with a clearer response."
                : feedback.trim());

        return answerRepository.save(answer);
    }

    private boolean isNonAnswer(String answerText) {
        if (answerText.isBlank()) {
            return true;
        }

        String normalized = answerText.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        int wordCount = normalized.split("\\s+").length;
        if (wordCount < 3) {
            return true;
        }

        return wordCount <= 8 && UNCERTAIN_ANSWER_PATTERN.matcher(normalized).find();
    }

    private boolean isQuestionRepeated(String questionText, String answerText) {
        String question = normalize(questionText);
        String answer = normalize(answerText);
        return !question.isBlank() && question.equals(answer);
    }

    private boolean isLikelyUnrelated(String questionText, String answerText) {
        Set<String> questionTerms = meaningfulTerms(questionText);
        Set<String> answerTerms = meaningfulTerms(answerText);

        if (questionTerms.isEmpty() || answerTerms.size() < 2 || answerTerms.size() > 12) {
            return false;
        }

        long sharedTerms = answerTerms.stream().filter(questionTerms::contains).count();
        return sharedTerms == 0 && answerTerms.size() <= 8;
    }

    private Evaluation applyLenientFloor(String questionText, String answerText, Evaluation evaluation) {
        int wordCount = normalize(answerText).isBlank() ? 0 : normalize(answerText).split("\\s+").length;
        long sharedTerms = meaningfulTerms(answerText).stream()
                .filter(meaningfulTerms(questionText)::contains)
                .count();

        int adjustedScore = evaluation.score();
        if (wordCount >= 35 && sharedTerms >= 1 && adjustedScore < 6) {
            adjustedScore = 6;
        }

        if (wordCount >= 55 && sharedTerms >= 2 && adjustedScore < 7) {
            adjustedScore = 7;
        }

        return new Evaluation(adjustedScore, evaluation.feedback());
    }

    private Set<String> meaningfulTerms(String text) {
        return Pattern.compile("[a-z0-9]+")
                .matcher(normalize(text))
                .results()
                .map(MatchResult::group)
                .filter(term -> term.length() > 2)
                .filter(term -> !STOP_WORDS.contains(term))
                .collect(Collectors.toSet());
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

    private Evaluation parseEvaluation(String text) {
        Evaluation jsonEvaluation = parseJsonEvaluation(text);
        if (jsonEvaluation != null) {
            return jsonEvaluation;
        }

        return new Evaluation(extractScore(text), extractFeedback(text));
    }

    private Evaluation parseJsonEvaluation(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String json = extractJsonObject(text);
        Matcher scoreMatcher = Pattern.compile("\"score\"\\s*:\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(json);
        if (!scoreMatcher.find()) {
            return null;
        }

        int score = Integer.parseInt(scoreMatcher.group(1));
        String feedback = "";
        Matcher feedbackMatcher = Pattern.compile("\"feedback\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"", Pattern.CASE_INSENSITIVE).matcher(json);
        if (feedbackMatcher.find()) {
            feedback = unescapeJsonString(feedbackMatcher.group(1));
        }

        return new Evaluation(Math.max(0, Math.min(10, score)), feedback);
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .trim();
    }

    private int extractScore(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher matcher = Pattern.compile("(?i)score\\s*[:=]?\\s*(\\d{1,2})(?:\\s*/\\s*10)?").matcher(text);
        if (!matcher.find()) {
            return 0;
        }

        int score = Integer.parseInt(matcher.group(1));
        return Math.max(0, Math.min(10, score));
    }

    private String extractFeedback(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Matcher matcher = Pattern.compile("(?is)feedback\\s*:\\s*(.*)").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return text.trim();
    }

    private record Evaluation(int score, String feedback) {
    }
}
