package com.interview.ai.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Interview {
    @Id
    @GeneratedValue
    private Long id;

    private String role;
    private String difficulty;
    private LocalDateTime startTime;

    @ManyToOne
    private User user;

}
