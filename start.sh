#!/usr/bin/bash

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