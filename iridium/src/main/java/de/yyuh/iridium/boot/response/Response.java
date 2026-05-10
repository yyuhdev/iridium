package de.yyuh.iridium.boot.response;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.json.JsonMapper;

/**
 * An immutable HTTP response with status code, headers, and body.
 *
 * <p>Use the static factory methods for common status codes, or
 * {@link #status(int)} / {@link #status(int, String)} for custom codes.
 * Headers are added fluently via {@link #header(String, String)} and
 * convenience methods like {@link #json(String)}.</p>
 *
 * @param status  the HTTP status code
 * @param headers the response headers (name → list of values)
 * @param body    the response body bytes
 */
@NullMarked
public record Response(int status, Map<String, List<String>> headers, byte[] body) {

  /**
   * Creates a {@code 200 OK} response with no body.
   *
   * @return the response
   */
  public static Response ok() {
    return new Response(200, Map.of(), new byte[0]);
  }

  /**
   * Creates a {@code 200 OK} response with a UTF-8 encoded string body.
   *
   * @param body the response body text
   * @return the response
   */
  public static Response ok(final String body) {
    return new Response(200, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a {@code 200 OK} response with a raw byte body.
   *
   * @param body the response body bytes
   * @return the response
   */
  public static Response ok(final byte[] body) {
    return new Response(200, Map.of(), body);
  }

  /**
   * Creates a {@code 200 OK} response with a JSON-serialized object body.
   *
   * @param value the object to serialize
   * @return the response
   */
  public static Response json(final Object value) {
    return json(200, value);
  }

  /**
   * Creates a response with the given status and a JSON-serialized
   * object body.
   *
   * @param status the HTTP status code
   * @param value  the object to serialize
   * @return the response
   */
  public static Response json(final int status, final Object value) {
    final var result = JsonMapper.write(value);
    return result.isOk()
        ? new Response(status, Map.of("Content-Type", List.of("application/json")), result.unwrap())
        : status(500, "{\"error\":\"serialization failed\"}");
  }

  /**
   * Creates a {@code 201 Created} response with a UTF-8 body.
   *
   * @param body the response body text
   * @return the response
   */
  public static Response created(final String body) {
    return new Response(201, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a {@code 201 Created} response with raw bytes.
   *
   * @param body the response body bytes
   * @return the response
   */
  public static Response created(final byte[] body) {
    return new Response(201, Map.of(), body);
  }

  /**
   * Creates a {@code 204 No Content} response with no body.
   *
   * @return the response
   */
  public static Response noContent() {
    return new Response(204, Map.of(), new byte[0]);
  }

  /**
   * Creates a {@code 400 Bad Request} response.
   *
   * @param body the error message
   * @return the response
   */
  public static Response badRequest(final String body) {
    return new Response(400, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a {@code 404 Not Found} response.
   *
   * @param body the error message
   * @return the response
   */
  public static Response notFound(final String body) {
    return new Response(404, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a response with the given status code and no body.
   *
   * @param status the HTTP status code
   * @return the response
   */
  public static Response status(final int status) {
    return new Response(status, Map.of(), new byte[0]);
  }

  /**
   * Creates a response with the given status code and UTF-8 string body.
   *
   * @param status the HTTP status code
   * @param body   the response body text
   * @return the response
   */
  public static Response status(final int status, final String body) {
    return new Response(status, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a response with the given status code and raw byte body.
   *
   * @param status the HTTP status code
   * @param body   the response body bytes
   * @return the response
   */
  public static Response status(final int status, final byte[] body) {
    return new Response(status, Map.of(), body);
  }

  /**
   * Returns a copy of this response with an additional header.
   *
   * @param name  the header name
   * @param value the header value
   * @return a new {@code Response} with the header appended
   */
  public Response header(final String name, final String value) {
    final var copy = new LinkedHashMap<>(headers);
    copy.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    return new Response(status, Collections.unmodifiableMap(copy), body);
  }

  /**
   * Returns a response with {@code Content-Type: application/json}.
   *
   * @param body the JSON body text
   * @return a new {@code Response}
   */
  public Response json(final String body) {
    return contentType("application/json").status(status, body);
  }

  /**
   * Returns a response with {@code Content-Type: text/html}.
   *
   * @param body the HTML body text
   * @return a new {@code Response}
   */
  public Response html(final String body) {
    return contentType("text/html").status(status, body);
  }

  /**
   * Returns a response with {@code Content-Type: text/plain}.
   *
   * @param body the plain-text body
   * @return a new {@code Response}
   */
  public Response text(final String body) {
    return contentType("text/plain").status(status, body);
  }

  /**
   * Returns a response with the given {@code Content-Type} header.
   *
   * @param value the media-type value, e.g. {@code application/json}
   * @return a new {@code Response}
   */
  public Response contentType(final String value) {
    return header("Content-Type", value);
  }
}
