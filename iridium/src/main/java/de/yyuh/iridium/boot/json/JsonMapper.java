package de.yyuh.iridium.boot.json;

import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.yyuh.iridium.shared.result.Result;

/**
 * Shared {@link ObjectMapper} used for request deserialization and
 * response serialization.
 *
 * <p>Configured with {@code FAIL_ON_UNKNOWN_PROPERTIES = false}
 * and the {@code JavaTimeModule} registered by default. Use
 * {@link #configure(Consumer)} for further customization.</p>
 */
@NullMarked
public final class JsonMapper {

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private JsonMapper() {
  }

  /**
   * Allows customization of the shared {@link ObjectMapper}.
   *
   * @param customizer a consumer that mutates the mapper
   */
  public static void configure(final Consumer<ObjectMapper> customizer) {
    customizer.accept(mapper);
  }

  /**
   * Deserializes a JSON byte array into the given type.
   *
   * @param <T>  the target type
   * @param body the raw JSON bytes
   * @param type the target class
   * @return a {@link Result} containing the parsed object or an exception
   */
  public static <T> Result<T, Exception> read(final byte[] body, final Class<T> type) {
    return Result.of(() -> mapper.readValue(body, type));
  }

  /**
   * Deserializes a JSON byte array into the given generic type.
   *
   * @param <T>      the target type
   * @param body     the raw JSON bytes
   * @param typeRef  the type reference capturing generic information
   * @return a {@link Result} containing the parsed object or an exception
   */
  public static <T> Result<T, Exception> read(final byte[] body, final TypeReference<T> typeRef) {
    return Result.of(() -> mapper.readValue(body, typeRef));
  }

  /**
   * Serializes an object to a JSON byte array.
   *
   * @param value the object to serialize
   * @return a {@link Result} containing the JSON bytes or an exception
   */
  public static Result<byte[], Exception> write(final Object value) {
    return Result.of(() -> mapper.writeValueAsBytes(value));
  }
}
