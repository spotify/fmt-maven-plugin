package com.spotify.fmt;

import static java.lang.Math.max;
import static java.lang.String.format;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Check mojo that will ensure all files are formatted. If some files are not formatted, an
 * exception is thrown.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class Check extends AbstractFMT {

  /** Flag to display or not the files that are not compliant. */
  @Parameter(defaultValue = "true", property = "displayFiles")
  private boolean displayFiles;

  /** Limit the number of non-complying files to display */
  @Parameter(defaultValue = "100", property = "displayLimit")
  private int displayLimit;

  /**
   * Post Execute action. It is called at the end of the execute method. Subclasses can add extra
   * checks.
   *
   * @param result The formatting result
   * @throws MojoFailureException if there is an exception
   */
  @Override
  protected void postExecute(FormattingResult result) throws MojoFailureException {
    if (!result.nonComplyingFiles().isEmpty()) {
      String message =
          "Found " + result.nonComplyingFiles().stream() + " non-complying files, failing build";
      getLog().error(message);
      getLog()
          .error("To fix formatting errors, run \"mvn com.spotify.fmt:fmt-maven-plugin:format\"");
      // do not support limit < 1
      displayLimit = max(1, displayLimit);

      // Display first displayLimit files not formatted
      if (displayFiles) {
        result.nonComplyingFiles().stream()
            .limit(displayLimit)
            .forEach(path -> getLog().error("Non complying file: " + path));

        if (result.nonComplyingFiles().size() > displayLimit) {
          getLog()
              .error(
                  format(
                      "... and %d more files.", result.nonComplyingFiles().size() - displayLimit));
        }
      }
      throw new MojoFailureException(message);
    }
  }

  @Override
  protected boolean shouldWriteReformattedFiles() {
    return false;
  }

  /**
   * Provides the name of the label used when a non-formatted file is found.
   *
   * @return the label to use in the log
   */
  @Override
  protected String getProcessingLabel() {
    return "non-complying";
  }
}
