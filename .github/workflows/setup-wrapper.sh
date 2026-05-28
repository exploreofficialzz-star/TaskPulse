#!/bin/bash
# Download real gradle-wrapper.jar if placeholder exists (< 100KB)
JAR="gradle/wrapper/gradle-wrapper.jar"
if [ $(wc -c < "$JAR") -lt 10000 ]; then
  echo "Downloading gradle-wrapper.jar..."
  curl -sL "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar" \
    -o "$JAR" || true
fi
