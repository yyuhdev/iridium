package de.yyuh.iridium.boot.response;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record Response(int status, Map<String, List<String>> headers, byte[] body) {

  public static Response ok() {
    return new Response(200, Map.of(), new byte[0]);
  }

  public static Response ok(final String body) {
    return new Response(200, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  public static Response ok(final byte[] body) {
    return new Response(200, Map.of(), body);
  }

  public static Response created(final String body) {
    return new Response(201, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  public static Response created(final byte[] body) {
    return new Response(201, Map.of(), body);
  }

  public static Response noContent() {
    return new Response(204, Map.of(), new byte[0]);
  }

  public static Response badRequest(final String body) {
    return new Response(400, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  public static Response notFound(final String body) {
    return new Response(404, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  public static Response status(final int status) {
    return new Response(status, Map.of(), new byte[0]);
  }

  public static Response status(final int status, final String body) {
    return new Response(status, Map.of(), body.getBytes(StandardCharsets.UTF_8));
  }

  public static Response status(final int status, final byte[] body) {
    return new Response(status, Map.of(), body);
  }

  public Response header(final String name, final String value) {
    final var copy = new LinkedHashMap<>(headers);
    copy.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    return new Response(status, Collections.unmodifiableMap(copy), body);
  }

  public Response json(final String body) {
    return contentType("application/json").status(status, body);
  }

  public Response html(final String body) {
    return contentType("text/html").status(status, body);
  }

  public Response text(final String body) {
    return contentType("text/plain").status(status, body);
  }

  public Response contentType(final String value) {
    return header("Content-Type", value);
  }
}
