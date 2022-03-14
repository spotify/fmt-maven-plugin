package com.spotify.fmt;

import org.apache.maven.plugin.logging.Log;

class Logging {

  private static final Logger log = new Logger();

  static Log getLog() {
    return log;
  }

  public static void configure(boolean debugLoggingEnabled) {
    log.debug = debugLoggingEnabled;
  }
}
