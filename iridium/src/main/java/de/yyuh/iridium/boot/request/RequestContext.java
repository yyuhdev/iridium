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

import de.yyuh.iridium.shared.result.Result;

@NullMarked
public final class RequestContext {

  private final HttpExchange exchange;

  public RequestContext(final HttpExchange exchange) {
    this.exchange = exchange;
  }

  public String method() {
    return exchange.getRequestMethod();
  }

  public URI uri() {
    return exchange.getRequestURI();
  }

  public String path() {
    return exchange.getRequestURI().getPath();
  }

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

  public Optional<String> queryParam(final String name) {
    final var values = queryParams().get(name);

    return values == null || values.isEmpty()
        ? Optional.empty()
        : Optional.of(values.getFirst());
  }

  public Headers requestHeaders() {
    return exchange.getRequestHeaders();
  }

  public Optional<String> requestHeader(final String name) {
    final var values = exchange.getRequestHeaders().get(name);

    return values == null || values.isEmpty()
        ? Optional.empty()
        : Optional.of(values.getFirst());
  }

  public Result<String, IOException> body() {
    return Result.of(() -> new String(readBody(), StandardCharsets.UTF_8));
  }

  public Result<byte[], IOException> bodyBytes() {
    return Result.of(this::readBody);
  }

  public Headers responseHeaders() {
    return exchange.getResponseHeaders();
  }

  public RequestContext responseHeader(final String name, final String value) {
    exchange.getResponseHeaders().add(name, value);
    return this;
  }

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

  public Result<Void, IOException> send(final int status, final String body) {
    return send(status, body.getBytes(StandardCharsets.UTF_8));
  }

  public Result<Void, IOException> send(final int status) {
    return send(status, "");
  }

  public Result<Void, IOException> send(final String body) {
    return send(200, body);
  }

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
