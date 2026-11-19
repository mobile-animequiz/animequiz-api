package com.animequiz.api.configuration;
import java.time.Clock;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration @EnableAsync
public class MailDeliveryConfig {
    @Bean public Clock applicationClock() { return Clock.systemUTC(); }
    @Bean(name="passwordMailExecutor")
    public Executor passwordMailExecutor() {
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(2); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-mail-");
        executor.setRejectedExecutionHandler((job,pool) ->
            org.slf4j.LoggerFactory.getLogger(MailDeliveryConfig.class).warn("Fila de e-mail temporariamente indisponível"));
        executor.initialize(); return executor;
    }
}

