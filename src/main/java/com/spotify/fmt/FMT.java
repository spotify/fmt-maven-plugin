package com.spotify.fmt;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
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

  /**
   * Hook called when the processd file is not compliant with the formatter.
   *
   * @param file the file that is not compliant
   * @param formatted the corresponding formatted of the file.
   */
  @Override
  protected void onNonComplyingFile(File file, String formatted) throws IOException {
    CharSink sink = Files.asCharSink(file, Charsets.UTF_8);
    sink.write(formatted);
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
