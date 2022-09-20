package com.spotify.fmt;

import io.norberg.automatter.AutoMatter;
import java.io.Serializable;
import java.util.List;

@AutoMatter
interface FormattingResult extends Serializable {

  List<String> processedFiles();

  List<String> nonComplyingFiles();

  static FormattingResultBuilder builder() {
    return new FormattingResultBuilder();
  }
}
