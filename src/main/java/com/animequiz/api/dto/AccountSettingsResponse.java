package com.animequiz.api.dto;
public record AccountSettingsResponse(Long userId, String displayName, String theme, String accent,
    String locale, boolean hasAvatar, long avatarVersion, boolean googleLinked) {}

