package de.yyuh.iridium.shared.timer;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;

/**
 * Measures elapsed time between start and end points.
 *
 * <p>Use for benchmarking operations — e.g. classpath scanning,
 * component registration, and startup duration.</p>
 */
@NullMarked
public final class Timer {

  private final long startNanos;

  private Timer() {
    this.startNanos = System.nanoTime();
  }

  /**
   * Starts a new timer at the current time.
   *
   * @return a new {@code Timer} instance
   */
  public static Timer start() {
    return new Timer();
  }

  /**
   * Returns the elapsed time in milliseconds.
   *
   * @return elapsed milliseconds
   */
  public long elapsedMillis() {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }

  /**
   * Returns the elapsed time in the given unit.
   *
   * @param unit the target time unit
   * @return elapsed time in that unit
   */
  public long elapsed(final TimeUnit unit) {
    return unit.convert(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
  }

  /**
   * Stops the timer and returns elapsed milliseconds.
   * Convenience alias for {@link #elapsedMillis()}.
   *
   * @return elapsed milliseconds
   */
  public long stop() {
    return elapsedMillis();
  }
}
