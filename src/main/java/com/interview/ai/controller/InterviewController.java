package com.interview.ai.controller;

import com.interview.ai.entities.Interview;
import com.interview.ai.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<?> startInterview(@RequestBody Map<String, String> req){
        String role = req.get("role");
        String difficulty = req.get("difficulty");

        if (role == null || role.isBlank()) {
            role = "Java developer";
        }

        if (difficulty == null || difficulty.isBlank()) {
            difficulty = "medium";
        }

        Interview interview = interviewService.startInterview(
                parseUserId(req.get("userId")),
                role,
                difficulty
        );
        return ResponseEntity.ok(interview);
    }

    private Long parseUserId(String userIdValue) {
        if (userIdValue == null || userIdValue.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(userIdValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
