package com.interview.ai.entities;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Answer {
    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private int score;

    @OneToOne
    private Question question;
}
