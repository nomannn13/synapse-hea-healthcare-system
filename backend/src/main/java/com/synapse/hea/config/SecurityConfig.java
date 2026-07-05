package com.synapse.hea.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.synapse.hea.user.User;
import com.synapse.hea.user.UserRepository;
import com.synapse.hea.user.UserStatus;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetails(UserRepository users) {
    return email -> {
      User u = users
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
      return org.springframework.security.core.userdetails.User.withUsername(
        u.getEmail()
      )
        .password(u.getPasswordHash())
        .authorities("ROLE_" + u.getRole().name())
        .disabled(u.getStatus() != UserStatus.ACTIVE)
        .build();
    };
  }

  @Bean
  AuthenticationManager authenticationManager(
    UserDetailsService uds,
    PasswordEncoder pe
  ) {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider(uds);
    p.setPasswordEncoder(pe);
    return new ProviderManager(p);
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${app.security.jwt-secret}") String value) {
    OctetSequenceKey jwk = new OctetSequenceKey.Builder(secret(value))
      .algorithm(JWSAlgorithm.HS256)
      .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${app.security.jwt-secret}") String value) {
    NimbusJwtDecoder d = NimbusJwtDecoder.withSecretKey(secret(value))
      .macAlgorithm(MacAlgorithm.HS256)
      .build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer("synapse-hea"));
    return d;
  }

  @Bean
  SecurityFilterChain security(
    HttpSecurity http,
    JwtAuthenticationConverter converter
  ) throws Exception {
    return http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password-reset/request",
            "/api/v1/auth/password-reset/confirm",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
          )
          .permitAll()
          .requestMatchers(
            HttpMethod.GET,
            "/api/v1/departments",
            "/api/v1/doctors/**"
          )
          .permitAll()
          .requestMatchers("/api/v1/admin/**")
          .hasRole("ADMIN")
          .requestMatchers("/api/v1/patients/**")
          .hasAnyRole("DOCTOR", "ADMIN")
          .requestMatchers("/api/v1/urgent-cases/**")
          .hasAnyRole("DOCTOR", "ADMIN")
          .anyRequest()
          .authenticated()
      )
      .oauth2ResourceServer(o ->
        o.jwt(j -> j.jwtAuthenticationConverter(converter))
      )
      .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter c = new JwtAuthenticationConverter();
    c.setJwtGrantedAuthoritiesConverter(jwt -> {
      List<String> roles = jwt.getClaimAsStringList("roles");
      if (roles == null) return List.of();
      return roles
        .stream()
        .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
        .toList();
    });
    return c;
  }

  @Bean
  CorsConfigurationSource cors(
    @Value("${app.security.allowed-origins}") String origins
  ) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(
      Arrays.stream(origins.split(",")).map(String::trim).toList()
    );
    c.setAllowedMethods(
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    );
    c.setAllowedHeaders(
      List.of("Authorization", "Content-Type", "X-Requested-With")
    );
    c.setExposedHeaders(List.of("Content-Disposition"));
    c.setAllowCredentials(true);
    c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }

  private SecretKey secret(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) throw new IllegalStateException(
      "JWT_SECRET must be at least 32 bytes"
    );
    return new SecretKeySpec(bytes, "HmacSHA256");
  }
}
