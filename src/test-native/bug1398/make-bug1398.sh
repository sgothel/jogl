#! /bin/bash

set -x

gcc -x objective-c -framework Cocoa -o Bug1398macOSContextOpsOnMainThread Bug1398macOSContextOpsOnMainThread.c \
	&& javac -source 1.8 -target 1.8 -classpath ../../../gluegen/build/gluegen-rt.jar:../../build/jar/jogl-all.jar Bug1398macOSContextOpsOnMainThread.java
