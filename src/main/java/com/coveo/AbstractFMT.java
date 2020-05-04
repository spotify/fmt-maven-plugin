package com.coveo;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.googlejavaformat.java.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractFMT extends AbstractMojo {

  @Parameter(
    defaultValue = "${project.build.sourceDirectory}",
    property = "sourceDirectory",
    required = true
  )
  private File sourceDirectory;

  @Parameter(
    defaultValue = "${project.build.testSourceDirectory}",
    property = "testSourceDirectory",
    required = true
  )
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

  private List<String> filesProcessed = new CopyOnWriteArrayList<>();
  private int nonComplyingFiles;

  /**
   * execute.
   *
   * @throws org.apache.maven.plugin.MojoExecutionException if any.
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
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
    List<File> directoriesToFormat = new ArrayList<File>();
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

    JavaFormatterOptions.Style style = style();
    Formatter formatter = getFormatter(style);

    for (File directoryToFormat : directoriesToFormat) {
      formatSourceFilesInDirectory(directoryToFormat, formatter, style);
    }

    logNumberOfFilesProcessed();
    postExecute(this.filesProcessed, this.nonComplyingFiles);
  }

  /**
   * Post Execute action. It is called at the end of the execute method. Subclasses can add extra
   * checks.
   *
   * @param filesProcessed the list of processed files by the formatter
   * @param nonComplyingFiles the number of files that are not compliant
   * @throws MojoFailureException if there is an exception
   */
  protected void postExecute(List<String> filesProcessed, int nonComplyingFiles)
      throws MojoFailureException {}

  /**
   * Getter for the field <code>filesProcessed</code>.
   *
   * @return a {@link java.util.List} object.
   */
  public List<String> getFilesProcessed() {
    return filesProcessed;
  }

  public void formatSourceFilesInDirectory(
      File directory, Formatter formatter, JavaFormatterOptions.Style style)
      throws MojoFailureException {
    if (!directory.isDirectory()) {
      getLog().info("Directory '" + directory + "' is not a directory. Skipping.");
      return;
    }

    try (Stream<Path> paths = Files.walk(Paths.get(directory.getPath()))) {
      FileFilter fileNameFilter = getFileNameFilter();
      FileFilter pathFilter = getPathFilter();
      paths
          .collect(Collectors.toList())
          .parallelStream()
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .filter(fileNameFilter::accept)
          .filter(pathFilter::accept)
          .forEach(file -> formatSourceFile(file, formatter, style));
    } catch (IOException exception) {
      throw new MojoFailureException(exception.getMessage());
    }
  }

  private JavaFormatterOptions.Style style() throws MojoFailureException {
    if ("aosp".equalsIgnoreCase(style)) {
      getLog().debug("Using AOSP style");
      return JavaFormatterOptions.Style.AOSP;
    }
    if ("google".equalsIgnoreCase(style)) {
      getLog().debug("Using Google style");
      return JavaFormatterOptions.Style.GOOGLE;
    }
    String message = "Unknown style '" + style + "'. Expected 'google' or 'aosp'.";
    getLog().error(message);
    throw new MojoFailureException(message);
  }

  private Formatter getFormatter(JavaFormatterOptions.Style style) throws MojoFailureException {
    return new Formatter(JavaFormatterOptions.builder().style(style).build());
  }

  private FileFilter getFileNameFilter() {
    if (verbose) {
      getLog().debug("Filter files on '" + filesNamePattern + "'.");
    }
    return pathname -> pathname.isDirectory() || pathname.getName().matches(filesNamePattern);
  }

  private FileFilter getPathFilter() {
    if (verbose) {
      getLog().debug("Filter paths on '" + filesPathPattern + "'.");
    }
    return pathname -> pathname.isDirectory() || pathname.getPath().matches(filesPathPattern);
  }

  private void formatSourceFile(File file, Formatter formatter, JavaFormatterOptions.Style style) {
    if (file.isDirectory()) {
      getLog().info("File '" + file + "' is a directory. Skipping.");
      return;
    }

    if (verbose) {
      getLog().debug("Formatting '" + file + "'.");
    }

    CharSource source = com.google.common.io.Files.asCharSource(file, Charsets.UTF_8);
    try {
      String input = source.read();
      String formatted = formatter.formatSource(input);
      formatted = RemoveUnusedImports.removeUnusedImports(formatted);
      if (!skipSortingImports) {
        formatted = ImportOrderer.reorderImports(formatted, style);
      }
      if (!input.equals(formatted)) {
        onNonComplyingFile(file, formatted);
        nonComplyingFiles += 1;
      }
      filesProcessed.add(file.getAbsolutePath());
      if (filesProcessed.size() % 100 == 0) {
        logNumberOfFilesProcessed();
      }
    } catch (FormatterException | IOException e) {
      getLog().warn("Failed to format file '" + file + "'.", e);
    }
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

  protected void logNumberOfFilesProcessed() {
    getLog()
        .info(
            String.format(
                "Processed %d files (%d %s).",
                filesProcessed.size(), nonComplyingFiles, getProcessingLabel()));
  }

  /**
   * Hook called when the processd file is not compliant with the formatter.
   *
   * @param file the file that is not compliant
   * @param formatted the corresponding formatted of the file.
   */
  protected abstract void onNonComplyingFile(File file, String formatted) throws IOException;

  /**
   * Provides the name of the label used when a non-formatted file is found.
   *
   * @return the label to use in the log
   */
  protected abstract String getProcessingLabel();
}
