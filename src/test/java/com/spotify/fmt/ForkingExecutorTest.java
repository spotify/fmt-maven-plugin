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

  @Test
  public void propagatesJavaArgs() throws IOException {
    final String result =
        forkingExecutor
            .javaArgs("-Dfoo=bar")
            .execute(
                () -> {
                  // Fork again with an executor without -Dfoo=bar explicitly configured
                  try (ForkingExecutor inner = new ForkingExecutor(new SilentLog())) {
                    // And check that -Dfoo=bar was automatically propagated
                    return inner.execute(() -> System.getProperty("foo"));
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });
    assertThat(result).isEqualTo("bar");
  }

  private static class FoobarException extends RuntimeException {

    FoobarException(String message) {
      super(message);
    }
  }
}
