package com.spotify.fmt;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

class Logging {

  private static volatile Log log = new DefaultLog(new ConsoleLogger());

  static Log getLog() {
    return log;
  }

  public static void configure(boolean debugLoggingEnabled) {
    log =
        new DefaultLog(
            new ConsoleLogger(
                debugLoggingEnabled ? Logger.LEVEL_DEBUG : Logger.LEVEL_INFO, "console"));
  }
}
