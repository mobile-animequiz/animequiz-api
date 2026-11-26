package com.animequiz.api.security;

import java.util.Set;
import java.util.regex.Pattern;

/** The small, public V1 never grants access to account or administrative operations. */
public final class AcademicVersionPolicy {
    private AcademicVersionPolicy() { }

    public static final Set<String> V1_PATHS = Set.of(
        "/api/hello", "/api/versioned/status", "/quizzes", "/quizzes/{id}",
        "/quizzes/search", "/quizzes/difficulty/{difficulty}", "/quizzes/{quizId}/questions",
        "/questions", "/questions/{id}", "/questions/by-quiz/{quizId}",
        "/options", "/options/{id}", "/options/by-question/{questionId}");

    private static final Pattern CATALOG = Pattern.compile(
        "^/(?:quizzes(?:/[^/]+|/difficulty/[^/]+|/[^/]+/questions)?"
        + "|questions(?:/[^/]+|/by-quiz/[^/]+)?"
        + "|options(?:/[^/]+|/by-question/[^/]+)?)$");

    public static String path(String uri, String contextPath) {
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
            ? uri.substring(contextPath.length()) : uri;
    }

    public static boolean isV1Read(String method, String path, String version) {
        return "GET".equals(method) && (version == null || version.equals("1"))
            && (isCatalog(path) || path.equals("/api/hello") || path.equals("/api/versioned/status"));
    }

    public static boolean isV2Catalog(String method, String path, String version) {
        return "GET".equals(method) && "2".equals(version)
            && (isCatalog(path) || path.equals("/scores/ranking"));
    }

    private static boolean isCatalog(String path) { return CATALOG.matcher(path).matches(); }
}
