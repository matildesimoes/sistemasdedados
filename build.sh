#!/bin/bash

JAR_PATH="lib/gson-2.12.1.jar"

mkdir -p out

find src/main -name "*.java" > sources.txt
javac -cp ".:$JAR_PATH" -d out @sources.txt
