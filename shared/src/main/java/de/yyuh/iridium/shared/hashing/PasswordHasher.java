package de.yyuh.iridium.shared.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.jspecify.annotations.NullMarked;

import de.yyuh.libs.core.result.Result;

@NullMarked
public final class PasswordHasher {

  private static final int ITERATIONS = 65536;
  private static final int KEY_LENGTH = 256;

  public static Result<String, String> hashPassword(final String password) {
    return Result.of(() -> {
      final byte[] salt = new byte[16];
      final SecureRandom random = new SecureRandom();
      random.nextBytes(salt);

      final PBEKeySpec spec = new PBEKeySpec(
          password.toCharArray(),
          salt,
          ITERATIONS,
          KEY_LENGTH);

      final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

      final byte[] hash = factory.generateSecret(spec).getEncoded();

      return Base64.getEncoder().encodeToString(salt)
          + ":"
          + Base64.getEncoder().encodeToString(hash);
    }).mapErr(Exception::getMessage);
  }

  public static Result<Boolean, String> verifyPassword(
      final String password,
      final String stored) {
    return Result.of(() -> {

      final String[] parts = stored.split(":");

      final byte[] salt = Base64.getDecoder().decode(parts[0]);
      final byte[] originalHash = Base64.getDecoder().decode(parts[1]);

      final PBEKeySpec spec = new PBEKeySpec(
          password.toCharArray(),
          salt,
          ITERATIONS,
          KEY_LENGTH);

      final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

      final byte[] testHash = factory.generateSecret(spec).getEncoded();

      return MessageDigest.isEqual(originalHash, testHash);
    }).mapErr(Exception::getMessage);
  }
}
