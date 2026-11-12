package com.animequiz.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: o padrão EAGER do @OneToOne gerava uma consulta extra por perfil nas listagens (N+1).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Size(max = 255, message = "A bio deve ter no máximo 255 caracteres")
    private String bio;

    @Size(max = 500, message = "O URL do avatar é demasiado longo")
    private String avatarUrl;

    public UserProfile() {}

    public UserProfile(User user, String bio, String avatarUrl) {
        this.user = user;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
