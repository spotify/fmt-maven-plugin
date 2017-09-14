package com.coveo;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class Check extends AbstractFMT {

  @Parameter(defaultValue = "true", property = "displayFiles")
  private boolean displayFiles;

  @Parameter(defaultValue = "100", property = "displayLimit")
  private int displayLimit;

  protected List<String> filesNotFormatted = new ArrayList<String>();

  @Override
  protected boolean isValidateOnly() {
    return true;
  }

  /** Hook called when a file is not compliant. */
  protected void nonComplyingFile(File file) {
    if (isValidateOnly()) {
      filesNotFormatted.add(file.getAbsolutePath());
    }
  }

  /** Display some statistics on the files that are not complying */
  protected void additionalFailComplying() {
    if (isValidateOnly() && nonComplyingFiles > 0) {

      // do not support limit < 1
      displayLimit = max(1, displayLimit);

      // Display first displayLimit files not formatted
      if (displayFiles) {
        if (nonComplyingFiles > displayLimit) {
          logger.error("Only printing the first " + displayLimit + " non-complying files.");
        }
        for (String path :
            filesNotFormatted.subList(0, min(displayLimit, filesNotFormatted.size()))) {
          logger.error("Non complying file: " + path);
        }
      }
    }
  }
}
