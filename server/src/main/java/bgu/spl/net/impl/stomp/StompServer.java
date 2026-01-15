package bgu.spl.net.impl.stomp;

import java.util.Scanner;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {

        // Validity Check
        if (args.length < 2) {
            System.out.println("Usage: port server_type(tpc/reactor)");
            return;
        }

        // Save the port and server type
        int port = Integer.parseInt(args[0]);
        String serverType = args[1];

        if (serverType.equals("tpc")) {
            Server.threadPerClient(
                    port,
                    () -> new StompMessagingProtocolImpl(),
                    () -> new StompMessageEncoderDecoder()).serve();

        } else if (serverType.equals("reactor")) {
            // יצירת שרת Reactor
            Server.reactor(
                    Runtime.getRuntime().availableProcessors(), // מספר הת'רדים (כמספר הליבות במעבד)
                    port,
                    () -> new StompMessagingProtocolImpl(), // Protocol Factory
                    () -> new StompMessageEncoderDecoder() // Encoder Factory
            ).serve(); // הרצת השרת

        } else {
            System.out.println("Unknown server type. Please use 'tpc' or 'reactor'.");
        }
    }
}
