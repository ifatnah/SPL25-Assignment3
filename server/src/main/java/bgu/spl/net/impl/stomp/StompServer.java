package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
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

        // יצירת אובייקט ה-Connections המשותף
        ConnectionsImpl<String> connections = new ConnectionsImpl<>();

        if (serverType.equals("tpc")) {
            runThreadPerClient(port, connections);
        } else if (serverType.equals("reactor")) {
            // אם תרצה לממש reactor בלי לגעת בבסיס, תצטרך לבצע שכפול דומה
            // כרגע נתמקד ב-TPC כדי שתוכל להתקדם
            int numThreads = Runtime.getRuntime().availableProcessors();
            Server.reactor(
                    numThreads,
                    port,
                    () -> new StompMessagingProtocolImpl(connections),
                    StompMessageEncoderDecoder::new).serve();
        } else {
            System.out.println("Invalid server type: " + serverType);
        }
    }

    /**
     * מימוש ידני של שרת Thread-Per-Client עבור STOMP
     * מאפשר שליטה מלאה על יצירת ה-Handler והרישום שלו ב-Connections
     */
    private static void runThreadPerClient(int port, ConnectionsImpl<String> connections) {
        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("TPC Server started on port " + port);

            int connectionIdCounter = 0;

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();

                // 1. יצירת הפרוטוקול
                StompMessagingProtocolImpl protocol = new StompMessagingProtocolImpl(connections);

                // 2. יצירת ה-Handler (משתמשים במחלקה הקיימת ב-srv, זה מותר כי היא public)
                BlockingConnectionHandler<String> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        new StompMessageEncoderDecoder(),
                        protocol);

                // 3. ביצוע הפעולות שדרשו שינוי במחלקות הבסיס - אבל עושים אותן כאן!
                int connectionId = connectionIdCounter++;

                // אתחול הפרוטוקול עם ה-ID
                protocol.start(connectionId, connections);

                // רישום ה-Handler ב-Connections כדי שנוכל לשלוח אליו הודעות
                connections.addConnection(connectionId, handler);

                // 4. הרצת ה-Handler בת'רד נפרד
                new Thread(handler).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}