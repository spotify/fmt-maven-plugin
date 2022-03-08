package com.spotify.fmt;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractFMT extends AbstractMojo {

  @Parameter(
      defaultValue = "${project.build.sourceDirectory}",
      property = "sourceDirectory",
      required = true)
  private File sourceDirectory;

  @Parameter(
      defaultValue = "${project.build.testSourceDirectory}",
      property = "testSourceDirectory",
      required = true)
  private File testSourceDirectory;

  @Parameter(defaultValue = "${project.packaging}", required = true)
  private String packaging;

  @Parameter(property = "additionalSourceDirectories")
  private File[] additionalSourceDirectories;

  @Parameter(defaultValue = "false", property = "verbose")
  private boolean verbose;

  @Parameter(defaultValue = "false", property = "failOnUnknownFolder")
  private boolean failOnUnknownFolder;

  @Parameter(defaultValue = ".*\\.java", property = "filesNamePattern")
  private String filesNamePattern;

  @Parameter(defaultValue = ".*", property = "filesPathPattern")
  private String filesPathPattern;

  @Parameter(defaultValue = "false", property = "fmt.skip")
  private boolean skip = false;

  @Parameter(defaultValue = "false", property = "skipSortingImports")
  private boolean skipSortingImports = false;

  @Parameter(defaultValue = "google", property = "style")
  private String style;

  @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
  private Map<String, Artifact> pluginArtifactMap;

  /**
   * Option to specify whether to run google-java-format in a fork or in-process. Can be {@code default}, {@code never} and {@code always.
   * The {@code default} (which is the default) will fork when JDK 16+ is detected.
   * The {@code never} will never fork and instead run in-process, regardless of JDK version.
   * The {@code always} will always fork, regardless of JDK version.<br>
   */
  @Parameter(defaultValue = "default", property = "fmt.forkMode")
  String forkMode;

  /**
   * Whether to use the classpath from the java.class.path property when forking. Only intended for
   * use by unit tests.
   */
  @VisibleForTesting boolean useDefaultClasspathWhenForking;

  private FormattingResult result;

  /** execute. */
  @Override
  public void execute() throws MojoFailureException {
    if (skip) {
      getLog().info("Skipping format check");
      return;
    }
    if ("pom".equals(packaging)) {
      getLog().info("Skipping format check: project uses 'pom' packaging");
      return;
    }
    if (skipSortingImports) {
      getLog().info("Skipping sorting imports");
    }
    List<File> directoriesToFormat = new ArrayList<>();
    if (sourceDirectory.exists()) {
      directoriesToFormat.add(sourceDirectory);
    } else {
      handleMissingDirectory("Source", sourceDirectory);
    }
    if (testSourceDirectory.exists()) {
      directoriesToFormat.add(testSourceDirectory);
    } else {
      handleMissingDirectory("Test source", testSourceDirectory);
    }

    for (File additionalSourceDirectory : additionalSourceDirectories) {
      if (additionalSourceDirectory.exists()) {
        directoriesToFormat.add(additionalSourceDirectory);
      } else {
        handleMissingDirectory("Additional source", additionalSourceDirectory);
      }
    }

    FormattingConfiguration configuration =
        FormattingConfiguration.builder()
            .debug(getLog().isDebugEnabled())
            .directoriesToFormat(directoriesToFormat)
            .style(style)
            .filesNamePattern(filesNamePattern)
            .filesPathPattern(filesPathPattern)
            .verbose(verbose)
            .skipSortingImports(skipSortingImports)
            .writeReformattedFiles(shouldWriteReformattedFiles())
            .processingLabel(getProcessingLabel())
            .build();

    FormattingCallable formattingCallable = new FormattingCallable(configuration);

    try {
      if (shouldFork()) {
        final List<String> classpath =
            pluginArtifactMap.values().stream()
                .map(a -> a.getFile().getAbsolutePath())
                .collect(Collectors.toList());

        try (ForkingExecutor executor =
            new ForkingExecutor(getLog())
                .javaArgs(javaArgs())
                .classpath(classpath)
                .withDefaultClasspath(useDefaultClasspathWhenForking)) {
          result = executor.execute(formattingCallable);
        }

      } else {
        result = formattingCallable.call();
      }
    } catch (Exception e) {
      throw new MojoFailureException(e);
    }

    postExecute(result);
  }

  private boolean shouldFork() {
    switch (forkMode) {
      case "never":
        return false;
      case "default":
        return hasModuleSystem();
      case "always":
        return true;
      default:
        throw new IllegalArgumentException(
            "Invalid forkMode: " + forkMode + ", must be `default`, `never` or `always`");
    }
  }

  /**
   * Post Execute action. It is called at the end of the execute method. Subclasses can add extra
   * checks.
   *
   * @param result The formatting result
   */
  protected void postExecute(FormattingResult result) throws MojoFailureException {}

  public FormattingResult getResult() {
    return result;
  }

  private void handleMissingDirectory(String directoryDisplayName, File directory)
      throws MojoFailureException {
    if (failOnUnknownFolder) {
      String message =
          directoryDisplayName
              + " directory '"
              + directory
              + "' does not exist, failing build (failOnUnknownFolder is true).";
      getLog().error(message);
      throw new MojoFailureException(message);
    } else {
      getLog()
          .warn(directoryDisplayName + " directory '" + directory + "' does not exist, ignoring.");
    }
  }

  /** Whether to write reformatted files to disk. */
  protected abstract boolean shouldWriteReformattedFiles();

  /**
   * Provides the name of the label used when a non-formatted file is found.
   *
   * @return the label to use in the log
   */
  protected abstract String getProcessingLabel();

  /** Is this JDK 16+? */
  private boolean hasModuleSystem() {
    try {
      Class.forName("java.lang.Module");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private List<String> javaArgs() {
    if (!hasModuleSystem()) {
      return Collections.emptyList();
    }

    return Arrays.asList(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");
  }

  private static class FormattingCallable implements SerializableCallable<FormattingResult> {

    private final FormattingConfiguration configuration;

    FormattingCallable(FormattingConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public FormattingResult call() {
      Logging.configure(configuration.debug());
      Formatter formatter = new Formatter(configuration);
      return formatter.format();
    }
  }
}
