#!/bin/bash
find main -name "*.java" > sources.txt
javac @sources.txt
