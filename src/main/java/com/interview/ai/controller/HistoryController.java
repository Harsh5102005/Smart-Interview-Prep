package com.interview.ai.controller;

import com.interview.ai.entities.Answer;
import com.interview.ai.entities.Interview;
import com.interview.ai.entities.Question;
import com.interview.ai.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final AnswerRepository answerRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserHistory(@PathVariable Long userId) {
        List<Map<String, Object>> history = answerRepository
                .findByQuestionInterviewUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearUserHistory(@PathVariable Long userId) {
        List<Answer> answers = answerRepository.findByQuestionInterviewUserIdOrderByIdDesc(userId);
        answerRepository.deleteAll(answers);

        return ResponseEntity.ok(Map.of("deleted", answers.size()));
    }

    private Map<String, Object> toHistoryResponse(Answer answer) {
        Question question = answer.getQuestion();
        Interview interview = question == null ? null : question.getInterview();

        return Map.of(
                "id", answer.getId(),
                "question", question == null ? "" : question.getQuestionText(),
                "answer", answer.getAnswerText() == null ? "" : answer.getAnswerText(),
                "feedback", answer.getFeedback() == null ? "" : answer.getFeedback(),
                "score", answer.getScore(),
                "role", interview == null || interview.getRole() == null ? "" : interview.getRole(),
                "difficulty", interview == null || interview.getDifficulty() == null ? "" : interview.getDifficulty()
        );
    }
}
