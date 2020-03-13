[![Build Status](https://travis-ci.org/coveo/fmt-maven-plugin.svg?branch=master)](https://travis-ci.org/coveo/fmt-maven-plugin)
[![license](http://img.shields.io/badge/license-MIT-brightgreen.svg)](https://github.com/coveo/fmt-maven-plugin/blob/master/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.coveo/fmt-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.coveo/fmt-maven-plugin)

## fmt-maven-plugin 

Formats your code using [google-java-format](https://github.com/google/google-java-format) which follows [Google's code styleguide](https://google.github.io/styleguide/javaguide.html).

The format cannot be configured by design.

If you want your IDE to stick to the same format, check out [the available configuration files](https://github.com/google/styleguide)

* [Eclipse](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml)
* [IntelliJ IDEA](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)

## Usage

### Standard pom.xml

To have your sources automatically formatted on each build, add to your pom.xml:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.coveo</groupId>
                <artifactId>fmt-maven-plugin</artifactId>
                <version>2.9</version>
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
                <groupId>com.coveo</groupId>
                <artifactId>fmt-maven-plugin</artifactId>
                <version>2.9</version>
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

If you prefer, you can only check formatting at build time during the maven `compile` phase using the `early-check` goal:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.coveo</groupId>
                <artifactId>fmt-maven-plugin</artifactId>
                <version>2.9</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>early-check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

### Options

`sourceDirectory` represents the directory where your Java sources that need to be formatted are contained. It defaults to `${project.build.sourceDirectory}`

`testSourceDirectory` represents the directory where your test's Java sources that need to be formatted are contained. It defaults to `${project.build.testSourceDirectory}`

`additionalSourceDirectories` represents a list of additional directories that contains Java sources that need to be formatted. It defaults to an empty list.

`verbose` is whether the plugin should print a line for every file that is being formatted. It defaults to `false`.

`filesNamePattern` represents the pattern that filters files to format. The defaults value is set to `.*\.java`.

`skip` is whether the plugin should skip the operation.

`skipSortingImports` is whether the plugin should skip sorting imports.

`style` sets the formatter style to be _google_ or _aosp_. By default this is 'google'. Projects using Android conventions may prefer `aosp`.

example:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.coveo</groupId>
            <artifactId>fmt-maven-plugin</artifactId>
            <version>2.9</version>
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
            <groupId>com.coveo</groupId>
            <artifactId>fmt-maven-plugin</artifactId>
            <version>2.9</version>
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
            <groupId>com.coveo</groupId>
            <artifactId>fmt-maven-plugin</artifactId>
            <version>2.9</version>
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

`mvn com.coveo:fmt-maven-plugin:format`

You can pass parameters via standard `-D` syntax.
`mvn com.coveo:fmt-maven-plugin:format -Dverbose=true`

`-Dfmt.skip` is whether the plugin should skip the operation.


### Deploy

```
git tag v0.0.0

mvn versions:set -DnewVersion=0.0.0
mvn clean deploy -P release
```
