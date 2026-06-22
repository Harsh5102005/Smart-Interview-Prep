package com.interview.ai.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Question {
    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    @ManyToOne
    private Interview interview;
    public void setQuestionText(String questionText) {
        this.questionText = questionText == null ? "" : questionText.trim();
    }

}
