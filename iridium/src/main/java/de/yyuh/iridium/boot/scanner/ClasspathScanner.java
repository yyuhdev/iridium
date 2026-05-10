package de.yyuh.iridium.boot.scanner;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

/**
 * Scans the classpath for classes and annotations.
 *
 * <p>Iterates over every entry in {@code java.class.path}, recursing
 * into directories and {@code .jar} files. Useful for discovering
 * annotated controllers, middleware, and components at startup.</p>
 *
 * <p>This class cannot be instantiated.</p>
 */
@NullMarked
public final class ClasspathScanner {

  private ClasspathScanner() {
  }

  /**
   * Returns all classes reachable from the current classpath.
   *
   * @return a (possibly empty) set of loaded classes
   */
  public static Set<Class<?>> allClasses() {
    final Set<Class<?>> classes = new LinkedHashSet<>();

    for (final String entry : classpathEntries()) {
      final File file = new File(entry);

      if (!file.exists()) {
        continue;
      }

      if (file.isDirectory()) {
        classes.addAll(scanDirectory(file.toPath()));
      }

      if (entry.endsWith(".jar")) {
        classes.addAll(scanJar(file));
      }
    }
    return classes;
  }

  /**
   * Returns all classpath classes annotated with the given annotation.
   *
   * @param annotation the annotation type to filter by
   * @return a set of annotated classes
   */
  public static Set<Class<?>> withAnnotation(final Class<? extends Annotation> annotation) {
    return allClasses().stream()
        .filter(c -> c.isAnnotationPresent(annotation))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Returns all methods annotated (directly or via meta-annotation)
   * with the given annotation.
   *
   * @param annotation the annotation type to filter by
   * @return a set of matching methods
   */
  public static Set<Method> methodsWithAnnotation(final Class<? extends Annotation> annotation) {
    return allClasses().stream()
        .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
        .filter(m -> m.isAnnotationPresent(annotation))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String[] classpathEntries() {
    return System.getProperty("java.class.path", "").split(File.pathSeparator);
  }

  private static Set<Class<?>> scanDirectory(final Path root) {
    try (final var walk = Files.walk(root)) {
      return walk
          .filter(p -> p.toString().endsWith(".class"))
          .map(p -> toClassName(root, p))
          .map(ClasspathScanner::tryLoad)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    } catch (final IOException e) {
      throw new ClasspathScanException("Failed to scan directory: " + root, e);
    }
  }

  private static String toClassName(final Path root, final Path classFile) {
    return root.relativize(classFile)
        .toString()
        .replace(File.separatorChar, '.')
        .replaceAll("\\.class$", "");
  }

  private static Set<Class<?>> scanJar(final File jar) {
    try (final var jarFile = new JarFile(jar)) {
      return jarFile.stream()
          .map(entry -> entry.getName())
          .filter(name -> name.endsWith(".class"))
          .map(ClasspathScanner::jarEntryToClassName)
          .map(ClasspathScanner::tryLoad)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    } catch (final IOException e) {
      throw new ClasspathScanException("Failed to scan JAR: " + jar, e);
    }
  }

  private static String jarEntryToClassName(final String entryName) {
    return entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
  }

  private static Optional<Class<?>> tryLoad(final String className) {
    try {
      return Optional.of(Class.forName(className));
    } catch (final ClassNotFoundException | NoClassDefFoundError ignored) {
      return Optional.empty();
    }
  }

  /**
   * Thrown when the classpath cannot be traversed (I/O errors in
   * directory walking or JAR reading).
   */
  public static final class ClasspathScanException extends RuntimeException {

    /**
     * Constructs a new exception with a message and cause.
     *
     * @param message the error description
     * @param cause   the underlying cause
     */
    public ClasspathScanException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
