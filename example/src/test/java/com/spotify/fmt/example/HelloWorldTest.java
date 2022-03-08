package com.spotify.fmt.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HelloWorldTest {

  @Test
  public void greeting() {
    assertEquals("Hello World!", HelloWorld.getGreeting());
  }
}
