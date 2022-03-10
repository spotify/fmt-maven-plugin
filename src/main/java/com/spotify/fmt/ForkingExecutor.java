package com.spotify.fmt;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

/**
 * An executor that executes a method in a sub-process JVM.
 *
 * <p>The function, its result and any thrown exception must be serializable as serialization is
 * used to transport these between the processes.
 *
 * <p>Adapted from
 * https://github.com/spotify/flo/blob/91d2e546bc8fa8e6fee9bc8c6dd484d87db3b0af/flo-runner/src/main/java/com/spotify/flo/context/ForkingExecutor.java
 */
class ForkingExecutor implements Closeable {

  private final org.apache.maven.plugin.logging.Log log;

  private final List<Execution<?>> executions = new ArrayList<>();

  private Map<String, String> environment = Collections.emptyMap();
  private List<String> javaArgs = Collections.emptyList();
  private boolean withDefaultClasspath = true;
  private List<String> configuredClasspath = Collections.emptyList();

  public ForkingExecutor(Log log) {
    this.log = log;
  }

  ForkingExecutor environment(Map<String, String> environment) {
    this.environment = new HashMap<>(environment);
    return this;
  }

  ForkingExecutor javaArgs(String... javaArgs) {
    return javaArgs(Arrays.asList(javaArgs));
  }

  ForkingExecutor javaArgs(List<String> javaArgs) {
    this.javaArgs = new ArrayList<>(javaArgs);
    return this;
  }

  ForkingExecutor classpath(Collection<String> classpath) {
    this.configuredClasspath = new ArrayList<>(classpath);
    return this;
  }

  ForkingExecutor withDefaultClasspath(boolean withDefaultClasspath) {
    this.withDefaultClasspath = withDefaultClasspath;
    return this;
  }

