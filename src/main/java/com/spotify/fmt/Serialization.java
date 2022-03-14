package com.spotify.fmt;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class Serialization {

  static {
    // Best effort. Hope that ObjectOutputStream has not been loaded yet.
    System.setProperty("sun.io.serialization.extendedDebugInfo", "true");
  }

  private Serialization() {
    throw new UnsupportedOperationException();
  }

  static void serialize(Object object, Path file) throws SerializationException {
    try (final OutputStream os = Files.newOutputStream(file, WRITE, CREATE_NEW)) {
      serialize(object, os);
    } catch (IOException e) {
      throw new SerializationException("Serialization failed", e);
    }
  }

  static void serialize(Object object, OutputStream outputStream) throws SerializationException {
    try (ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
      oos.writeObject(object);
    } catch (Throwable t) {
      throw new SerializationException("Serialization failed", t);
    }
  }

  static <T> T deserialize(Path filePath) throws SerializationException {
    try {
      return deserialize(Files.newInputStream(filePath));
    } catch (IOException e) {
      throw new SerializationException("Deserialization failed", e);
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T deserialize(InputStream inputStream) throws SerializationException {
    try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
      return (T) ois.readObject();
    } catch (Throwable t) {
      throw new SerializationException("Deserialization failed", t);
    }
  }
}
