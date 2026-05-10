package de.yyuh.iridium.boot.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * A glob-style path pattern supporting {@code *} and {@code **} wildcards.
 *
 * <p>Used by the middleware system to select or exclude request paths.
 * Matching is segment-by-segment (split by {@code /}):
 * <ul>
 *   <li>{@code *} — matches exactly one path segment</li>
 *   <li>{@code **} — matches zero or more path segments</li>
 *   <li>literal — matches exactly that segment</li>
 * </ul>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code /api/**} — matches {@code /api}, {@code /api/users}, {@code /api/users/42}</li>
 *   <li>{@code /api/*} — matches {@code /api/users} but not {@code /api/users/42}</li>
 *   <li>{@code /**} — matches everything</li>
 * </ul>
 */
@NullMarked
public record GlobPattern(String pattern, List<String> segments) {

  /**
   * Parses a glob pattern string.
   *
   * @param raw the raw pattern, e.g. {@code /api/**}
   * @return the parsed pattern
   */
  public static GlobPattern parse(final String raw) {
    final var parts = new ArrayList<String>();

    for (final var part : raw.split("/")) {
      if (!part.isEmpty()) {
        parts.add(part);
      }
    }

    return new GlobPattern(raw, Collections.unmodifiableList(parts));
  }

  /**
   * Parses multiple patterns.
   *
   * @param raw an array of raw patterns
   * @return a list of parsed patterns
   */
  public static List<GlobPattern> parseAll(final String[] raw) {
    final var patterns = new ArrayList<GlobPattern>(raw.length);

    for (final var p : raw) {
      patterns.add(parse(p));
    }

    return Collections.unmodifiableList(patterns);
  }

  /**
   * Tests whether the given path matches this pattern.
   *
   * @param path the request path, e.g. {@code /api/users/42}
   * @return {@code true} if the path matches
   */
  public boolean matches(final String path) {
    final var pathSegments = split(path);
    return matches(0, pathSegments, 0);
  }

  /**
   * Tests whether any of the given patterns matches the path.
   *
   * @param patterns a list of patterns
   * @param path     the request path
   * @return {@code true} if at least one pattern matches
   */
  public static boolean anyMatches(final List<GlobPattern> patterns, final String path) {
    for (final var pattern : patterns) {
      if (pattern.matches(path)) {
        return true;
      }
    }

    return false;
  }

  private boolean matches(final int patternIdx, final List<String> pathSegments, final int pathIdx) {
    if (patternIdx == segments.size()) {
      return pathIdx == pathSegments.size();
    }

    final var segment = segments.get(patternIdx);

    if ("**".equals(segment)) {
      return matchesDoubleStar(patternIdx, pathSegments, pathIdx);
    }

    if (pathIdx >= pathSegments.size()) {
      return false;
    }

    final var pathSegment = pathSegments.get(pathIdx);

    if ("*".equals(segment) || segment.equals(pathSegment)) {
      return matches(patternIdx + 1, pathSegments, pathIdx + 1);
    }

    return false;
  }

  private boolean matchesDoubleStar(final int patternIdx, final List<String> pathSegments, final int pathIdx) {
    for (int consumed = 0; consumed <= pathSegments.size() - pathIdx; consumed++) {
      if (matches(patternIdx + 1, pathSegments, pathIdx + consumed)) {
        return true;
      }
    }

    return false;
  }

  private static List<String> split(final String path) {
    final var parts = new ArrayList<String>();

    for (final var part : path.split("/")) {
      if (!part.isEmpty()) {
        parts.add(part);
      }
    }

    return parts;
  }
}
