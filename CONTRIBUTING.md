# How to Contribute

We'd love to get patches from you!

## Getting Started

## Building the Project

`mvn clean verify`

## Workflow

We follow the [GitHub Flow Workflow](https://guides.github.com/introduction/flow/)

1.  Fork the project 
1.  Check out the `main` branch 
1.  Create a feature branch
1.  Write code and tests for your change 
1.  From your branch, make a pull request against `https://ghe.spotify.net/spotify/fmt-maven-plugin/main` 
1.  Work with repo maintainers to get your change reviewed 
1.  Wait for your change to be pulled into `https://ghe.spotify.net/spotify/fmt-maven-plugin/main`
1.  Delete your feature branch

## Testing

Change the version with `mvn versions:set -DnewVersion=x.y.z-SNAPSHOT`, do a `mvn install` and then use `x.y.z-SNAPSHOT` as the new plugin version in your other Maven project (or this project).

## Style

Quite obviously this project uses the `google-java-format`

## Issues

When creating an issue please try to adhere to the following format:

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior.

**Expected behavior**
A clear and concise description of what you expected to happen.

**Additional context**
Add any other context about the problem here.


## Pull Requests

Before opening a pull request, make sure `mvn verify`  runs successfully, this will also format your code with this plugin.

If there is already a GitHub issue for the task you are working on, leave a comment to let people know that you are working on it. If there isn't already an issue and it is a non-trivial task, it's a good idea to create one (and note that you're working on it). This prevents contributors from duplicating effort.

## Code Review

Branch protection is set up to require one approving review from maintainers of this repo. 

## Documentation

See [README](README.md)

We also welcome improvements to the project documentation or to the existing
docs. Please file an [issue](https://$REPOURL/issues/New).

# License 

By contributing your code, you agree to license your contribution under the 
terms of the [LICENSE](https://$LINKTOLICENSEFILE)

# Code of Conduct

Read our [Code of Conduct](CODE_OF_CONDUCT.md) for the project.
