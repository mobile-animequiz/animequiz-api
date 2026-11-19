package com.animequiz.api.service;

import com.animequiz.api.dto.*;
import com.animequiz.api.entity.*;
import com.animequiz.api.repository.*;
import com.animequiz.api.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.*;

@Service
public class AccountService {
    public static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;
    private final UserRepository users;
    private final AccountSettingsRepository settings;
    private final QuizRepository quizzes;
    private final QuizService quizService;
    public AccountService(UserRepository users, AccountSettingsRepository settings,
                          QuizRepository quizzes, QuizService quizService) {
        this.users=users; this.settings=settings; this.quizzes=quizzes; this.quizService=quizService;
    }
    private User user(Long id, boolean lock) {
        return (lock ? users.findForUpdate(id) : users.findById(id))
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
    @Transactional(readOnly=true)
    public AccountSettingsResponse read(Long id) {
        User owner=user(id, false);
        return response(owner, settings.findById(id).orElseGet(() -> new AccountSettings(id)));
    }
    @Transactional
    public AccountSettingsResponse update(Long id, AccountSettingsRequest request) {
        User owner=user(id, true);
        AccountSettings value=settings.findById(id).orElseGet(() -> new AccountSettings(id));
        if (request.displayName()!=null) {
            String name=request.displayName().strip();
            if(name.length()<2 || name.length()>50 || name.codePoints().anyMatch(Character::isISOControl))
                throw new BadRequestException("Nome deve ter entre 2 e 50 caracteres, sem controles");
            value.setDisplayName(name);
        }
        if(request.theme()!=null) {
            require(Set.of("midnight","eclipse"),request.theme()); value.setTheme(request.theme());
        }
        if(request.accent()!=null) {
            require(Set.of("violet","cyan","rose"),request.accent()); value.setAccent(request.accent());
        }
        if(request.locale()!=null) {
            require(Set.of("pt-BR","en","device"),request.locale()); value.setLocale(request.locale());
        }
        return response(owner, settings.save(value));
    }
    private void require(Set<String> allowed, String value) {
        if(!allowed.contains(value)) throw new BadRequestException("Preferência inválida");
    }
    @Transactional
    public AccountSettingsResponse upload(Long id, MultipartFile file) {
        if(file.isEmpty() || file.getSize()>MAX_AVATAR_BYTES) throw new BadRequestException("Foto deve ter até 2 MB");
        try {
            byte[] bytes=file.getBytes();
            if(bytes.length>MAX_AVATAR_BYTES) throw new BadRequestException("Foto deve ter até 2 MB");
            String type;
            try(ImageInputStream input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new BadRequestException("Selecione uma imagem JPEG ou PNG");
                ImageReader reader=readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!Set.of("jpeg","jpg","png").contains(format)) throw new BadRequestException("Selecione JPEG ou PNG");
                    int width=reader.getWidth(0), height=reader.getHeight(0);
                    if(width<1 || height<1 || width>4096 || height>4096 || (long)width*height>8000000)
                        throw new BadRequestException("Imagem muito grande; recorte a foto antes de enviar");
                    type=format.equals("png") ? "image/png" : "image/jpeg";
                    if(reader.read(0)==null) throw new BadRequestException("Imagem inválida");
                } finally { reader.dispose(); }
            }
            User owner=user(id, true);
            AccountSettings value=settings.findById(id).orElseGet(() -> new AccountSettings(id));
            value.setAvatar(bytes,type);
            return response(owner,settings.save(value));
        } catch(IOException e) { throw new BadRequestException("Não foi possível ler a foto"); }
    }
    @Transactional(readOnly=true)
    public Avatar avatar(Long id) {
        user(id, false);
        AccountSettings value=settings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Foto não encontrada"));
        if(value.getAvatar()==null) throw new ResourceNotFoundException("Foto não encontrada");
        return new Avatar(value.getAvatar().clone(), value.getAvatarType(),value.getAvatarVersion());
    }
    @Transactional
    public AccountSettingsResponse deleteAvatar(Long id) {
        User owner=user(id,true);
        AccountSettings value=settings.findById(id).orElseGet(() -> new AccountSettings(id));
        value.setAvatar(null,null); return response(owner,settings.save(value));
    }
    @Transactional(readOnly=true)
    public Page<QuizResponse> favorites(Long id, int page, int size) {
        user(id,false);
        return users.findFavorites(id,PageRequest.of(page,size,Sort.by("id")))
            .map(quiz -> quizService.getQuizById(quiz.getId()));
    }
    @Transactional
    public void favorite(Long id, Long quizId, boolean add) {
        User owner=user(id,true);
        Quiz quiz=quizzes.findById(quizId).orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado"));
        if(add) {
            if(owner.getFavoriteQuizzes().stream().noneMatch(item -> item.getId().equals(quizId)))
                owner.getFavoriteQuizzes().add(quiz);
        } else owner.getFavoriteQuizzes().removeIf(item -> item.getId().equals(quizId));
        users.save(owner);
    }
    private AccountSettingsResponse response(User owner, AccountSettings value) {
        return new AccountSettingsResponse(owner.getId(),
            value.getDisplayName()==null ? owner.getUsername() : value.getDisplayName(),
            value.getTheme(),value.getAccent(),value.getLocale(),value.getAvatar()!=null,
            value.getAvatarVersion(),owner.getGoogleSubject()!=null);
    }
    public record Avatar(byte[] bytes,String type,long version) {}
}

