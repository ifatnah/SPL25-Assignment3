# SPL Assignment 3: World Cup 2026 Informer (STOMP Protocol)

## Project Overview
This project is the final assignment in the Systems Programming (SPL) course at Ben-Gurion University of the Negev. It implements a "community-led" World Cup update subscription service using the **STOMP** (Simple Text Oriented Messaging Protocol). 

The system features a multi-threaded server implemented in **Java** and a high-performance client implemented in **C++**. Users can subscribe to specific game channels, report real-time match events, and receive live broadcasts from other participants.

**Developers:** Tal Azizi & Ifat Nahmani

## Technical Features
- **Full STOMP Implementation:** Supports standard frames including `CONNECT`, `SUBSCRIBE`, `UNSUBSCRIBE`, `SEND`, and `DISCONNECT`.
- **Dual-Architecture Java Server:** Supports both **Thread-Per-Client (TPC)** for simple blocking I/O and the **Reactor** model for efficient non-blocking I/O.
- **Concurrent C++ Client:** A multi-threaded client designed to handle simultaneous user inputs and server messages without blocking the main execution flow.
- **Event-Driven Reporting:** Integration of JSON-based event reporting, allowing users to broadcast detailed match updates (goals, cards, etc.) to the community.

## System Architecture

### Server (Java)
The server manages client connections, subscriptions, and message broadcasting across different game channels. It is designed to be highly scalable, allowing the selection of the server model (TPC or Reactor) at startup.

### Client (C++)
The client handles the STOMP protocol logic, converting user commands into valid STOMP frames and processing incoming messages from the server. It includes a dedicated frame-to-object parser and connection handler.

## Project Structure
```text
├── server/                 # Java implementation
│   ├── src/main/java/      # Logic for StompMessagingProtocol and Connections
│   └── pom.xml             # Maven configuration
├── client/                 # C++ implementation
│   ├── include/            # ConnectionHandler, StompProtocol, and Event headers
│   ├── src/                # Core client logic
│   └── makefile            # Build script
└── data/                   # Sample JSON event files
