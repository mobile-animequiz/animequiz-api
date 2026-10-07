package com.animequiz.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int points;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Score() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}