package com.interview.ai.controller;

import com.interview.ai.entities.Answer;
import com.interview.ai.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/answer")
@RequiredArgsConstructor
public class AnswerController {
    private final AnswerService answerService;

    @PostMapping
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> req) {

        Object questionIdValue = req.get("questionId");
        if (questionIdValue == null || questionIdValue.toString().isBlank()) {
            return ResponseEntity.badRequest().body("questionId is required");
        }

        Long questionId;
        try {
            questionId = Long.parseLong(questionIdValue.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("questionId must be a number");
        }

        Object answerTextValue = req.get("answerText");
        String answerText = answerTextValue == null ? "" : answerTextValue.toString();

        return ResponseEntity.ok(
                answerService.submitAnswer(questionId, answerText)
        );
    }

}
