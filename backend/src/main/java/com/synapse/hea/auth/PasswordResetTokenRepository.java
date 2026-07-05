package com.synapse.hea.auth;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository
  extends JpaRepository<PasswordResetToken, UUID>
{
  Optional<PasswordResetToken> findByTokenHash(String hash);
}
