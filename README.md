# Distributed Systems Project

## Requirements 
- Docker
- JDK v11+

## Configuration
```shell script
# Run Redis through Docker
docker-compose up --build --detach

# Compile Code
javac src/common/model/*.java -d out/
javac -cp .;out/ src/common/*.java -d out/
javac -cp .;out/ src/client/*.java -d out/
javac -cp .;out/;lib/* src/server/*.java -d out/ 

# Launch RMI Registry
rmiregistry -J-Djava.class.path=out
rmiregistry 45682 -J-Djava.class.path=out

# Run Server
java -cp .;out/;lib/* -Djava.security.policy=src/server/policy.txt server.MasterNode
java -cp .;out/;lib/* -Djava.security.policy=src/server/policy.txt server.BackupNode
java -cp .;out/;lib/* server.RedisBroker

# Run Client
java -cp ".;out/" client.Client
```