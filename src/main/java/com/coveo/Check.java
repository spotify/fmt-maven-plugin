package com.coveo;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

  /** List of unformatted files. */
  private List<String> filesNotFormatted = new ArrayList<String>();

  /**
   * Post Execute action. It is called at the end of the execute method. Subclasses can add extra
   * checks.
   *
   * @param filesProcessed the list of processed files by the formatter
   * @param nonComplyingFiles the number of files that are not compliant
   * @throws MojoFailureException if there is an exception
   */
  @Override
  protected void postExecute(List<String> filesProcessed, int nonComplyingFiles)
      throws MojoFailureException {
    if (nonComplyingFiles > 0) {
      String message = "Found " + nonComplyingFiles + " non-complying files, failing build";
      getLog().error(message);
      getLog().error("To fix formatting errors, run \"mvn com.coveo:fmt-maven-plugin:format\"");
      // do not support limit < 1
      displayLimit = max(1, displayLimit);

      // Display first displayLimit files not formatted
      if (displayFiles) {
        for (String path :
            filesNotFormatted.subList(0, min(displayLimit, filesNotFormatted.size()))) {
          getLog().error("Non complying file: " + path);
        }

        if (nonComplyingFiles > displayLimit) {
          getLog().error(format("... and %d more files.", nonComplyingFiles - displayLimit));
        }
      }
      throw new MojoFailureException(message);
    }
  }

  /**
   * Hook called when the processd file is not compliant with the formatter.
   *
   * @param file the file that is not compliant
   * @param formatted the corresponding formatted of the file.
   */
  @Override
  protected void onNonComplyingFile(final File file, final String formatted) throws IOException {
    filesNotFormatted.add(file.getAbsolutePath());
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
