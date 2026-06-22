package com.interview.ai.repository;

import com.interview.ai.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByInterviewIdOrderByIdAsc(Long interviewId);
}
