package com.animequiz.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Resultado de UMA tentativa de quiz.
 *
 * <p>Além dos pontos, guarda os números exatos da tentativa para que as estatísticas do perfil sejam
 * históricas e não mudem se o quiz for editado depois:
 * <ul>
 *   <li>{@code totalQuestions}: quantas questões o quiz tinha no momento da tentativa;</li>
 *   <li>{@code answeredQuestions}: quantas respostas foram efetivamente enviadas (a tentativa pode ser parcial);</li>
 *   <li>{@code correctAnswers}: quantas dessas respostas estavam corretas.</li>
 * </ul>
 * Invariante: {@code 0 <= correctAnswers <= answeredQuestions <= totalQuestions} e {@code totalQuestions >= 1}.
 */
@Entity
@Table(name = "tb_scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "A pontuação é obrigatória")
    @Min(value = 0, message = "A pontuação não pode ser negativa")
    @Column(nullable = false)
    private Integer points;

    @NotNull(message = "A quantidade de acertos é obrigatória")
    @Min(value = 0, message = "A quantidade de acertos não pode ser negativa")
    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @NotNull(message = "A quantidade de respostas enviadas é obrigatória")
    @Min(value = 0, message = "A quantidade de respostas enviadas não pode ser negativa")
    @Column(name = "answered_questions", nullable = false)
    private Integer answeredQuestions;

    @NotNull(message = "O total de questões do quiz é obrigatório")
    @Min(value = 1, message = "O quiz deve ter ao menos uma questão")
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @NotNull(message = "O utilizador associado é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "O quiz associado é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    public Score() {}

    public Score(User user, Quiz quiz, Integer points, int correctAnswers, int answeredQuestions, int totalQuestions) {
        requireConsistentAttempt(correctAnswers, answeredQuestions, totalQuestions);
        this.user = user;
        this.quiz = quiz;
        this.points = points;
        this.correctAnswers = correctAnswers;
        this.answeredQuestions = answeredQuestions;
        this.totalQuestions = totalQuestions;
    }

    private static void requireConsistentAttempt(int correctAnswers, int answeredQuestions, int totalQuestions) {
        if (totalQuestions < 1 || correctAnswers < 0 || answeredQuestions < correctAnswers
                || totalQuestions < answeredQuestions) {
            throw new IllegalArgumentException(
                    "Tentativa inconsistente: é preciso 0 <= acertos (" + correctAnswers + ") <= respondidas ("
                            + answeredQuestions + ") <= total de questões (" + totalQuestions + ") e total >= 1");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(Integer correctAnswers) { this.correctAnswers = correctAnswers; }

    public Integer getAnsweredQuestions() { return answeredQuestions; }
    public void setAnsweredQuestions(Integer answeredQuestions) { this.answeredQuestions = answeredQuestions; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
}
