#!/bin/bash


# Execute with args
mvn exec:java -Dexec.mainClass="main.main" -Dexec.args="$*"

