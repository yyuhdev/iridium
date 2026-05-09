package de.yyuh.iridium.boot.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/**
 * A path pattern that can contain variable segments like {@code {id}}.
 *
 * <p>Example patterns:
 * <ul>
 *   <li>{@code /test/{id}} — matches {@code /test/42}, extracting {@code id=42}</li>
 *   <li>{@code /auth/users/{id}/profiles/{profile}} — matches nested variables</li>
 *   <li>{@code /static} — matches exactly</li>
 * </ul>
 *
 * <p>Variables are named placeholders enclosed in curly braces.
 * Matching is done segment-by-segment (split by {@code /}).
 */
@NullMarked
public record PathPattern(String pattern, List<Segment> segments) {

  /**
   * A single segment of a path pattern — either a literal or a variable.
   */
  public sealed interface Segment permits LiteralSegment, VariableSegment {

    /** The raw text of this segment (e.g. "test" or "{id}"). */
    String text();
  }

  /** A fixed literal path segment. */
  public record LiteralSegment(String text) implements Segment {}

  /** A variable path segment like {@code {id}}. The stored text is the raw form including braces. */
  public record VariableSegment(String text, String name) implements Segment {}

  /**
   * Parses a path pattern string into a {@link PathPattern}.
   *
   * @param raw the raw pattern, e.g. {@code /test/{id}}
   * @return the parsed pattern
   */
  public static PathPattern parse(final String raw) {
    final var segments = new ArrayList<Segment>();
    final var parts = raw.split("/");

    for (final var part : parts) {
      if (part.isEmpty()) {
        continue;
      }

      if (part.startsWith("{") && part.endsWith("}") && part.length() > 2) {
        final var name = part.substring(1, part.length() - 1);
        segments.add(new VariableSegment(part, name));
      } else {
        segments.add(new LiteralSegment(part));
      }
    }

    return new PathPattern(raw, Collections.unmodifiableList(segments));
  }

  /**
   * Attempts to match an incoming request path against this pattern.
   *
   * @param path the actual request path, e.g. {@code /test/42}
   * @return a map of variable names to extracted values if matched, or empty if no match
   */
  public Optional<Map<String, String>> match(final String path) {
    final var pathParts = splitPath(path);

    if (pathParts.size() != segments.size()) {
      return Optional.empty();
    }

    final var variables = new LinkedHashMap<String, String>();

    for (int i = 0; i < segments.size(); i++) {
      final var segment = segments.get(i);
      final var value = pathParts.get(i);

      switch (segment) {
        case LiteralSegment lit -> {
          if (!lit.text().equals(value)) {
            return Optional.empty();
          }
        }
        case VariableSegment var -> variables.put(var.name(), value);
      }
    }

    return Optional.of(Collections.unmodifiableMap(variables));
  }

  private static List<String> splitPath(final String path) {
    final var parts = new ArrayList<String>();

    for (final var part : path.split("/")) {
      if (!part.isEmpty()) {
        parts.add(part);
      }
    }

    return parts;
  }
}
