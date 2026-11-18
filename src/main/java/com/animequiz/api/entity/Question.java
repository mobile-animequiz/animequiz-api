package com.animequiz.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O enunciado é obrigatório")
    @Size(min = 5, max = 255, message = "O enunciado deve ter entre 5 e 255 caracteres")
    @Column(nullable = false)
    private String statement;

    @Size(max = 1000)
    @Column(length = 1000)
    private String imageUrl;

    @Size(max = 255)
    private String imageAltText;

    @Column(nullable = false, columnDefinition = "integer default 30")
    private int timeLimitSeconds = 30;

    @NotNull(message = "O quiz associado é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    public Question() {}
    public Question(String statement, Quiz quiz) {
        this.statement = statement;
        this.quiz = quiz;
    }
    public Question(Quiz quiz, String statement) {
        this.quiz = quiz;
        this.statement = statement;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageAltText() { return imageAltText; }
    public void setImageAltText(String imageAltText) { this.imageAltText = imageAltText; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(int timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public String getText() {
        return this.statement;
    }
    public void setText(String text) {
        this.statement = text;
    }
}
