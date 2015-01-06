# G5K-VMPlaceS

This repository contains the source of the G5K-VMPlaceS project. It corresponds to the experimental version (grid'5000) of the [VMPlaceS](htttps://github.com/BeyondTheClouds/VMPlaceS) project.

Please note that this README.md file only deals with the java code of the project. 
If you want to see how conducting the experiments on top of the Grid'5000 testbed, you must give a look to the README file present in [https://github.com/BeyondTheClouds/VMPlaceS/tree/master/g5k-deployment-scripts](https://github.com/BeyondTheClouds/VMPlaceS/tree/master/g5k-deployment-scripts) directory. In addition to a specific README file, this sub-directory contains all the files and executables such as the memtouch program which are mandatory for the experiment. 

## Requirements
* java
* maven
* python
* git (optional)

## Installation

Firstly, clone the project.

### 1-a Installation with the correct entropy jar:
If you have a jar generated from our fork of the entropy project located at [https://github.com/BeyondTheClouds/Entropy](https://github.com/BeyondTheClouds/Entropy),

just run the following command:

```
$ ./build.py
```
### 1-b Installation without the correct entropy jar:

To ask the building script to reclone and recompile entropy, add **-c** to the previous command:

```
$ ./build.py -c
```

### 2- Result:

It will result in the creation of an archive **output.tgz** that contains the following files:


```
drwxr-xr-x  5 jonathan  staff   170B Dec 15 22:01 jars
-rwxr-xr-x  1 jonathan  staff    44B Dec 15 22:01 launch.sh
```

where the **jars** folder contains:

```
-rw-r--r--  1 jonathan  staff    53K Dec 15 22:01 G5K-VMPlaceS-1.0-executable.jar
-rw-r--r--  1 jonathan  staff    16K Dec 15 22:01 VirtualizationDriver-0.0.1-SNAPSHOT.jar
-rw-r--r--  1 jonathan  staff    11M Dec 15 22:01 entropy-2.1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Run jar on grid'5000

extract the previously generated archive on a server, and run:

```
$ ./launch.sh
```

## Modify the project from IntelliJ

### Open the project with IntelliJ

Follow the steps that are described by:

[http://wiki.jetbrains.net/intellij/Creating_and_importing_Maven_projects](http://wiki.jetbrains.net/intellij/Creating_and_importing_Maven_projects)

It should run fine!

### /!\ In case there are a lot of mistakes:

Maybe the maven dependencies have not been correctly managed by IntelliJ: check the following:

1. Open the **"Maven projects"** tab in the right part of intelliJ.
2. Right click on **"entropy"** .
3. Click on **"Reimport"**.

![image](http://dropbox.jonathanpastor.fr/intellij_maven_reimport_steps.png)
