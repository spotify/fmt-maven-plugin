package com.coveo;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import com.google.googlejavaformat.java.RemoveUnusedImports.JavadocOnlyImports;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractFMT extends AbstractMojo {
  private Log logger = getLog();
  private Formatter formatter = new Formatter();

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

  @Parameter(property = "additionalSourceDirectories")
  private File[] additionalSourceDirectories;

  @Parameter(defaultValue = "false", property = "verbose")
  private boolean verbose;

  @Parameter(defaultValue = "false", property = "failOnUnknownFolder")
  private boolean failOnUnknownFolder;

  @Parameter(defaultValue = ".*\\.java", property = "filesNamePattern")
  private String filesNamePattern;

  private List<String> filesFormatted = new ArrayList<String>();
  private int nonComplyingFiles;

  /**
   * execute.
   *
   * @throws org.apache.maven.plugin.MojoExecutionException if any.
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
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

    for (File directoryToFormat : directoriesToFormat) {
      formatSourceFilesInDirectory(directoryToFormat);
    }

    maybeFailIfNonComplying();
    logNumberOfFilesFormatted();
  }

  /**
   * Getter for the field <code>filesFormatted</code>.
   *
   * @return a {@link java.util.List} object.
   */
  public List<String> getFilesFormatted() {
    return filesFormatted;
  }

  protected abstract boolean isValidateOnly();

  private void formatSourceFilesInDirectory(File directory) {
    if (!directory.isDirectory()) {
      logger.info("Directory '" + directory + "' is not a directory. Skipping.");
      return;
    }

    List<File> files = Arrays.asList(directory.listFiles(getFileFilter()));
    for (File file : files) {
      if (file.isDirectory()) {
        formatSourceFilesInDirectory(file);
      } else {
        formatSourceFile(file);
      }
    }
  }

  private FileFilter getFileFilter() {
    if (verbose) {
      logger.debug("Filter files on '" + filesNamePattern + "'.");
    }
    return new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().matches(filesNamePattern);
      }
    };
  }

  private void formatSourceFile(File file) {
    if (file.isDirectory()) {
      logger.info("File '" + file + "' is a directory. Skipping.");
      return;
    }

    if (verbose) {
      logger.debug("Formatting '" + file + "'.");
    }

    CharSource source = Files.asCharSource(file, Charsets.UTF_8);
    CharSink sink = Files.asCharSink(file, Charsets.UTF_8);
    try {
      String input = source.read();
      String formatted = formatter.formatSource(input);
      formatted = RemoveUnusedImports.removeUnusedImports(formatted, JavadocOnlyImports.KEEP);
      formatted = ImportOrderer.reorderImports(formatted);
      if (!input.equals(formatted)) {
        if (!isValidateOnly()) {
          sink.write(formatted);
        }
        nonComplyingFiles += 1;
      }
      filesFormatted.add(file.getAbsolutePath());
      if (filesFormatted.size() % 100 == 0) {
        logNumberOfFilesFormatted();
      }
    } catch (FormatterException e) {
      logger.warn("Failed to format file '" + file + "'.", e);
    } catch (IOException e) {
      logger.warn("Failed to format file '" + file + "'.", e);
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
      logger.error(message);
      throw new MojoFailureException(message);
    } else {
      logger.warn(
          directoryDisplayName + " directory '" + directory + "' does not exist, ignoring.");
    }
  }

  private void logNumberOfFilesFormatted() {
    logger.info(
        String.format(
            "Processed %d files (%d %s).",
            filesFormatted.size(),
            nonComplyingFiles,
            (isValidateOnly() ? "non-complying" : "reformatted")));
  }

  private void maybeFailIfNonComplying() throws MojoFailureException {
    if (isValidateOnly() && nonComplyingFiles > 0) {
      String message =
          "Found "
              + nonComplyingFiles
              + " non-complying files, failing build (validateOnly is true)";
      logger.error(message);
      throw new MojoFailureException(message);
    }
  }
}
