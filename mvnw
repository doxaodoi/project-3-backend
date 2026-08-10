#!/bin/sh
# Maven Wrapper startup script (Unix/Linux/macOS)
set -e

WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    if command -v curl > /dev/null 2>&1; then
        curl -sL -o "$WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "Error: curl or wget required to download Maven Wrapper" >&2
        exit 1
    fi
fi

exec java \
  -Dmaven.multiModuleProjectDirectory="$(dirname "$0")" \
  -jar "$WRAPPER_JAR" "$@"
