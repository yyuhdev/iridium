package de.yyuh.iridium.auth.request;

public record CreateUserRequest(
    String name,
    String email,
    String password) {
}
