#!/bin/bash

# Configuration
JAR_PATH=".:libs/jsoup-1.21.2.jar:libs/kotlinx-serialization-core-jvm-1.9.0.jar:libs/kotlinx-serialization-json-jvm-1.9.0.jar:"
X_PLUGIN="libs/kotlin-serialization-compiler-plugin-2.3.0.jar"
PACKAGE="com.krawl"
MAIN_FILE="Main.kt"
CLASS_NAME="MainKt"

# 1. Compile
echo "COMPILING..."
kotlinc -cp "$JAR_PATH" -Xplugin="$X_PLUGIN" "$MAIN_FILE" -d .

# 2. Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "RUNNING..."
    # 3. Run (using : as separator for Unix)
    kotlin -cp "$JAR_PATH:." "$PACKAGE.$CLASS_NAME" "$@"
else
    echo "COMPILATION FAILED."
fi