#!/bin/bash
cd /Users/stevepodell/workspace/offhours
java -cp "creds:target/offhours-1.0-SNAPSHOT.jar:$(echo lib/*.jar | tr ' ' ':')" main.java.com.podell.OffHoursMonitor CommandLineStevesComputer2 classpath
