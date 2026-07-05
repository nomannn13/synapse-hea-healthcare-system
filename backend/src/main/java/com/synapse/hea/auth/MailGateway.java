package com.synapse.hea.auth;

public interface MailGateway {
  void sendPasswordReset(String email, String displayName, String rawToken);
  void sendNotification(String email, String displayName, String title, String message);
}
