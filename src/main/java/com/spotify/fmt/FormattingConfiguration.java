package com.spotify.fmt;

import io.norberg.automatter.AutoMatter;
import java.io.File;
import java.io.Serializable;
import java.util.List;

@AutoMatter
interface FormattingConfiguration extends Serializable {

  String style();

  List<File> directoriesToFormat();

  boolean verbose();

  String filesNamePattern();

  String filesPathPattern();

  boolean skipSortingImports();

  boolean writeReformattedFiles();

  String processingLabel();

  static FormattingConfigurationBuilder builder() {
    return new FormattingConfigurationBuilder();
  }
}
