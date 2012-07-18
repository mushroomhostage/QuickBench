#!/bin/sh -x
#CLASSPATH=../craftbukkit-1.2.5-R1.0.jar javac *.java -Xlint:unchecked -Xlint:deprecation
CLASSPATH=../craftbukkit-1.2.5-R4.1-MCPC-SNAPSHOT-173.jar javac *.java -Xlint:unchecked -Xlint:deprecation
rm -rf com
mkdir -p com/exphc/QuickBench
mv *.class com/exphc/QuickBench
jar cf QuickBench.jar com/ *.yml *.java README.md ChangeLog LICENSE

