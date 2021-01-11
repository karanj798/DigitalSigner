#!/usr/bin/bash

if [ "$1" = "start" ] ; then
    # Run Redis through Docker
    docker-compose up --build --detach

    # Compile Code
    mvn clean compile && cd target/

    # Launch RMI Registry
    rmiregistry -J-Djava.class.path=classes &
    rmiregistry 45682 -J-Djava.class.path=classes &

    cd classes/

    # Run Server
    java -cp ".:../dependency/*" server.MasterNode &
    java -cp ".:../dependency/*" server.BackupNode &
    java -cp ".:../dependency/*" server.RedisBroker &

elif [ "$1" = "stop" ] ; then
    # Stop Redis
    docker-compose down

    # Stop RMI Registry
    ps -ef | grep 'rmiregistry' | grep -v grep | awk '{print $2}' | xargs -r kill -9

    # Stop Java Servers
    ps -ef | grep 'java -cp*' | grep -v grep | awk '{print $2}' | xargs -r kill -9

    # Remove Compiled Code
    mvn clean

else 
    echo "[Usage] ./service.sh [start/stop]"
fi