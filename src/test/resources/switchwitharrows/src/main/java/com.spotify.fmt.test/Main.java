package com.spotify.fmt.test;

public class Main {
  public static void main(String[] args) {
    var test = "a";
    var result =
        switch (test) {
          case "videos" -> null;
          case "images" -> null;
          default -> null;
        };
  }
}
