package de.yyuh.iridium.boot.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import com.fasterxml.jackson.core.type.TypeReference;

import de.yyuh.iridium.boot.json.JsonMapper;
import de.yyuh.libs.core.result.Result;

/**
 * Wraps an incoming HTTP request and provides helpers for reading
 * the request and writing the response.
 *
 * <p>Gives access to method, URI, path, query parameters, headers,
 * body, and path variables extracted by the router. Response sending
 * is done through the same context.</p>
 */
@NullMarked
public final class RequestContext {

  private final HttpExchange exchange;
  private Map<String, String> pathVariables = Map.of();

  /**
   * Constructs a context around the given exchange.
   *
   * @param exchange the underlying HTTP exchange
   */
  public RequestContext(final HttpExchange exchange) {
    this.exchange = exchange;
  }

  /**
   * Stores path variables extracted by the router during matching.
   *
   * @param pathVariables a map of variable names to values
   */
  public void setPathVariables(final Map<String, String> pathVariables) {
    this.pathVariables = Map.copyOf(pathVariables);
  }

  /**
   * Returns the HTTP method (e.g. {@code GET}, {@code POST}).
   *
   * @return the method string
   */
  public String method() {
    return exchange.getRequestMethod();
  }

  /**
   * Returns the full request URI.
   *
   * @return the URI
   */
  public URI uri() {
    return exchange.getRequestURI();
  }

  /**
   * Returns the path component of the request URI.
   *
   * @return the path, e.g. {@code /api/users/42}
   */
  public String path() {
    return exchange.getRequestURI().getPath();
  }

  /**
   * Returns all path variables extracted by the router.
   *
   * @return an unmodifiable map of variable names to values
   */
  public Map<String, String> pathVariables() {
    return pathVariables;
  }

  /**
   * Returns a single path variable by name.
   *
   * @param name the variable name, e.g. {@code id}
   * @return the value if present, otherwise empty
   */
  public Optional<String> pathVariable(final String name) {
    return Optional.ofNullable(pathVariables.get(name));
  }

  /**
   * Parses and returns all query parameters.
   *
   * @return an unmodifiable map of parameter names → list of values
   */
  public Map<String, List<String>> queryParams() {
    final var query = exchange.getRequestURI().getRawQuery();

    if (query == null || query.isEmpty()) {
      return Collections.emptyMap();
    }

    final var map = new LinkedHashMap<String, List<String>>();

    for (final var pair : query.split("&")) {
      final var idx = pair.indexOf('=');

      final var key = idx >= 0
          ? decode(pair.substring(0, idx))
          : decode(pair);

      final var value = idx >= 0
          ? decode(pair.substring(idx + 1))
          : "";

      map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    return Collections.unmodifiableMap(map);
  }

  /**
   * Returns the first value for a query parameter.
   *
   * @param name the parameter name
   * @return the first value if present, otherwise empty
   */
  public Optional<String> queryParam(final String name) {
    final var values = queryParams().get(name);

    return values == null || values.isEmpty()
        ? Optional.empty()
        : Optional.of(values.getFirst());
  }

  /**
   * Returns all request headers.
   *
   * @return the headers map
   */
  public Headers requestHeaders() {
    return exchange.getRequestHeaders();
  }

  /**
   * Returns the first value for a request header.
   *
   * @param name the header name (case-insensitive)
   * @return the first value if present, otherwise empty
   */
  public Optional<String> requestHeader(final String name) {
    final var values = exchange.getRequestHeaders().get(name);

    return values == null || values.isEmpty()
        ? Optional.empty()
        : Optional.of(values.getFirst());
  }

  /**
   * Reads the request body as a UTF-8 string.
   *
   * @return a {@link Result} containing the body or an {@link IOException}
   */
  public Result<String, IOException> body() {
    return Result.of(() -> new String(readBody(), StandardCharsets.UTF_8));
  }

  /**
   * Reads the request body as raw bytes.
   *
   * @return a {@link Result} containing the bytes or an {@link IOException}
   */
  public Result<byte[], IOException> bodyBytes() {
    return Result.of(this::readBody);
  }

  /**
   * Reads the request body and deserializes it from JSON.
   *
   * @param <T>  the target type
   * @param type the target class
   * @return a {@link Result} containing the parsed object or an exception
   */
  public <T> Result<T, Exception> body(final Class<T> type) {
    return bodyBytes().<Exception>mapErr(e -> e).flatMap(bytes -> JsonMapper.read(bytes, type));
  }

  /**
   * Reads the request body and deserializes it from JSON for generic types.
   *
   * @param <T>     the target type
   * @param typeRef the type reference capturing generic information
   * @return a {@link Result} containing the parsed object or an exception
   */
  public <T> Result<T, Exception> body(final TypeReference<T> typeRef) {
    return bodyBytes().<Exception>mapErr(e -> e).flatMap(bytes -> JsonMapper.read(bytes, typeRef));
  }

  /**
   * Returns a mutable view of the response headers.
   *
   * @return the response headers map
   */
  public Headers responseHeaders() {
    return exchange.getResponseHeaders();
  }

  /**
   * Adds a response header, returning this context for chaining.
   *
   * @param name  the header name
   * @param value the header value
   * @return this context
   */
  public RequestContext responseHeader(final String name, final String value) {
    exchange.getResponseHeaders().add(name, value);
    return this;
  }

  /**
   * Sends a response with the given status and raw byte body.
   *
   * @param status the HTTP status code
   * @param body   the response body bytes
   * @return a {@link Result} indicating success or an {@link IOException}
   */
  public Result<Void, IOException> send(final int status, final byte[] body) {
    return Result.of(() -> {
      exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);

      if (body.length > 0) {
        try (final var out = exchange.getResponseBody()) {
          out.write(body);
        }
      }

      return null;
    });
  }

  /**
   * Sends a response with the given status and UTF-8 string body.
   *
   * @param status the HTTP status code
   * @param body   the response body text
   * @return a {@link Result} indicating success or an {@link IOException}
   */
  public Result<Void, IOException> send(final int status, final String body) {
    return send(status, body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends a response with the given status and an empty body.
   *
   * @param status the HTTP status code
   * @return a {@link Result} indicating success or an {@link IOException}
   */
  public Result<Void, IOException> send(final int status) {
    return send(status, "");
  }

  /**
   * Sends a {@code 200 OK} response with a UTF-8 string body.
   *
   * @param body the response body text
   * @return a {@link Result} indicating success or an {@link IOException}
   */
  public Result<Void, IOException> send(final String body) {
    return send(200, body);
  }

  /**
   * Sends a {@code 200 OK} response with raw bytes.
   *
   * @param body the response body bytes
   * @return a {@link Result} indicating success or an {@link IOException}
   */
  public Result<Void, IOException> send(final byte[] body) {
    return send(200, body);
  }

  private byte[] readBody() throws IOException {
    final var in = exchange.getRequestBody();
    final var out = new ByteArrayOutputStream();

    in.transferTo(out);
    return out.toByteArray();
  }

  private static String decode(final String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
