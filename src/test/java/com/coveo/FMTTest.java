package com.coveo;

import static com.google.common.truth.Truth.*;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class FMTTest {
  private static String FORMAT = "format";
  private static String CHECK = "check";

  @Rule public MojoRule mojoRule = new MojoRule();

  @Test
  public void noSource() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("nosource"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).isEmpty();
  }

  @Test
  public void withoutTestSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("notestsource"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).hasSize(2);
  }

  @Test
  public void withOnlyTestSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("onlytestsources"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).hasSize(1);
  }

  @Test
  public void withAllTypesOfSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("simple"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).hasSize(3);
  }

  @Test
  public void failOnUnknownFolderDoesNotFailWhenEverythingIsThere() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("failonerrorwithsources"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).isNotEmpty();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownFolderFailsWhenAFolderIsMissing() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("failonerrormissingsources"), FORMAT);
    fmt.execute();
  }

  @Test
  public void canAddAdditionalFolders() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("additionalfolders"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).hasSize(8);
  }

  @Test
  public void withOnlyAvajFiles() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("onlyavajsources"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesFormatted()).hasSize(1);
  }

  @Test(expected = MojoFailureException.class)
  public void validateOnlyFailsWhenNotFormatted() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("validateonly_notformatted"), FORMAT);
    fmt.execute();
  }

  @Test
  public void validateOnlySucceedsWhenFormatted() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("validateonly_formatted"), FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnusedImports() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("importunused"), FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnsortedImports() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("importunsorted"), FORMAT);
    fmt.execute();
  }

  @Test
  public void withCleanImports() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("importclean"), FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsWhenNotFormatted() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("check_notformatted"), CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenFormatted() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("check_formatted"), CHECK);
    check.execute();
  }

  public File loadPom(String folderName) {
    return new File("src/test/resources/", folderName);
  }
}
