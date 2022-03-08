package com.spotify.fmt;

import org.apache.maven.plugin.logging.SystemStreamLog;

class Logger extends SystemStreamLog {

  volatile boolean debug;

  @Override
  public boolean isDebugEnabled() {
    return debug;
  }

  @Override
  public void debug(CharSequence content) {
    if (isDebugEnabled()) {
      super.debug(content);
    }
  }

  @Override
  public void debug(CharSequence content, Throwable error) {
    if (isDebugEnabled()) {
      super.debug(content, error);
    }
  }

  @Override
  public void debug(Throwable error) {
    if (isDebugEnabled()) {
      super.debug(error);
    }
  }
}
