package com.animequiz.api.repository;
import com.animequiz.api.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    @Query("select t.userId from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> findOwnerByHash(@Param("hash") String hash);
}

