all: src/*.java
	javac -sourcepath src -d bin src/Main.java

clean:
	rm -rf bin/*
