package com.animequiz.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_options")
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O texto da opção é obrigatório")
    @Size(max = 150, message = "O texto deve ter no máximo 150 caracteres")
    @Column(nullable = false)
    private String text;

    @NotNull(message = "A indicação de corretude é obrigatória")
    @Column(nullable = false)
    private Boolean correct;

    @NotNull(message = "A questão associada é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    public Option() {}

    // Construtor exigido pelo OptionService (Question primeiro)
    public Option(Question question, String text, Boolean correct) {
        this.question = question;
        this.text = text;
        this.correct = correct;
    }

    // Construtor alternativo (caso necessário)
    public Option(String text, Boolean correct, Question question) {
        this.text = text;
        this.correct = correct;
        this.question = question;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    // Métodos de compatibilidade para o serviço
    public Boolean getIsCorrect() { return correct; }
    public void setIsCorrect(Boolean correct) { this.correct = correct; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}