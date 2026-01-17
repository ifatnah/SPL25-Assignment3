package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.LoginStatus;

import java.util.concurrent.atomic.AtomicInteger;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate = false;
    private boolean isLoggedIn = false;
    private static final AtomicInteger messageIdCounter = new AtomicInteger(0);

    public StompMessagingProtocolImpl(Connections<String> connections) {
        this.connections = connections;
    }

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public String process(String message) {
        StompFrame frame = StompFrame.parse(message);

        // If the frame is empty return null
        if (frame == null) {
            return null;
        }

        if (!isLoggedIn && !frame.getCommand().equals("CONNECT")) {
            connections.send(connectionId, "ERROR\nmessage:Not connected\n\nYou must Connect first\n");
            shouldTerminate = true;
            return null;
        }

        // Case Handeling
        switch (frame.getCommand()) {
            case "CONNECT":
                handleConnect(frame);
                return null;
            case "SEND":
                handleSend(frame);
                return null;
            case "SUBSCRIBE":
                handleSubscribe(frame);
                return null;
            case "UNSUBSCRIBE":
                handleUnsubscribe(frame);
                return null;
            case "DISCONNECT":
                handleDisconnect(frame);
                return null;
            default:
                connections.send(connectionId, "ERROR\nmessage:Unknown Command\n\n\u0000");
                return null;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private void handleConnect(StompFrame frame) {
        String login = frame.getHeader("login");
        String passcode = frame.getHeader("passcode");
        String acceptVersion = frame.getHeader("accept-version");

        // Making sure every headline is correct and exists
        if (login == null || passcode == null || acceptVersion == null) {
            connections.send(connectionId,
                    "ERROR\nmessage:Malformed Frame\n\nMissing login or passcode headers\n\u0000");
            shouldTerminate = true;
            return;
        }

        // Call the DB
        LoginStatus status = Database.getInstance().login(connectionId, login, passcode);

        if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY || status == LoginStatus.ADDED_NEW_USER) {
            // Succesful login
            isLoggedIn = true;
            String response = "CONNECTED\n" +
                    "version:1.2\n" +
                    "\n";
            connections.send(connectionId, response);
        } else {
            // Handle Login Errors
            String errorMsg = "Login failed";
            if (status == LoginStatus.WRONG_PASSWORD) {
                errorMsg = "Wrong password";
            } else if (status == LoginStatus.ALREADY_LOGGED_IN) {
                errorMsg = "User already logged in";
            } else if (status == LoginStatus.CLIENT_ALREADY_CONNECTED) {
                errorMsg = "Client already connected";
            }

            // Send an ERORR msg and dissconnect the user from the server
            connections.send(connectionId, "ERROR\nmessage:Login Failed\n\n" + errorMsg + "\n");
            shouldTerminate = true;
            connections.disconnect(connectionId);
        }
    }

    private void handleSend(StompFrame frame) {
        String destination = frame.getHeader("destination");
        String body = frame.getBody();

        if (destination != null) {
            // Create a Unique msgId
            int msgId = messageIdCounter.incrementAndGet();

            // Frame building
            String serverFrame = "MESSAGE\n" +
                    "destination:" + destination + "\n" +
                    "message-id:" + msgId + "\n" +
                    "\n" +
                    body;

            // send a msg to all users that subscribe to the channel
            connections.send(destination, serverFrame);
        }

        // Reciept handeling
        handleReceipt(frame);
    }

    private void handleSubscribe(StompFrame frame) {
        String destination = frame.getHeader("destination");
        String idStr = frame.getHeader("id");

        if (destination != null && idStr != null) {
            int subscriptionId = Integer.parseInt(idStr);

            // Connect to connectionsImpl
            ((ConnectionsImpl<String>) connections).subscribe(destination, connectionId, subscriptionId);

            // Reciept handeling
            handleReceipt(frame);
        }
    }

    private void handleUnsubscribe(StompFrame frame) {
        String idStr = frame.getHeader("id");
        if (idStr != null) {
            int subscriptionId = Integer.parseInt(idStr);

            ((ConnectionsImpl<String>) connections).unsubscribe(idStr, connectionId);

            handleReceipt(frame);
        }
    }

    private void handleDisconnect(StompFrame frame) {

        // Handle reciept
        handleReceipt(frame);
        // change the bool
        shouldTerminate = true;

        // Disconnect from connections
        connections.disconnect(connectionId);
    }

    // Auxillary function that helps with reciept
    private void handleReceipt(StompFrame frame) {
        String receiptId = frame.getHeader("receipt");
        if (receiptId != null) {
            String receiptFrame = "RECEIPT\n" +
                    "receipt-id:" + receiptId + "\n" +
                    "\n";
            connections.send(connectionId, receiptFrame);
        }
    }
}