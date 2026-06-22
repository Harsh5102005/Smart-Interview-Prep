package com.interview.ai.entities;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Result {
    @Id
    @GeneratedValue
    private Long id;

    private int totalScore;
    private String finalFeedback;

    @OneToOne
    private Interview interview;
}