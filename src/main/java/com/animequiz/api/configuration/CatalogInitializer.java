package com.animequiz.api.configuration;
import com.animequiz.api.entity.*;
import com.animequiz.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
@ConditionalOnProperty(name="app.catalog.seed-enabled",havingValue="true")
public class CatalogInitializer implements ApplicationRunner {
    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final OptionRepository options;
    private final ObjectMapper json;
    public CatalogInitializer(QuizRepository quizzes,QuestionRepository questions,OptionRepository options,ObjectMapper json) {
        this.quizzes=quizzes; this.questions=questions; this.options=options; this.json=json;
    }
    public record CatalogQuiz(String title,String description,Difficulty difficulty,Category category,
        List<String> sources,List<List<String>> questions,String language,String reviewedAt) {}
    @Override @Transactional
    public void run(ApplicationArguments ignored) throws Exception {
        CatalogQuiz[] catalog;
        try(var input=new ClassPathResource("catalog/pt-BR.json").getInputStream()) {
            catalog=json.readValue(input,CatalogQuiz[].class);
        }
        for(CatalogQuiz entry:catalog) {
            Quiz quiz=quizzes.findByTitleIgnoreCase(entry.title()).orElseGet(() ->
                quizzes.save(new Quiz(entry.title(),entry.description(),entry.difficulty(),entry.category())));
            List<Question> existing=questions.findAllByQuizId(quiz.getId());
            Set<String> statements=new HashSet<>();
            for(Question question:existing) {
                statements.add(normalize(question.getStatement()));
                // Old demo hotlinks have no recorded license. A missing image uses the app's textual fallback.
                String url=question.getImageUrl();
                if(url!=null && (url.startsWith("https://www.posters.de/") || url.startsWith("https://98types.co.uk/"))) {
                    question.setImageUrl(null);question.setImageAltText(null);
                }
            }
            int size=existing.size();
            for(List<String> row:entry.questions()) {
                if(size>=15) break; // Preserve custom questions, identities and historical attempts.
                if(row.size()!=5 || new HashSet<>(row.subList(1,5)).size()!=4)
                    throw new IllegalStateException("Catálogo com alternativas inválidas");
                if(!statements.add(normalize(row.get(0)))) continue;
                Question question=questions.save(new Question(row.get(0),quiz));
                List<String> choices=new ArrayList<>(row.subList(1,5));
                Collections.rotate(choices,size%4);
                for(String choice:choices) options.save(new Option(question,choice,choice.equals(row.get(1))));
                size++;
            }
        }
    }
    private String normalize(String value) { return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+"," "); }
}

