package com.interview.ai.service;

import com.interview.ai.entities.Interview;
import com.interview.ai.repository.InterviewRepository;
import com.interview.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public Interview startInterview(Long userId, String role, String difficulty) {
        Interview interview = new Interview();
        interview.setRole(role);
        interview.setDifficulty(difficulty);
        interview.setStartTime(LocalDateTime.now());
        if (userId != null) {
            userRepository.findById(userId).ifPresent(interview::setUser);
        }

        return interviewRepository.save(interview);
    }
}
