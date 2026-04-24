package com.analytics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clicks")
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String code;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @PrePersist
    protected void onCreate() {
        clickedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDateTime getClickedAt() { return clickedAt; }
}
