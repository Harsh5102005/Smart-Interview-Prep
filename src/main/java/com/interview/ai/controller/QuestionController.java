package com.interview.ai.controller;

import com.interview.ai.entities.Question;
import com.interview.ai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody Map<String, String> req) {

        String interviewIdValue = req.get("interviewId");
        if (interviewIdValue == null || interviewIdValue.isBlank()) {
            return ResponseEntity.badRequest().body("interviewId is required");
        }

        Long interviewId;
        try {
            interviewId = Long.parseLong(interviewIdValue);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("interviewId must be a number");
        }

        Question question = questionService.generateQuestion(interviewId);

        return ResponseEntity.ok(question);
    }
}
