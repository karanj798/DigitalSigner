#!/usr/bin/bash

# Stop Redis
docker-compose down

# Stop RMI Registry
ps -ef | grep 'rmiregistry' | grep -v grep | awk '{print $2}' | xargs -r kill -9

# Stop Java Servers
ps -ef | grep 'java -cp*' | grep -v grep | awk '{print $2}' | xargs -r kill -9

mvn clean