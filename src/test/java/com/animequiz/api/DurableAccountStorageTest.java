package com.animequiz.api;

import com.animequiz.api.entity.*;
import com.animequiz.api.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DurableAccountStorageTest {
    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses=User.class)
    @EnableJpaRepositories(basePackageClasses=UserRepository.class)
    static class StorageConfig {}

    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(StorageConfig.class).web(WebApplicationType.NONE)
            .run("--spring.datasource.url="+url,"--spring.jpa.hibernate.ddl-auto=update",
                "--spring.sql.init.mode=never","--spring.h2.console.enabled=false",
                "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration");
    }

    @Test void nameAppearancePhotoAndFavoritesSurviveClosingAndReopeningTheDatabase() {
        String filename=Path.of("target","durability-"+UUID.randomUUID()).toAbsolutePath().toString().replace('\\','/');
        String url="jdbc:h2:file:"+filename+";DB_CLOSE_ON_EXIT=FALSE";
        Long[] ids=new Long[2];
        try(var context=start(url)) {
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                var users=context.getBean(UserRepository.class);
                User user=users.save(new User("persistent-player","persistent@example.test"));
                Quiz quiz=context.getBean(QuizRepository.class).save(new Quiz("Durable quiz","Teste de persistência",Difficulty.FACIL,Category.SHONEN));
                user.getFavoriteQuizzes().add(quiz);users.save(user);
                var settings=new AccountSettings(user.getId());
                settings.setDisplayName("Isaac");settings.setTheme("eclipse");settings.setAccent("cyan");settings.setLocale("en");
                settings.setAvatar(new byte[]{1,2,3},"image/png");
                context.getBean(AccountSettingsRepository.class).save(settings);
                ids[0]=user.getId();ids[1]=quiz.getId();
            });
        }
        try(var context=start(url)) {
            var saved=context.getBean(AccountSettingsRepository.class).findById(ids[0]).orElseThrow();
            assertEquals("Isaac",saved.getDisplayName());assertEquals("eclipse",saved.getTheme());
            assertEquals("cyan",saved.getAccent());assertEquals("en",saved.getLocale());
            assertArrayEquals(new byte[]{1,2,3},saved.getAvatar());
            var favorites=context.getBean(UserRepository.class).findFavorites(ids[0],PageRequest.of(0,20));
            assertEquals(1,favorites.getTotalElements());assertEquals(ids[1],favorites.getContent().get(0).getId());
        }
    }
}

