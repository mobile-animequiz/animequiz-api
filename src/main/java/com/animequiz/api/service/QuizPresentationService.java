package com.animequiz.api.service;

import com.animequiz.api.dto.*;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.repository.QuizRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** V2 adapter: bulk metadata lookup, shared catalogue/count/business services. */
@Service
@Transactional(readOnly = true)
public class QuizPresentationService {
    private final QuizRepository quizzes;
    public QuizPresentationService(QuizRepository quizzes) { this.quizzes = quizzes; }

    public Page<QuizV2Response> page(Page<QuizResponse> page, String lang) {
        ContentLanguage.validate(lang);
        Map<Long, Quiz> metadata = quizzes.findAllById(page.map(QuizResponse::id).getContent()).stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));
        return page.map(base -> view(base, metadata.get(base.id()), lang));
    }
    public QuizV2Response one(QuizResponse base, String lang) {
        ContentLanguage.validate(lang);
        return view(base, quizzes.findById(base.id()).orElseThrow(), lang);
    }
    private QuizV2Response view(QuizResponse base, Quiz stored, String lang) {
        boolean english = "en".equals(lang) && stored.getEnglishTitle() != null && !stored.getEnglishTitle().isBlank()
                && stored.getEnglishDescription() != null && !stored.getEnglishDescription().isBlank();
        return new QuizV2Response(base.id(), english ? stored.getEnglishTitle() : base.title(),
                english ? stored.getEnglishDescription() : base.description(), base.difficulty(), base.category(),
                base.questionCount(), stored.getImageUrl() == null ? "/media/quizzes/default.png" : stored.getImageUrl(),
                english ? stored.getEnglishTitle() : stored.getImageAltText() == null ? base.title() : stored.getImageAltText(),
                QuizRules.seconds(base.difficulty()), english ? "en" : "pt-BR");
    }
}
