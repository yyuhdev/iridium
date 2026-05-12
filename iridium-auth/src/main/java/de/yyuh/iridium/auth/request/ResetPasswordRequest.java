package de.yyuh.iridium.auth.request;

public record ResetPasswordRequest(String token, String newPassword) {
}
