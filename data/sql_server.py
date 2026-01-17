#!/usr/bin/env python3
"""
Basic Python Server for STOMP Assignment – Stage 3.3
Implemented by Azizi
"""

import socket
import sys
import threading
import sqlite3

SERVER_NAME = "STOMP_PYTHON_SQL_SERVER"  # DO NOT CHANGE!
DB_FILE = "stomp_server.db"              # DO NOT CHANGE!

def recv_null_terminated(sock: socket.socket) -> str:
    data = b""
    while True:
        chunk = sock.recv(1024)
        if not chunk:
            return ""
        data += chunk
        if b"\0" in data:
            msg, _ = data.split(b"\0", 1)
            return msg.decode("utf-8", errors="replace")

def init_database():
    """
    Initialize the database tables based on the pdf requirements
    """
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    
    # Table for registered users
    c.execute('''CREATE TABLE IF NOT EXISTS users
                 (username TEXT PRIMARY KEY, 
                  password TEXT, 
                  registration_date TEXT)''')

    # Table for login/logout history
    c.execute('''CREATE TABLE IF NOT EXISTS login_history
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT, 
                  login_time TEXT, 
                  logout_time TEXT,
                  FOREIGN KEY(username) REFERENCES users(username))''')

    # Table for file uploads tracking
    c.execute('''CREATE TABLE IF NOT EXISTS file_tracking
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT, 
                  filename TEXT, 
                  upload_time TEXT,
                  game_channel TEXT,
                  FOREIGN KEY(username) REFERENCES users(username))''')

    conn.commit()
    conn.close()
    print(f"[{SERVER_NAME}] Database initialized successfully.")

def process_sql_request(sql_msg: str) -> str:
    """
    Executes the SQL and returns a formatted string.
    Format expected by Java: "SUCCESS|row1_field1, row1_field2...|row2..."
    """
    sql_msg = sql_msg.strip()
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    response = ""

    try:
        # Check if it's a SELECT query (Read) or Command (Write)
        is_select = sql_msg.upper().startswith("SELECT")
        
        c.execute(sql_msg)
        
        if is_select:
            rows = c.fetchall()
            # Format: SUCCESS|col1, col2, col3|col1, col2, col3...
            # This matches the split("\\|") logic in Database.java
            formatted_rows = []
            for row in rows:
                # Convert all items to string and join with comma
                row_str = ", ".join([str(item) for item in row])
                formatted_rows.append(row_str)
            
            response = "SUCCESS|" + "|".join(formatted_rows)
        else:
            conn.commit()
            response = "SUCCESS"

    except sqlite3.Error as e:
        response = f"ERROR:{str(e)}"
    except Exception as e:
        response = f"ERROR:{str(e)}"
    finally:
        conn.close()
    
    return response

def handle_client(client_socket: socket.socket, addr):
    print(f"[{SERVER_NAME}] Client connected from {addr}")

    try:
        while True:
            message = recv_null_terminated(client_socket)
            if message == "":
                break

            print(f"[{SERVER_NAME}] Received SQL: {message}")
            
            # Execute the logic
            result = process_sql_request(message)
            
            # Send back response + Null Terminator
            response_bytes = (result + "\0").encode('utf-8')
            client_socket.sendall(response_bytes)

    except Exception as e:
        print(f"[{SERVER_NAME}] Error handling client {addr}: {e}")
    finally:
        try:
            client_socket.close()
        except Exception:
            pass
        print(f"[{SERVER_NAME}] Client {addr} disconnected")


def start_server(host="127.0.0.1", port=7778):
    # Initialize DB tables before starting
    init_database()

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((host, port))
        server_socket.listen(5)
        print(f"[{SERVER_NAME}] Server started on {host}:{port}")
        print(f"[{SERVER_NAME}] Waiting for connections...")

        while True:
            client_socket, addr = server_socket.accept()
            t = threading.Thread(
                target=handle_client,
                args=(client_socket, addr),
                daemon=True
            )
            t.start()

    except KeyboardInterrupt:
        print(f"\n[{SERVER_NAME}] Shutting down server...")
    finally:
        try:
            server_socket.close()
        except Exception:
            pass


if __name__ == "__main__":
    port = 7778
    if len(sys.argv) > 1:
        raw_port = sys.argv[1].strip()
        try:
            port = int(raw_port)
        except ValueError:
            print(f"Invalid port '{raw_port}', falling back to default {port}")

    start_server(port=port)