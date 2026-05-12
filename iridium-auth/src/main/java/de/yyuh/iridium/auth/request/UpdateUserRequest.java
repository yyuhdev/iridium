package de.yyuh.iridium.auth.request;

import java.util.Optional;

public record UpdateUserRequest(
    Optional<String> username,
    Optional<String> email,
    Optional<String> role) {
}
