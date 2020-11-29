# Distributed Systems Project
## Installation
```shell script
git clone https://github.com/karanj798/DistributedSystemsProject.git

# Run Redis through Docker
docker-compose up --build --detach

# Compile Code
javac src/common/*.java -d out/
javac -cp ".;out/" src/client/*.java -d out/
javac -cp ".;out/;lib/jeromq-0.5.2.jar" src/server/*.java -d out/ 

# Run Server
java -cp ".;out/" -Djava.security.policy=src/server/policy.txt server.MasterNode
java -cp ".;out/" -Djava.security.policy=src/server/policy.txt server.BackupNode

# Run Client
java -cp ".;out/" client.Client
```