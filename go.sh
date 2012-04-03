#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.4-R1.0.jar javac *.java -Xlint:unchecked -Xlint:deprecation
rm -rf me
mkdir -p me/exphc/QuickBench
mv *.class me/exphc/QuickBench
jar cf QuickBench.jar me/ *.yml *.java
