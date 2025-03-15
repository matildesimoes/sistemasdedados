#!/bin/bash

JAR_PATH="lib/gson-2.9.1.jar"

find main -name "*.java" > sources.txt
javac -cp ".:$JAR_PATH" @sources.txt
