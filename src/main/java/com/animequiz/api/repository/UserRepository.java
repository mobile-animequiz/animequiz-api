package com.animequiz.api.repository;

import com.animequiz.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    java.util.Optional<User> findForUpdate(@Param("id") Long id);

    java.util.Optional<User> findByGoogleSubject(String subject);

    @Query("select q from User u join u.favoriteQuizzes q where u.id = :userId")
    Page<com.animequiz.api.entity.Quiz> findFavorites(@Param("userId") Long userId, Pageable pageable);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    java.util.Optional<User> findByEmailIgnoreCase(String email);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /** Indica se algum usuário tem o quiz entre os favoritos (impede excluir o quiz). */
    @Query("select count(u) > 0 from User u join u.favoriteQuizzes f where f.id = :quizId")
    boolean isQuizFavorited(@Param("quizId") Long quizId);
}
