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
