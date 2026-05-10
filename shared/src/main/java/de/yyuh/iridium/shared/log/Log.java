package de.yyuh.iridium.shared.log;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Log {

  private static final String PREFIX = "iridium.";

  private static final String RESET = "\u001B[0m";
  private static final String GREY = "\u001B[90m";
  private static final String CYAN = "\u001B[36m";
  private static final String YELLOW = "\u001B[33m";
  private static final String RED = "\u001B[31m";
  private static final String BLUE = "\u001B[34m";
  private static final String BOLD = "\u001B[1m";
  private static final String DIM = "\u001B[2m";

  static {
    final Logger root = Logger.getLogger("iridium");

    root.setUseParentHandlers(false);
    root.setLevel(Level.ALL);

    final ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.ALL);
    handler.setFormatter(new CompactFormatter());

    root.addHandler(handler);
  }

  private final Logger logger;

  private Log(final Logger logger) {
    this.logger = logger;
  }

  public static Log of(final Class<?> type) {
    return of(type.getSimpleName());
  }

  public static Log of(final String name) {
    return new Log(Logger.getLogger(PREFIX + name));
  }

  public void info(final String format, final Object... args) {
    log(Level.INFO, format, args);
  }

  public void warn(final String format, final Object... args) {
    log(Level.WARNING, format, args);
  }

  public void debug(final String format, final Object... args) {
    log(Level.FINE, format, args);
  }

  public void error(final String format, final Object... args) {
    log(Level.SEVERE, format, args);
  }

  public void error(final String message, final Throwable throwable) {
    logger.log(Level.SEVERE, message, throwable);
  }

  private void log(final Level level, final String format, final Object... args) {
    logger.log(level, args.length == 0 ? format : String.format(format, args));
  }

  private static final class CompactFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(final LogRecord record) {
      final String timestamp = ZonedDateTime
          .ofInstant(record.getInstant(), ZoneId.systemDefault())
          .format(TIMESTAMP_FORMAT);

      final String loggerName = stripPrefix(record.getLoggerName());

      final String levelColor;
      final String levelLabel;

      final Level level = record.getLevel();

      if (level == Level.SEVERE) {
        levelColor = RED;
        levelLabel = "ERROR";
      } else if (level == Level.WARNING) {
        levelColor = YELLOW;
        levelLabel = "WARN ";
      } else if (level == Level.FINE
          || level == Level.FINER
          || level == Level.FINEST) {
        levelColor = CYAN;
        levelLabel = "DEBUG";
      } else {
        levelColor = BLUE;
        levelLabel = "INFO ";
      }

      final StringBuilder builder = new StringBuilder()
          .append(GREY).append(timestamp).append(RESET)
          .append("  ")
          .append(levelColor).append(BOLD).append(levelLabel).append(RESET)
          .append("  ")
          .append(BOLD).append(loggerName).append(RESET)
          .append(DIM).append("  -  ").append(RESET)
          .append(formatMessage(record))
          .append('\n');

      appendThrowable(builder, record.getThrown());

      return builder.toString();
    }

    private static String stripPrefix(final String name) {
      return name.startsWith(PREFIX)
          ? name.substring(PREFIX.length())
          : name;
    }

    private static void appendThrowable(
        final StringBuilder builder,
        final Throwable throwable) {
      if (throwable == null) {
        return;
      }

      builder.append(RED)
          .append(throwable.getClass().getName())
          .append(": ")
          .append(throwable.getMessage())
          .append(RESET)
          .append('\n');

      for (final StackTraceElement element : throwable.getStackTrace()) {
        builder.append(DIM)
            .append('\t')
            .append(element)
            .append(RESET)
            .append('\n');
      }
    }
  }
}
