package com.animequiz.api.service;
public interface PasswordMailer {
    boolean isConfigured();
    void send(String recipient,String url) throws Exception;
}