  private List<String> defaultClasspath() {
    return Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator));
  }

  private List<String> executionClassPath() {
    return withDefaultClasspath
        ? Stream.concat(configuredClasspath.stream(), defaultClasspath().stream())
            .collect(Collectors.toList())
        : configuredClasspath;
  }

  /**
   * Execute a function in a sub-process.
   *
   * @param f The function to execute.
   * @return The return value of the function. Any exception thrown by the function the will be
   *     propagated and re-thrown.
   * @throws IOException if
   */
  <T> T execute(SerializableCallable<T> f) throws IOException {
    try (final Execution<T> execution = new Execution<>(executionClassPath(), f)) {
      executions.add(execution);
      execution.start();
      return execution.waitFor();
    }
  }

  @Override
  public void close() {
    executions.forEach(Execution::close);
  }

  private class Execution<T> implements Closeable {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Path tempdir = Files.createTempDirectory("fmt-maven-plugin");

    private final Path workdir = Files.createDirectory(tempdir.resolve("workdir"));

    private final Path closureFile = tempdir.resolve("closure");
    private final Path resultFile = tempdir.resolve("result");
    private final Path errorFile = tempdir.resolve("error");

    private final String home = System.getProperty("java.home");
    private final Path java = Paths.get(home, "bin", "java").toAbsolutePath().normalize();

    private final List<String> classpath;

    private final SerializableCallable<T> f;

    private Process process;

    Execution(List<String> classpath, SerializableCallable<T> f) throws IOException {
      this.classpath = classpath;
      this.f = Objects.requireNonNull(f);
    }

    void start() {
      if (process != null) {
        throw new IllegalStateException();
      }
      log.debug("serializing closure");
      try {
        Serialization.serialize(f, closureFile);
      } catch (SerializationException e) {
        throw new RuntimeException("Failed to serialize closure", e);
      }

      final String classPathArg = String.join(File.pathSeparator, classpath);

      final ProcessBuilder processBuilder =
          new ProcessBuilder(java.toString(), "-cp", classPathArg).directory(workdir.toFile());

      // Propagate -Xmx and -D.
      ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
          .filter(s -> s.startsWith("-Xmx") || s.startsWith("-D"))
          .forEach(processBuilder.command()::add);

      // Custom jvm args
      javaArgs.forEach(processBuilder.command()::add);

      // Trampoline arguments
      processBuilder.command().add(Trampoline.class.getName());
      processBuilder.command().add(closureFile.toString());
      processBuilder.command().add(resultFile.toString());
      processBuilder.command().add(errorFile.toString());

      processBuilder.environment().putAll(environment);

      log.debug(
          MessageFormat.format(
              "Starting subprocess: environment={0}, command={1}, directory={2}",
              processBuilder.environment(), processBuilder.command(), processBuilder.directory()));
      try {
        process = processBuilder.start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      // Copy std{err,out} line by line to avoid interleaving and corrupting line contents.
      executor.submit(() -> copyLines(process.getInputStream(), System.out));
      executor.submit(() -> copyLines(process.getErrorStream(), System.err));
    }

    T waitFor() {
      if (process == null) {
        throw new IllegalStateException();
      }
      log.debug("Waiting for subprocess exit");
      final int exitValue;
      try {
        exitValue = process.waitFor();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } finally {
        process.destroyForcibly();
      }

      log.debug("Subprocess exited: " + exitValue);
      if (exitValue != 0) {
        throw new RuntimeException("Subprocess failed: " + process.exitValue());
      }

      if (Files.exists(errorFile)) {
        // Failed
        log.debug("Subprocess exited with error file");
        final Throwable error;
        try {
          error = Serialization.deserialize(errorFile);
        } catch (SerializationException e) {
          throw new RuntimeException("Failed to deserialize error", e);
        }
        if (error instanceof Error) {
          throw (Error) error;
        } else if (error instanceof RuntimeException) {
          throw (RuntimeException) error;
        } else {
          throw new RuntimeException(error);
        }
      } else {
        // Success
        log.debug("Subprocess exited with result file");
        final T result;
        try {
          result = Serialization.deserialize(resultFile);
        } catch (SerializationException e) {
          throw new RuntimeException("Failed to deserialize result", e);
        }
        return result;
      }
    }

    @Override
    public void close() {
      if (process != null) {
        process.destroyForcibly();
        process = null;
      }
      executor.shutdown();
      tryDeleteDir(tempdir);
    }
  }

  private void tryDeleteDir(Path path) {
    try {
      deleteDir(path);
    } catch (IOException e) {
      log.warn("Failed to delete directory: " + path, e);
    }
  }

  private static void deleteDir(Path path) throws IOException {
    try {
      Files.walkFileTree(
          path,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              try {
                Files.delete(file);
              } catch (NoSuchFileException ignore) {
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              try {
                Files.delete(dir);
              } catch (NoSuchFileException ignore) {
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (NoSuchFileException ignore) {
    }
  }

  private void copyLines(InputStream in, PrintStream out) {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        out.println(line);
      }
    } catch (IOException e) {
      log.error("Caught exception during stream copy", e);
    }
  }

  private static class Trampoline {

    private static org.apache.maven.plugin.logging.Log log = Logging.getLog();

    private static class Watchdog extends Thread {

      Watchdog() {
        setDaemon(true);
      }

      @Override
      public void run() {
        // Wait for parent to exit.
        try {
          while (true) {
            int c = System.in.read();
            if (c == -1) {
              break;
            }
          }
        } catch (IOException e) {
          log.error("watchdog failed", e);
        }
        log.debug("child process exiting");
        // Exit with non-zero status code to skip shutdown hooks
        System.exit(-1);
      }
    }

    public static void main(String... args) {
      log.debug("child process started: args=" + Arrays.asList(args));
      final Watchdog watchdog = new Watchdog();
      watchdog.start();

      if (args.length != 3) {
        log.error("args.length != 3");
        System.exit(3);
        return;
      }
      final Path closureFile;
      final Path resultFile;
      final Path errorFile;
      try {
        closureFile = Paths.get(args[0]);
        resultFile = Paths.get(args[1]);
        errorFile = Paths.get(args[2]);
      } catch (InvalidPathException e) {
        log.error("Failed to get file path", e);
        System.exit(4);
        return;
      }

      run(closureFile, resultFile, errorFile);
    }

    private static void run(Path closureFile, Path resultFile, Path errorFile) {
      log.debug("deserializing closure: " + closureFile);
      final SerializableCallable<?> fn;
      try {
        fn = Serialization.deserialize(closureFile);
      } catch (SerializationException e) {
        log.error("Failed to deserialize closure: " + closureFile, e);
        System.exit(5);
        return;
      }

      log.debug("executing closure");
      Object result = null;
      Throwable error = null;
      try {
        result = fn.call();
      } catch (Throwable e) {
        error = e;
      }

      if (error != null) {
        log.debug("serializing error", error);
        try {
          Serialization.serialize(error, errorFile);
        } catch (SerializationException e) {
          log.error("failed to serialize error", e);
          System.exit(6);
          return;
        }
      } else {
        log.debug("serializing result: " + result);
        try {
          Serialization.serialize(result, resultFile);
        } catch (SerializationException e) {
          log.error("failed to serialize result", e);
          System.exit(7);
          return;
        }
      }

      System.err.flush();
      System.exit(0);
    }
  }
}
