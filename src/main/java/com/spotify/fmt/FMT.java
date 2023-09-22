/*-
 * -\-\-
 * com.spotify.fmt:fmt-maven-plugin
 * --
 * Copyright (C) 2016 - 2023 Spotify AB
 * --
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * -/-/-
 */

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
