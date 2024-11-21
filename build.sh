#!/bin/bash
# в первой строчке указываем путь к bash-интерпретатору

# build.sh - script to install JVM, check version, compile, and run the shell program.

MIN_JAVA_VERSION=17 # Указываем минимальную версию Java, которая необходима для выполнения программы.

# Function to compare Java versions
version_ge() { # Проверяем, подходит ли установленная версия Java.
    printf '%s\n%s' "$1" "$2" | sort -C -V
}

# Check if Java is installed
if ! command -v java &>/dev/null || ! command -v javac &>/dev/null; then
    echo "Java Development Kit (JDK) is not installed. Installing..."
    sudo apt update
    sudo apt install -y openjdk-17-jdk
else
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    JAVA_VERSION_NUMBER=$(echo "$JAVA_VERSION" | cut -d'.' -f1)

    echo "Found Java version $JAVA_VERSION"

    if ! version_ge "$JAVA_VERSION_NUMBER" "$MIN_JAVA_VERSION"; then
        echo "Java version is below $MIN_JAVA_VERSION. Installing Java $MIN_JAVA_VERSION..."
        sudo apt update
        sudo apt install -y openjdk-17-jdk
    else
        echo "Java version is sufficient."
    fi
fi

# Compile the program
echo "Compiling the Java shell..."
mkdir -p bin # Создаёт директорию bin (если её ещё нет), куда будет помещён скомпилированный .class файл.
javac -d bin src/Shell.java # Компилирует исходный файл Shell.java в директорию bin.

# Check if compilation succeeded
if [ $? -eq 0 ]; then
    echo "Compilation successful. Running the shell..."
    java -cp bin src.Shell
else
    echo "Compilation failed. Please check your code for errors."
fi
