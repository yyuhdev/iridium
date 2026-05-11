package de.yyuh.iridium.boot.health;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.NullMarked;

import de.yyuh.iridium.boot.controller.type.RestController;
import de.yyuh.iridium.boot.request.RequestContext;
import de.yyuh.iridium.boot.request.type.GET;
import de.yyuh.iridium.boot.response.Response;

/**
 * Built-in health endpoints providing liveness, readiness, and detailed
 * system health information.
 *
 * <h3>Endpoints</h3>
 * <ul>
 * <li>{@code GET /health/live} — liveness probe (is the process alive?)</li>
 * <li>{@code GET /health/ready} — readiness probe (is the process ready
 * to accept traffic?)</li>
 * <li>{@code GET /health} — full health overview with memory, uptime,
 * and JVM details</li>
 * </ul>
 */
@NullMarked
@RestController
public final class HealthController {

  public record LiveStatus(String status) {
  }

  public record ReadyStatus(String status, String check) {
  }

  public record HealthDetails(
      String status,
      String uptime,
      MemoryInfo memory,
      int availableProcessors,
      String javaVersion,
      String javaVendor,
      Instant timestamp) {
  }

  public record MemoryInfo(
      long heapUsed,
      long heapMax,
      long heapCommitted,
      long nonHeapUsed,
      long nonHeapCommitted) {
  }

  @GET("/health/live")
  public Response live(final RequestContext ctx) {
    return Response.json(new LiveStatus("UP"));
  }

  @GET("/health/ready")
  public Response ready(final RequestContext ctx) {
    final var memory = ManagementFactory.getMemoryMXBean();
    final var heap = memory.getHeapMemoryUsage();

    final var max = heap.getMax();

    if (max > 0) {
      final var usedRatio = (double) heap.getUsed() / (double) max;

      if (usedRatio > 0.95) {
        return Response.json(503, new ReadyStatus("DOWN",
            "heap usage %.1f%% exceeds 95%% threshold".formatted(usedRatio * 100)));
      }
    }

    return Response.json(new ReadyStatus("UP", "healthy"));
  }

  @GET("/health")
  public Response health(final RequestContext ctx) {
    final var runtime = ManagementFactory.getRuntimeMXBean();
    final var memory = ManagementFactory.getMemoryMXBean();

    final var details = new HealthDetails(
        "UP",
        formatUptime(runtime.getUptime()),
        memoryInfo(memory),
        Runtime.getRuntime().availableProcessors(),
        System.getProperty("java.version", "unknown"),
        System.getProperty("java.vendor", "unknown"),
        Instant.now());

    return Response.json(details);
  }

  private static MemoryInfo memoryInfo(final MemoryMXBean memory) {
    final var heap = memory.getHeapMemoryUsage();
    final var nonHeap = memory.getNonHeapMemoryUsage();

    return new MemoryInfo(
        heap.getUsed(),
        heap.getMax(),
        heap.getCommitted(),
        nonHeap.getUsed(),
        nonHeap.getCommitted());
  }

  private static String formatUptime(final long uptimeMillis) {
    final var days = uptimeMillis / 86_400_000;
    final var hours = (uptimeMillis % 86_400_000) / 3_600_000;
    final var minutes = (uptimeMillis % 3_600_000) / 60_000;
    final var seconds = (uptimeMillis % 60_000) / 1_000;

    final var sb = new StringBuilder();
    if (days > 0)
      sb.append(days).append("d ");
    if (hours > 0)
      sb.append(hours).append("h ");
    if (minutes > 0)
      sb.append(minutes).append("m ");

    return sb.append(seconds).append('s').toString();
  }
}
