#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include <thread>
#include <mutex>
#include <atomic>

int main(int argc, char *argv[])
{
	// Building a new stomp protocol and the socket thread
	StompProtocol protocol;
	ConnectionHandler *connectionHandler = nullptr;
	std::thread *socketThread = nullptr;
	std::atomic<bool> isConnected(false);

	while (true)
	{
		// Creating the buff size and array
		const short bufsize = 1024;
		char buf[bufsize];

		// Reading user input and checking its validity
		if (!std::cin.getline(buf, bufsize))
		{
			break;
		}

		// Spliting the user input into lines
		std::string line(buf);
		std::stringstream ss(line);
		std::string command;
		ss >> command;

		if (command.empty())
		{
			continue;
		}

		// Handle login case
		if (command == "login")
		{
			if (isConnected)
			{
				std::cout << "The client is already logged in, log out before trying again" << std::endl;
				continue;
			}

			// Parse host:port
			std::string hostPort;
			ss >> hostPort;

			// Find the :
			size_t colonPos = hostPort.find(':');

			// Validity check
			if (colonPos == std::string::npos)
			{
				std::cerr << "Error: Invalid host:port format" << std::endl;
				continue;
			}

			// Extract the host
			std::string host = hostPort.substr(0, colonPos);

			// Extract the port
			short port = (short)std::stoi(hostPort.substr(colonPos + 1));

			// Create Handler and Connect
			connectionHandler = new ConnectionHandler(host, port);

			// If not succeeded, delete the connection handler and return to wait for another user input
			if (!connectionHandler->connect())
			{
				std::cerr << "Could not connect to server" << std::endl;
				delete connectionHandler;
				connectionHandler = nullptr;
				continue;
			}

			isConnected = true;

			// Defining the socket thread
			socketThread = new std::thread([&connectionHandler, &protocol, &isConnected]()
										   {
                while (isConnected) {
                    std::string packet;
                    // Blocking message from server
                    if (!connectionHandler->getFrameAscii(packet, '\0')) {
                        std::cout << "Disconnected from server" << std::endl;
                        isConnected = false;
                        break;
                    }

                    // Building a packet for the frame
                    StompFrame frame(packet, true);
                    bool shouldContinue = protocol.processServerFrame(frame);

                    // If the protocol said to stop, stop the process
                    if (!shouldContinue) {
                        isConnected = false;
                        connectionHandler->close();
                        break;
                    }
                } });
		}

		// Dealing with other cases that arent login
		if (!isConnected && command != "login")
		{
			std::cout << "Not connected. Please login first." << std::endl;
			continue;
		}

		// Create frames to send
		std::vector<StompFrame> framesToSend = protocol.processKeyboardCommand(line);

		// Send frames to the server
		for (const auto &frame : framesToSend)
		{
			if (connectionHandler && isConnected)
			{
				if (!connectionHandler->sendFrameAscii(frame.toString(), '\0'))
				{
					std::cout << "Error sending frame" << std::endl;
					isConnected = false;
					break;
				}
			}
		}

		// Logout
		if (command == "logout" && socketThread)
		{

			socketThread->join();

			// Data Cleaning
			delete socketThread;
			socketThread = nullptr;
			delete connectionHandler;
			connectionHandler = nullptr;
			isConnected = false;
		}
	}

	// Cleaning
	if (socketThread)
	{
		if (socketThread->joinable())
			socketThread->join();
		delete socketThread;
	}
	if (connectionHandler)
	{
		delete connectionHandler;
	}

	return 0;
}