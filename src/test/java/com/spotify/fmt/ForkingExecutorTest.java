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

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForkingExecutorTest {

  @Rule public ExpectedException exception = ExpectedException.none();

  private ForkingExecutor forkingExecutor;

  @Before
  public void setUp() {
    forkingExecutor = new ForkingExecutor(new SilentLog());
  }

  @Test
  public void returnsResult() throws IOException {
    final String result = forkingExecutor.execute(() -> "hello world!");
    assertThat(result).isEqualTo("hello world!");
  }

  @Test
  public void propagatesException() throws IOException {
    exception.expect(FoobarException.class);
    exception.expectMessage("foobar!");
    forkingExecutor.execute(
        () -> {
          throw new FoobarException("foobar!");
        });
  }

  @Test
  public void captures() throws IOException {
    final Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    final Map<String, String> result = forkingExecutor.execute(() -> map);
    assertThat(result).isEqualTo(map);
  }

  @Test
  public void executesInSubprocess() throws IOException {
    final String thisJvm = ManagementFactory.getRuntimeMXBean().getName();
    final String subprocessJvm =
        forkingExecutor.execute(() -> ManagementFactory.getRuntimeMXBean().getName());
    assertThat(thisJvm).isNotEqualTo(subprocessJvm);
  }

  @Test
  public void setsEnvironment() throws IOException {
    final String result =
        forkingExecutor
            .environment(Collections.singletonMap("foo", "bar"))
            .execute(() -> System.getenv("foo"));
    assertThat(result).isEqualTo("bar");
  }

  @Test
  public void setsJavaArgs() throws IOException {
    final String result =
        forkingExecutor.javaArgs("-Dfoo=bar").execute(() -> System.getProperty("foo"));
    assertThat(result).isEqualTo("bar");
  }

  private static class FoobarException extends RuntimeException {

    FoobarException(String message) {
      super(message);
    }
  }
}
