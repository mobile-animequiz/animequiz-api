package com.animequiz.api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account_settings")
public class AccountSettings {
    @Id private Long userId;
    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId",insertable=false,updatable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private User owner;
    @Column(length = 50) private String displayName;
    @Column(nullable = false, length = 20) private String theme = "midnight";
    @Column(nullable = false, length = 20) private String accent = "violet";
    @Column(nullable = false, length = 10) private String locale = "pt-BR";
    @JdbcTypeCode(SqlTypes.VARBINARY) @Column(length = 2097152) private byte[] avatar;
    @Column(length = 32) private String avatarType;
    private long avatarVersion;
    public AccountSettings() {}
    public AccountSettings(Long userId) { this.userId = userId; }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getTheme() { return theme; }
    public void setTheme(String value) { theme = value; }
    public String getAccent() { return accent; }
    public void setAccent(String value) { accent = value; }
    public String getLocale() { return locale; }
    public void setLocale(String value) { locale = value; }
    public byte[] getAvatar() { return avatar; }
    public String getAvatarType() { return avatarType; }
    public long getAvatarVersion() { return avatarVersion; }
    public void setAvatar(byte[] bytes, String type) {
        avatar = bytes; avatarType = type; avatarVersion++;
    }
}

