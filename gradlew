#!/bin/sh

#
# Smart Teacher - Gradle launcher for Unix
#
# Note: the gradle-wrapper.jar binary is not committed to this repository.
# To generate it (and the rest of the wrapper) run once with a local Gradle 8.2 install:
#   gradle wrapper --gradle-version 8.2
# Or simply open the project in Android Studio, which will generate it automatically.
#

DIR="$(cd "$(dirname "$0")" && pwd)"
APP_ARGS=""

# Use gradle if available, otherwise instruct the user
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Gradle wrapper jar not found and 'gradle' is not on the PATH."
    echo "Install Gradle 8.2 (https://gradle.org/install/) and run:"
    echo "  gradle wrapper --gradle-version 8.2"
    echo "or open this project in Android Studio."
    exit 1
fi
