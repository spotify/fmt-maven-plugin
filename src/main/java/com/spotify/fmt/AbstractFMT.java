package com.spotify.fmt;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
            .directoriesToFormat(directoriesToFormat)
            .style(style)
            .filesNamePattern(filesNamePattern)
            .filesPathPattern(filesPathPattern)
            .verbose(verbose)
            .skipSortingImports(skipSortingImports)
            .writeReformattedFiles(shouldWriteReformattedFiles())
            .processingLabel(getProcessingLabel())
            .build();

    final boolean debugLoggingEnabled = getLog().isDebugEnabled();
    try (ForkingExecutor executor = new ForkingExecutor(getLog())) {
      executor.javaArgs(javaArgs());
      try {
        result =
            executor.execute(
                () -> {
                  Logging.configure(debugLoggingEnabled);
                  Formatter formatter = new Formatter(configuration);
                  return formatter.format();
                });
      } catch (Exception e) {
        throw new MojoFailureException(e);
      }
    }

    postExecute(result);
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
}
