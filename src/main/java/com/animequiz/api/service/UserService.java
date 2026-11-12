package com.animequiz.api.service;

import com.animequiz.api.dto.UserRequest;
import com.animequiz.api.dto.UserResponse;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ScoreRepository scoreRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, ScoreRepository scoreRepository,
                       UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.scoreRepository = scoreRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + request.email());
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Nome de usuário já em uso: " + request.username());
        }

        User user = new User(request.username(), request.email());
        User saved = userRepository.save(user);
        // Todo usuário nasce com um perfil (vazio), na mesma transação: o app consulta o perfil sempre.
        userProfileRepository.save(new UserProfile(saved, null, null));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = findUserOrThrow(id);

        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Já existe um usuário com o e-mail: " + request.email());
        }
        // Antes só o e-mail era checado: um username repetido estourava a unique do banco (HTTP 500).
        if (!user.getUsername().equalsIgnoreCase(request.username()) && userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Nome de usuário já em uso: " + request.username());
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
        }
        if (scoreRepository.existsByUserId(id)) {
            throw new ConflictException("Não é possível deletar usuário que possui pontuações (scores) registradas");
        }
        // Antes a FK do perfil estourava como HTTP 500; agora segue a mesma regra de dependentes (409).
        if (userProfileRepository.existsByUserId(id)) {
            throw new ConflictException("Não é possível deletar usuário que possui perfil; remova o perfil primeiro");
        }
        userRepository.deleteById(id);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchByUsername(String username, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(username, pageable).map(this::toResponse);
    }
}