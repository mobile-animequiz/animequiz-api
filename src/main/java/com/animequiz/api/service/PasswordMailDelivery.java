package com.animequiz.api.service;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.*;
@Component
public class PasswordMailDelivery {
    private final PasswordMailer mailer;
    public PasswordMailDelivery(PasswordMailer mailer) { this.mailer=mailer; }
    @Async("passwordMailExecutor") @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void deliver(PasswordResetService.ResetMail event) {
        try { mailer.send(event.recipient(),event.url()); }
        catch(Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Não foi possível entregar e-mail de recuperação");
        }
    }
}

