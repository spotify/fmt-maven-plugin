[![Build Status](https://github.com/spotify/fmt-maven-plugin/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/spotify/fmt-maven-plugin/actions/workflows/ci.yml?query=branch%3Amain)
[![license](http://img.shields.io/badge/license-MIT-brightgreen.svg)](https://github.com/spotify/fmt-maven-plugin/blob/main/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.spotify.fmt/fmt-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify.fmt/fmt-maven-plugin)

## fmt-maven-plugin 

**UPDATE 2022-02-14:** This plugin has moved from [coveooss](https://github.com/coveooss/) to the [spotify](https://github.com/spotify/) Github org. The new groupId will be `com.spotify.fmt`, and the `master` branch has been renamed to `main`.

Formats your code using [google-java-format](https://github.com/google/google-java-format) which follows [Google's code styleguide](https://google.github.io/styleguide/javaguide.html).

The format cannot be configured by design.

If you want your IDE to stick to the same format, google-java-format also includes integrations for IntelliJ and Eclipse IDE's, following the installation instructions on the [README](https://github.com/google/google-java-format/blob/master/README.md#using-the-formatter).

## Usage

### Standard pom.xml

To have your sources automatically formatted on each build, add to your pom.xml:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.spotify.fmt</groupId>
                <artifactId>fmt-maven-plugin</artifactId>
                <version>VERSION</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>format</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

If you prefer, you can only check formatting at build time using the `check` goal:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.spotify.fmt</groupId>
                <artifactId>fmt-maven-plugin</artifactId>
                <version>VERSION</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

#### Overriding the Default Lifecycle Phase

Both goals have a [Maven lifecycle phase](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#lifecycle-reference) configured by default.

| Goal      | Default Phase     |
|-----------|-------------------|
| `format`  | `process-sources` |
| `check`   | `verify`          |

You may prefer to run these goals in a different phase instead.  
Maven allows you to override the default phase by specifying a `<phase>` for the `<execution>`.

For example, you may prefer that the `check` goal is performed in an earlier phase such as `validate`:

```xml
                    <execution>
                        <phase>validate</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
```

### Options

`sourceDirectory` represents the directory where your Java sources that need to be formatted are contained. It defaults to `${project.build.sourceDirectory}`

`testSourceDirectory` represents the directory where your test's Java sources that need to be formatted are contained. It defaults to `${project.build.testSourceDirectory}`

`additionalSourceDirectories` represents a list of additional directories that contains Java sources that need to be formatted. It defaults to an empty list.

`verbose` is whether the plugin should print a line for every file that is being formatted. It defaults to `false`.

`filesNamePattern` represents the pattern that filters files to format. The defaults value is set to `.*\.java`.

`skip` is whether the plugin should skip the operation.

`skipSortingImports` is whether the plugin should skip sorting imports.

`style` sets the formatter style to be `google` or `aosp`. By default this is `google`. Projects using Android conventions may prefer `aosp`.

`forkMode` lets you specify whether to run google-java-format in a fork or in-process. Also adds JVM arguments when needed. Value `default` will fork when JDK 16+ is detected, `never` runs in-process, regardless of JDK version and `always` will always fork.

example:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.spotify.fmt</groupId>
            <artifactId>fmt-maven-plugin</artifactId>
            <version>VERSION</version>
            <configuration>
                <sourceDirectory>some/source/directory</sourceDirectory>
                <testSourceDirectory>some/test/directory</testSourceDirectory>
                <verbose>true</verbose>
                <filesNamePattern>.*\.java</filesNamePattern>
                <additionalSourceDirectories>
                    <param>some/dir</param>
                    <param>some/other/dir</param>
                </additionalSourceDirectories>
                <skip>false</skip>
                <skipSortingImports>false</skipSortingImports>
                <style>google</style>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>format</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```



### check Options

`displayFiles` default = true. Display the list of the files that are not compliant

`displayLimit` default = 100. Number of files to display that are not compliant`

`style` sets the formatter style to be _google_ or _aosp_. By default this is 'google'. Projects using Android conventions may prefer `aosp`.

example to not display the non-compliant files:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.spotify.fmt</groupId>
            <artifactId>fmt-maven-plugin</artifactId>
            <version>VERSION</version>
            <configuration>
                <displayFiles>false</displayFiles>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

example to limit the display up to 10 files
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.spotify.fmt</groupId>
            <artifactId>fmt-maven-plugin</artifactId>
            <version>VERSION</version>
            <configuration>
                <displayLimit>10</displayLimit>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Command line

You can also use it on the command line

`mvn com.spotify.fmt:fmt-maven-plugin:format`

You can pass parameters via standard `-D` syntax.
`mvn com.spotify.fmt:fmt-maven-plugin:format -Dverbose=true`

`-Dfmt.skip` is whether the plugin should skip the operation.

### Using with Java 8

Starting from version 1.8, Google Java Formatter requires Java 11 to run. Incidently, all versions of this plugin starting from 2.10 inclusively also require this Java version to properly function. The 2.9.x release branch is the most up-to-date version that still runs on Java 8.

### Deploy

- Make a PR to bump the version in the POM with the change from `mvn versions:set -DnewVersion=x.y[.z]`
- Make a tag - `git tag x.y.z`
- Do the release - `mvn clean deploy -P release`
- Create a GitHub release with merged PRs and other information.
- Wait for the release to be available in [Maven Central](https://search.maven.org/search?q=com.spotify.fmt)
- Update version in use in the POM, and version in the README.