package com.spotify.fmt;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.List;
import org.apache.commons.io.IOUtils;
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

    assertThat(fmt.getFilesProcessed()).isEmpty();
  }

  @Test
  public void skipSource() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("skipsourcedirectory"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(1);
  }

  @Test
  public void withoutTestSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("notestsource"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(2);
  }

  @Test
  public void skipTestSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("skiptestsourcedirectory"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(2);
  }

  @Test
  public void withOnlyTestSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("onlytestsources"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(1);
  }

  @Test
  public void withAllTypesOfSources() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("simple"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(3);
  }

  @Test
  public void withAllTypesOfSourcesWithAospStyleSpecified() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("simple_aosp"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(3);

    /* Let's make sure we formatted with AOSP using 4 spaces */
    List<String> lines =
        IOUtils.readLines(
            getClass().getResourceAsStream("/simple_aosp/src/main/java/HelloWorld1.java"));
    assertThat(lines.get(3)).startsWith("    public");
  }

  @Test
  public void withAllTypesOfSourcesWithGoogleStyleSpecified() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("simple_google"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(3);

    /* Let's make sure we formatted with Google using 2 spaces */
    List<String> lines =
        IOUtils.readLines(
            getClass().getResourceAsStream("/simple_google/src/main/java/HelloWorld1.java"));
    assertThat(lines.get(3)).startsWith("  public");
  }

  @Test
  public void failOnUnknownFolderDoesNotFailWhenEverythingIsThere() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("failonerrorwithsources"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).isNotEmpty();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownFolderFailsWhenAFolderIsMissing() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("failonerrormissingsources"), FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownStyle() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("failonunknownstyle"), FORMAT);
    fmt.execute();
  }

  @Test
  public void canAddAdditionalFolders() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("additionalfolders"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(8);
  }

  @Test
  public void withOnlyAvajFiles() throws Exception {
    FMT fmt = (FMT) mojoRule.lookupConfiguredMojo(loadPom("onlyavajsources"), FORMAT);
    fmt.execute();

    assertThat(fmt.getFilesProcessed()).hasSize(1);
  }

  @Test(expected = MojoFailureException.class)
  public void validateOnlyFailsWhenNotFormatted() throws Exception {
    Check check =
        (Check) mojoRule.lookupConfiguredMojo(loadPom("validateonly_notformatted"), CHECK);
    check.execute();
  }

  @Test
  public void validateOnlySucceedsWhenFormatted() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("validateonly_formatted"), CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnusedImports() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("importunused"), CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnsortedImports() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("importunsorted"), CHECK);
    check.execute();
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
  public void checkSucceedsWhenSkipSourceDirectory() throws Exception {
    Check check =
        (Check) mojoRule.lookupConfiguredMojo(loadPom("check_skipsourcedirectory"), CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenSkipTestSourceDirectory() throws Exception {
    Check check =
        (Check) mojoRule.lookupConfiguredMojo(loadPom("check_skiptestsourcedirectory"), CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenFormatted() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("check_formatted"), CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenNotFormattedButIgnored() throws Exception {
    Check check =
        (Check) mojoRule.lookupConfiguredMojo(loadPom("check_notformatted_ignored"), CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsWhenFormattingFails() throws Exception {
    Check check = (Check) mojoRule.lookupConfiguredMojo(loadPom("failed_formatting"), CHECK);
    check.execute();
  }

  private File loadPom(String folderName) {
    return new File("src/test/resources/", folderName);
  }
}
