package com.coveo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class Check extends AbstractFMT {

  @Override
  protected boolean isValidateOnly() {
    return true;
  }
}
