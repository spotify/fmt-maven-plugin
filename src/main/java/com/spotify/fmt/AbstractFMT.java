/*-
 * -\-\-
 * com.spotify.fmt:fmt-maven-plugin
 * --
 * Copyright (C) 2016 - 2023 Spotify AB
 * --
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * -/-/-
 */

package com.spotify.fmt;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

  @Parameter(defaultValue = "false", property = "skipSourceDirectory")
  private boolean skipSourceDirectory = false;

  @Parameter(defaultValue = "false", property = "skipTestSourceDirectory")
  private boolean skipTestSourceDirectory = false;

  @Parameter(defaultValue = "false", property = "skipSortingImports")
  private boolean skipSortingImports = false;

  @Parameter(defaultValue = "google", property = "style")
  private String style;

  /**
   * Option to specify whether to run google-java-format in a fork or in-process. Can be {@code
   * default}, {@code never} and {@code always}. Also adds JVM arguments when needed.
   *
   * <p>Specifying {@code default} (which is the default) will fork when JDK 16+ is detected.
   * Specifying {@code never} will never fork and instead run in-process, regardless of JDK version.
   * Specifying {@code always} will always fork, regardless of JDK version.<br>
   */
  @Parameter(defaultValue = "default", property = "fmt.forkMode")
  private String forkMode;

  @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
  private Map<String, Artifact> pluginArtifactMap;

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
    if (sourceDirectory.exists() && !skipSourceDirectory) {
      directoriesToFormat.add(sourceDirectory);
    } else {
      handleMissingDirectory("Source", sourceDirectory);
    }
    if (testSourceDirectory.exists() && !skipTestSourceDirectory) {
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

  @VisibleForTesting
  boolean shouldFork() {
    switch (forkMode) {
      case "default":
      case "always":
        return true;
      case "never":
        return false;
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

  private List<String> javaArgs() {
    // https://github.com/google/google-java-format/blame/13ca73ebbfa86f6aca5f86be16e6829de6d5014c/pom.xml#L238
    return Arrays.asList(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED");
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
