#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.3-R0.1.jar javac *.java -Xlint:unchecked -Xlint:deprecation
rm -rf com
mkdir -p com/exphc/QuickBench
mv *.class com/exphc/QuickBench
jar cf QuickBench.jar com/ *.yml *.java
