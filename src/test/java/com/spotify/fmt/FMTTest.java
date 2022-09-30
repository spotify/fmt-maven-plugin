package com.spotify.fmt;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.AdditionalMatchers.not;

import java.io.File;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class FMTTest {
  private static String FORMAT = "format";
  private static String CHECK = "check";

  @Rule public MojoRule mojoRule = new MojoRule();

  @Test
  public void noSource() throws Exception {
    FMT fmt = loadMojo("nosource", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).isEmpty();
  }

  @Test
  public void skipSource() throws Exception {
    FMT fmt = loadMojo("skipsourcedirectory", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test
  public void withoutTestSources() throws Exception {
    FMT fmt = loadMojo("notestsource", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(2);
  }

  @Test
  public void skipTestSources() throws Exception {
    FMT fmt = loadMojo("skiptestsourcedirectory", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(2);
  }

  @Test
  public void withOnlyTestSources() throws Exception {
    FMT fmt = loadMojo("onlytestsources", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test
  public void withAllTypesOfSources() throws Exception {
    FMT fmt = loadMojo("simple", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(3);
  }

  @Test
  public void withAllTypesOfSourcesWithAospStyleSpecified() throws Exception {
    FMT fmt = loadMojo("simple_aosp", FORMAT);
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
    FMT fmt = loadMojo("simple_google", FORMAT);
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
    FMT fmt = loadMojo("failonerrorwithsources", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).isNotEmpty();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownFolderFailsWhenAFolderIsMissing() throws Exception {
    FMT fmt = loadMojo("failonerrormissingsources", FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void failOnUnknownStyle() throws Exception {
    FMT fmt = loadMojo("failonunknownstyle", FORMAT);
    fmt.execute();
  }

  @Test
  public void canAddAdditionalFolders() throws Exception {
    FMT fmt = loadMojo("additionalfolders", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(8);
  }

  @Test
  public void withOnlyAvajFiles() throws Exception {
    FMT fmt = loadMojo("onlyavajsources", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test
  public void forkByDefaultAfterJDK9() throws Exception {
    FMT fmt = loadMojo("simple", FORMAT);
    assertThat(fmt.shouldFork()).isTrue();
  }

  @Test
  public void forkAlways() throws Exception {
    FMT fmt = loadMojo("fork_always", FORMAT);
    assertThat(fmt.shouldFork()).isTrue();
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test
  public void forkNeverBeforeJDK16() throws Exception {
    assumeFalse(javaRuntimeStronglyEncapsulatesByDefault()); // Skip if forking is needed.
    FMT fmt = loadMojo("fork_never_beforejdk16", FORMAT);
    assertThat(fmt.shouldFork()).isFalse();
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test(
      expected =
          IllegalAccessError.class) // Could stop throwing this if google-java-format is fixed.
  public void forkNeverAfterJDK16() throws Exception {
    assumeTrue(javaRuntimeStronglyEncapsulatesByDefault()); // Skip if forking is not needed.
    FMT fmt = loadMojo("fork_never_afterjdk16", FORMAT);
    assertThat(fmt.shouldFork()).isFalse();
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test(expected = MojoFailureException.class)
  public void unsupportedForkMode() throws Exception {
    FMT fmt = loadMojo("unsupported_fork_mode", FORMAT);
    fmt.execute();

    assertThat(fmt.getResult().processedFiles()).hasSize(1);
  }

  @Test(expected = MojoFailureException.class)
  public void validateOnlyFailsWhenNotFormatted() throws Exception {
    Check check = loadMojo("validateonly_notformatted", CHECK);
    check.execute();
  }

  @Test
  public void validateOnlySucceedsWhenFormatted() throws Exception {
    Check check = loadMojo("validateonly_formatted", CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnusedImports() throws Exception {
    Check check = loadMojo("importunused", CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void withUnsortedImports() throws Exception {
    Check check = loadMojo("importunsorted", CHECK);
    check.execute();
  }

  @Test
  public void withCleanImports() throws Exception {
    FMT fmt = loadMojo("importclean", FORMAT);
    fmt.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsWhenNotFormatted() throws Exception {
    Check check = loadMojo("check_notformatted", CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenSkipSourceDirectory() throws Exception {
    Check check = loadMojo("check_skipsourcedirectory", CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenSkipTestSourceDirectory() throws Exception {
    Check check = loadMojo("check_skiptestsourcedirectory", CHECK);
    check.execute();
  }

  @Test
  public void checkSucceedsWhenFormatted() throws Exception {
    Check check = loadMojo("check_formatted", CHECK);

    check.execute();
  }

  @Test
  public void checkSucceedsWhenNotFormattedButIgnored() throws Exception {
    Check check = loadMojo("check_notformatted_ignored", CHECK);
    check.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsWhenFormattingFails() throws Exception {
    Check check = loadMojo("failed_formatting", CHECK);
    check.execute();
  }

  @Test
  public void checkWarnsWhenNotFormattedAndConfiguredWithFailOnErrorFalse() throws Exception {
    Check check = loadMojo("failonerrorfalse_notformatted", CHECK);
    Log logSpy = setupLogSpy(check);

    check.execute();

    Mockito.verify(logSpy).warn(Mockito.contains("Non complying file"));
  }

  @Test
  public void checkDoesNotWarnWhenFormattedAndConfiguredWithFailOnErrorFalse() throws Exception {
    Check check = loadMojo("failonerrorfalse_formatted", CHECK);
    Log logSpy = setupLogSpy(check);

    check.execute();

    Mockito.verify(logSpy).warn(not(Mockito.contains("Non complying file")));
  }

  @Test(expected = MojoFailureException.class)
  public void checkFailsAndLogsErrorWhenFormattingFailsAndConfiguredWithWarningOnlyFalse()
      throws Exception {
    Check check = loadMojo("failonerrortrue_notformatted", CHECK);
    Log logSpy = setupLogSpy(check);

    check.execute();

    Mockito.verify(logSpy).error(Mockito.contains("Non complying file"));
  }

  @SuppressWarnings("unchecked")
  private <T extends AbstractFMT> T loadMojo(String pomFilePath, String goal) throws Exception {
    File pomFile = loadPom(pomFilePath);
    T fmt = (T) mojoRule.lookupConfiguredMojo(pomFile, goal);
    // Required for forking to work in unit tests where ${plugin.artifactMap} is not populated.
    fmt.useDefaultClasspathWhenForking = true;
    return fmt;
  }

  private File loadPom(String folderName) {
    return new File("src/test/resources/", folderName);
  }

  private Log setupLogSpy(Mojo mojo) {
    Log spy = Mockito.spy(mojo.getLog());
    mojo.setLog(spy);
    return spy;
  }

  private static boolean javaRuntimeStronglyEncapsulatesByDefault() {
    return Runtime.version().compareTo(Runtime.Version.parse("16")) >= 0;
  }
}
