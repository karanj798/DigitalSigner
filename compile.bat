javac -d out/ src/common/*.java
javac -d out/ -cp ".;out/" src/client/*.java
javac -d out/ -cp ".;out/;lib/jeromq-0.5.2.jar" src/server/*.java