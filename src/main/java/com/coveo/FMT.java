package com.coveo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * FMT class.
 *
 * @author guisim
 * @version $Id: $Id
 */
@Mojo(name = "format", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class FMT extends AbstractFMT {

  /**
   * @deprecated Use the {@code fmt:check} goal instead.
   */
  @Deprecated
  @Parameter(defaultValue = "false", property = "validateOnly")
  private boolean validateOnly;

  @Override
  @SuppressWarnings("deprecation")
  public boolean isValidateOnly() {
    return validateOnly;
  }
}
