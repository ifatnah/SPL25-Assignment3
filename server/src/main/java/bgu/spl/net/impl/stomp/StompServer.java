package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {

        if (args.length < 2) {

            System.out.println("Usage: StompServer <port> <tpc|reactor>");

            return;

        }

        int port;

        try {

            port = Integer.parseInt(args[0]);

        } catch (NumberFormatException e) {

            System.out.println("Invalid port number: " + args[0]);

            return;

        }

        String serverType = args[1].toLowerCase();

        // Create the shared Connections object

        ConnectionsImpl<String> connections = new ConnectionsImpl<>();

        if (serverType.equals("tpc")) {

            // Thread-Per-Client server

            Server.threadPerClient(

                    port,

                    () -> new StompMessagingProtocolImpl(connections),

                    StompMessageEncoderDecoder::new

            ).serve();

        } else if (serverType.equals("reactor")) {

            // Reactor server

            int numThreads = Runtime.getRuntime().availableProcessors();

            Server.reactor(

                    numThreads,

                    port,

                    () -> new StompMessagingProtocolImpl(connections),

                    StompMessageEncoderDecoder::new

            ).serve();

        } else {

            System.out.println("Invalid server type: " + serverType);

            System.out.println("Use 'tpc' for Thread-Per-Client or 'reactor' for Reactor pattern");

        }

    }

}
