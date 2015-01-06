#!/bin/bash

java -cp "jars/*" Launcher.Main 2>&1 | tee logs.txt
