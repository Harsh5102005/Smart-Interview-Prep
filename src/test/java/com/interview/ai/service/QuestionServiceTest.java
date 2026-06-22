package com.interview.ai.service;

import com.interview.ai.entities.Interview;
import com.interview.ai.entities.Question;
import com.interview.ai.repository.InterviewRepository;
import com.interview.ai.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void retriesWhenAiReturnsPreviousQuestion() {
        Question previous = question("What is polymorphism in Java?");
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview("medium")));
        when(questionRepository.findByInterviewIdOrderByIdAsc(1L)).thenReturn(List.of(previous));
        when(aiService.callAI(any()))
                .thenReturn("What is polymorphism in Java?")
                .thenReturn("Explain the difference between abstraction and encapsulation in Java.");
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Question generated = questionService.generateQuestion(1L);

        assertEquals("Explain the difference between abstraction and encapsulation in Java.", generated.getQuestionText());
    }

    @Test
    void usesFallbackWhenAiKeepsReturningDuplicate() {
        Question previous = question("What is polymorphism in Java?");
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview("medium")));
        when(questionRepository.findByInterviewIdOrderByIdAsc(1L)).thenReturn(List.of(previous));
        when(aiService.callAI(any())).thenReturn("What is polymorphism in Java?");
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Question generated = questionService.generateQuestion(1L);

        assertFalse(generated.getQuestionText().equalsIgnoreCase(previous.getQuestionText()));
    }

    @Test
    void easyDifficultyPromptForcesBeginnerTheoryQuestion() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview("Java developer", "easy")));
        when(questionRepository.findByInterviewIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(aiService.callAI(any())).thenReturn("What is a class in Java?");
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        questionService.generateQuestion(1L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).callAI(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("beginner-friendly"));
        assertTrue(prompt.contains("Avoid algorithms"));
        assertTrue(prompt.contains("Question type for this request: theory/concept"));
        assertTrue(prompt.contains("Do not ask the candidate to write code"));
    }

    @Test
    void promptUsesSelectedRoleInsteadOfForcingJava() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview("C++ developer", "easy")));
        when(questionRepository.findByInterviewIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(aiService.callAI(any())).thenReturn("What is a pointer in C++?");
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        questionService.generateQuestion(1L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).callAI(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("exact role: C++ developer"));
        assertTrue(prompt.contains("Do not switch to Java unless the role itself asks for Java"));
        assertFalse(prompt.contains("level Java interview question"));
    }

    @Test
    void easyFallbackUsesBasicTheoryQuestion() {
        Question previous = question("For a C++ developer, what is one basic concept every beginner in this role should understand?");
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview("C++ developer", "easy")));
        when(questionRepository.findByInterviewIdOrderByIdAsc(1L)).thenReturn(List.of(previous));
        when(aiService.callAI(any())).thenReturn(previous.getQuestionText());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Question generated = questionService.generateQuestion(1L);

        assertEquals("For a C++ developer, what is the purpose of a common tool or language feature used in this role?", generated.getQuestionText());
    }

    private Interview interview(String difficulty) {
        return interview("Java developer", difficulty);
    }

    private Interview interview(String role, String difficulty) {
        Interview interview = new Interview();
        interview.setId(1L);
        interview.setRole(role);
        interview.setDifficulty(difficulty);
        return interview;
    }

    private Question question(String text) {
        Question question = new Question();
        question.setQuestionText(text);
        return question;
    }
}
