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
    FMT fmt = loadMojo(FMT.class, "nosource", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).isEmpty();
  }

  @Test
  public void withoutTestSources() throws Exception {
    FMT fmt = loadMojo(FMT.class, "notestsource", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(2);
  }

  @Test
  public void withOnlyTestSources() throws Exception {
    FMT fmt = loadMojo(FMT.class, "onlytestsources", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test
  public void withAllTypesOfSources() throws Exception {
    FMT fmt = loadMojo(FMT.class, "simple", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(3);
  }

  @Test
  public void withAllTypesOfSourcesWithAospStyleSpecified() throws Exception {
    FMT fmt = loadMojo(FMT.class, "simple_aosp", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(3);

    /* Let's make sure we formatted with AOSP using 4 spaces */
    List<String> lines =
        IOUtils.readLines(
            getClass().getResourceAsStream("/simple_aosp/src/main/java/HelloWorld1.java"));
    assertThat(lines.get(3)).startsWith("    public");
  }

  @Test
  public void withAllTypesOfSourcesWithGoogleStyleSpecified() throws Exception {
    FMT fmt = loadMojo(FMT.class, "simple_google", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(3);

    /* Let's make sure we formatted with Google using 2 spaces */
    List<String> lines =
        IOUtils.readLines(
            getClass().getResourceAsStream("/simple_google/src/main/java/HelloWorld1.java"));
    assertThat(lines.get(3)).startsWith("  public");
  }

  @Test
  public void failOnUnknownFolderDoesNotFailWhenEverythingIsThere() throws Exception {
    FMT fmt = loadMojo(FMT.class, "failonerrorwithsources", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).isNotEmpty();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownFolderFailsWhenAFolderIsMissing() throws Exception {
    FMT fmt = loadMojo(FMT.class, "failonerrormissingsources", FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownStyle() throws Exception {
    FMT fmt = loadMojo(FMT.class, "failonunknownstyle", FORMAT);
    fmt.execute();
  }

  @Test
  public void canAddAdditionalFolders() throws Exception {
    FMT fmt = loadMojo(FMT.class, "additionalfolders", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(8);
  }

  @Test
  public void withOnlyAvajFiles() throws Exception {
    FMT fmt = loadMojo(FMT.class, "onlyavajsources", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test(expected = MojoFailureException.class)
  public void validateOnlyFailsWhenNotFormatted() throws Exception {
    Check check = loadMojo(Check.class, "validateonly_notformatted", CHECK);
    check.execute();
  }

  @Test
  public void validateOnlySucceedsWhenFormatted() throws Exception {
    Check check = loadMojo(Check.class, "validateonly_formatted", CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnusedImports() throws Exception {
    Check check = loadMojo(Check.class, "importunused", CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnsortedImports() throws Exception {
    Check check = loadMojo(Check.class, "importunsorted", CHECK);
    check.execute();
  }

  @Test
  public void withCleanImports() throws Exception {
    FMT fmt = loadMojo(FMT.class, "importclean", FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsWhenNotFormatted() throws Exception {
    Check check = loadMojo(Check.class, "check_notformatted", CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenFormatted() throws Exception {
    Check check = loadMojo(Check.class, "check_formatted", CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenNotFormattedButIgnored() throws Exception {
    Check check = loadMojo(Check.class, "check_notformatted_ignored", CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsWhenFormattingFails() throws Exception {
    Check check = loadMojo(Check.class, "failed_formatting", CHECK);
    check.execute();
  }

  @SuppressWarnings("unchecked")
  private <T extends AbstractFMT> T loadMojo(Class<T> cls, String pomFilePath, String goal)
      throws Exception {
    File pomFile = loadPom(pomFilePath);
    T fmt = (T) mojoRule.lookupConfiguredMojo(pomFile, goal);
    // Required for forking to work in unit tests where ${plugin.artifactMap} is not populated.
    fmt.forkWithDefaultClasspath = true;
    return fmt;
  }

  private File loadPom(String folderName) {
    return new File("src/test/resources/", folderName);
  }
}
