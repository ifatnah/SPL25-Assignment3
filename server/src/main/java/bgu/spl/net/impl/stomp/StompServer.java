package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.srv.Reactor; // <--- הוסף אימפורט אם חסר
import bgu.spl.net.srv.BlockingConnectionHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class StompServer {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: StompServer <port> <tpc|reactor>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String serverType = args[1].toLowerCase();

        ConnectionsImpl<String> connections = new ConnectionsImpl<>();

        if (serverType.equals("tpc")) {
            runThreadPerClient(port, connections);
        } else if (serverType.equals("reactor")) {
            int numThreads = Runtime.getRuntime().availableProcessors();

            try (Server<String> server = new Reactor<>(
                    numThreads,
                    port,
                    () -> new StompMessagingProtocolImpl(connections),
                    StompMessageEncoderDecoder::new,
                    connections)) {

                server.serve();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid server type: " + serverType);
        }
    }

    private static void runThreadPerClient(int port, ConnectionsImpl<String> connections) {
        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("TPC Server started on port " + port);

            int connectionIdCounter = 0;

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();

                StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl(connections);

                BlockingConnectionHandler<String> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        new StompMessageEncoderDecoder(),
                        protocol);

                int connectionId = connectionIdCounter++;
                protocol.start(connectionId, connections);
                connections.addConnection(connectionId, handler);

                new Thread(handler).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}