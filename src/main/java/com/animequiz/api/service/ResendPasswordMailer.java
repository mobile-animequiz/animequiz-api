package com.animequiz.api.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
@Service
public class ResendPasswordMailer implements PasswordMailer {
    private final String key,from;
    private final ObjectMapper mapper;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public ResendPasswordMailer(@Value("${mail.api-key:}") String key,@Value("${mail.from:}") String from,ObjectMapper mapper) {
        this.key=key; this.from=from; this.mapper=mapper;
    }
    public boolean isConfigured() { return !key.isBlank() && !from.isBlank(); }
    public void send(String recipient,String url) throws Exception {
        String content="Você solicitou uma nova senha do AnimeQuiz. Este link vale por 15 minutos e só pode ser usado uma vez:\n"
            +url+"\n\nSe não foi você, ignore esta mensagem. Sua senha continua a mesma.";
        var request=HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
            .timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+key)
            .header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of(
                "from",from,"to",new String[]{recipient},"subject","AnimeQuiz — redefinir senha","text",content)))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.discarding());
        if(response.statusCode()<200 || response.statusCode()>299) throw new IllegalStateException("Serviço de e-mail recusou o envio");
    }
}

