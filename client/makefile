CFLAGS:=-c -Wall -Weffc++ -g -std=c++17 -Iinclude
LDFLAGS:=-lboost_system -lpthread

all: StompWCIClient

StompWCIClient: bin/ConnectionHandler.o bin/StompClient.o bin/StompFrame.o bin/event.o bin/StompProtocol.o
	g++ -o bin/StompWCIClient bin/ConnectionHandler.o bin/StompClient.o bin/StompFrame.o bin/event.o bin/StompProtocol.o $(LDFLAGS)

bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/StompClient.o: src/StompClient.cpp
	g++ $(CFLAGS) -o bin/StompClient.o src/StompClient.cpp

bin/StompFrame.o: src/StompFrame.cpp
	g++ $(CFLAGS) -o bin/StompFrame.o src/StompFrame.cpp

bin/event.o: src/event.cpp
	g++ $(CFLAGS) -o bin/event.o src/event.cpp

bin/StompProtocol.o: src/StompProtocol.cpp
	g++ $(CFLAGS) -o bin/StompProtocol.o src/StompProtocol.cpp

clean:
	rm -f bin/*