package com.interview.ai.service;

import com.interview.ai.entities.Answer;
import com.interview.ai.entities.Interview;
import com.interview.ai.entities.Question;
import com.interview.ai.repository.AnswerRepository;
import com.interview.ai.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private AnswerService answerService;

    @Test
    void blankAnswerGetsZeroWithoutCallingAi() {
        Question question = question("What is JVM?");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.empty());
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Answer answer = answerService.submitAnswer(1L, "   ");

        assertEquals(0, answer.getScore());
        assertTrue(answer.getFeedback().contains("does not provide enough information"));
        verify(aiService, never()).callAI(any());
    }

    @Test
    void longAnswerWithUncertaintyPhraseStillGetsEvaluatedByAi() {
        Question question = question("What is inheritance in Java?");
        String longAnswer = "I am not sure about every detail, but inheritance in Java means one class can reuse " +
                "fields and methods from another class. The child class extends a parent class and can add new " +
                "behavior or override existing behavior. It helps reduce duplicate code and supports object oriented design.";

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.empty());
        when(aiService.callAI(any())).thenReturn("{\"score\":7,\"feedback\":\"Good basic explanation.\"}");
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Answer answer = answerService.submitAnswer(1L, longAnswer);

        assertEquals(7, answer.getScore());
        assertEquals("Good basic explanation.", answer.getFeedback());
    }

    @Test
    void unrelatedShortAnswerGetsZeroWithoutCallingAi() {
        Question question = question("What is polymorphism in Java?");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.empty());
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Answer answer = answerService.submitAnswer(1L, "database table index");

        assertEquals(0, answer.getScore());
        assertTrue(answer.getFeedback().contains("unrelated"));
        verify(aiService, never()).callAI(any());
    }

    @Test
    void parsesStructuredAiEvaluationAndClampsScore() {
        Question question = question("What is encapsulation in Java?");
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.empty());
        when(aiService.callAI(any())).thenReturn("{\"score\":12,\"feedback\":\"Good but too broad.\"}");
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Answer answer = answerService.submitAnswer(1L, "Encapsulation keeps fields private and exposes behavior through methods.");

        assertEquals(10, answer.getScore());
        assertEquals("Good but too broad.", answer.getFeedback());
    }

    @Test
    void gradingPromptUsesQuestionInterviewRole() {
        Question question = question("What is RAII in C++?");
        Interview interview = new Interview();
        interview.setRole("C++ developer");
        question.setInterview(interview);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.empty());
        when(aiService.callAI(any())).thenReturn("{\"score\":8,\"feedback\":\"Relevant C++ answer.\"}");
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        answerService.submitAnswer(1L, "RAII ties resource lifetime to object lifetime so destructors release resources automatically.");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).callAI(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("role: C++ developer"));
        assertFalse(prompt.contains("Java interview candidate"));
        assertTrue(prompt.contains("not as a Java-only interview"));
    }

    @Test
    void resubmittingSameQuestionUpdatesExistingAnswer() {
        Question question = question("What is inheritance in Java?");
        Answer existing = new Answer();
        existing.setId(99L);
        existing.setQuestion(question);
        existing.setAnswerText("old answer");
        existing.setScore(2);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.of(existing));
        when(aiService.callAI(any())).thenReturn("{\"score\":8,\"feedback\":\"Much clearer.\"}");
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Answer updated = answerService.submitAnswer(1L, "Inheritance lets a class reuse behavior from a parent class.");

        assertEquals(99L, updated.getId());
        assertEquals(8, updated.getScore());
        assertEquals("Much clearer.", updated.getFeedback());
        assertEquals("Inheritance lets a class reuse behavior from a parent class.", updated.getAnswerText());
    }

    private Question question(String text) {
        Question question = new Question();
        question.setId(1L);
        question.setQuestionText(text);
        return question;
    }
}
