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

  private List<String> filesProcessed = new CopyOnWriteArrayList<>();
  private int nonComplyingFiles;

  /**
   * execute.
   *
   * @throws org.apache.maven.plugin.MojoExecutionException if any.
   */
  @Override
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

    JavaFormatterOptions.Style style = style();

    for (File directoryToFormat : directoriesToFormat) {
      formatSourceFilesInDirectory(directoryToFormat, style);
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

  public void formatSourceFilesInDirectory(File directory, JavaFormatterOptions.Style style)
      throws MojoFailureException {
    if (!directory.isDirectory()) {
      getLog().info("Directory '" + directory + "' is not a directory. Skipping.");
      return;
    }

    try (Stream<Path> paths = Files.walk(Paths.get(directory.getPath()))) {
      FileFilter fileNameFilter = getFileNameFilter();
      FileFilter pathFilter = getPathFilter();
      long failures =
          paths.collect(Collectors.toList()).stream()
              .filter(p -> p.toFile().exists())
              .map(Path::toFile)
              .filter(fileNameFilter::accept)
              .filter(pathFilter::accept)
              .map(file -> formatSourceFile(file, style))
              .filter(r -> !r)
              .count();

      if (failures > 0) {
        throw new MojoFailureException(
            "There where errors when formatting files. Error count: " + failures);
      }
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

  private Formatter getFormatter(JavaFormatterOptions.Style style) {
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

  private boolean formatSourceFile(File file, JavaFormatterOptions.Style style) {
    if (file.isDirectory()) {
      if (verbose) {
        getLog().debug("File '" + file + "' is a directory. Skipping.");
      }
      return true;
    }

    if (verbose) {
      getLog().debug("Formatting '" + file + "'.");
    }

    return this.formatSourceFileUsingExternalProcess(file, style);
    // return this.formatSourceFileInProcess(file, style);
  }

  private boolean formatSourceFileInProcess(File file, JavaFormatterOptions.Style style) {
    Formatter formatter = getFormatter(style);

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
      getLog().error("Failed to format file '" + file + "'.", e);
      return false;
    }
    return true;
  }

  private boolean formatSourceFileUsingExternalProcess(
      File file, JavaFormatterOptions.Style style) {
    String os = System.getProperty("os.name");
    assert os != null;

    String javaHome = System.getProperty("java.home");
    assert javaHome != null;

    File javaExecutable =
        new File(new File(javaHome), os.equals("Windows") ? "bin\\java.exe" : "bin/java");
    assert file.exists();

    String classPath = System.getProperty("java.class.path");
    assert classPath != null;

    ClassLoader loader = style.getClass().getPackage().;

    try {
      String command =
          javaExecutable.getAbsolutePath()
              + " "
              + "--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED "
              + "--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED "
              + "--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED "
              + "--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED "
              + "--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED "
              + "-cp \""
              + classPath
              + "\" "
              + "com.google.googlejavaformat.java "
              + "\""
              + file.getAbsolutePath()
              + "\"";

      getLog().info("Executing command: " + command);

      Process process = Runtime.getRuntime().exec(command);

      if (process.waitFor()      == 0) {
        getLog().error("Failed to format file '" + file + "'.");
        return false;
      }
    } catch (IOException ex) {
      getLog().error("Cannot run the Google Java Formatter CLI", ex);
      return false;
    } catch (InterruptedException ex) {
      return false;
    }

    return true;
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
   * Hook called when the processed file is not compliant with the formatter.
   *
   * @param file the file that is not compliant
   * @param formatted the corresponding formatted of the file.
   * @throws IOException on any
   */
  protected abstract void onNonComplyingFile(File file, String formatted) throws IOException;

  /**
   * Provides the name of the label used when a non-formatted file is found.
   *
   * @return the label to use in the log
   */
  protected abstract String getProcessingLabel();
}
