package com.spotify.fmt;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * FMT class.
 *
 * @author guisim
 * @version $Id: $Id
 */
@Mojo(name = "format", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class FMT extends AbstractFMT {

  @Override
  protected boolean shouldWriteReformattedFiles() {
    return true;
  }

  /**
   * Provides the name of the label used when a non-formatted file is found.
   *
   * @return the label to use in the log
   */
  @Override
  protected String getProcessingLabel() {
    return "reformatted";
  }
}
