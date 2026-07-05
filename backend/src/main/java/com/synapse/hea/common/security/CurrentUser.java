package com.synapse.hea.common.security;

import com.synapse.hea.common.exception.ForbiddenOperationException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

  public UUID id(Authentication authentication) {
    if (
      authentication == null ||
      !(authentication.getPrincipal() instanceof Jwt jwt)
    ) {
      throw new ForbiddenOperationException("Authentication is required");
    }
    return UUID.fromString(jwt.getSubject());
  }
}
