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
    private static final AtomicInteger messageIdCounter = new AtomicInteger(0);

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(String message) {
        StompFrame frame = StompFrame.parse(message);

        // If the frame is empty return null
        if (frame == null) {
            return;
        }

        // Case Handeling
        switch (frame.getCommand()) {
            case "CONNECT":
                handleConnect(frame);
                break;
            case "SEND":
                handleSend(frame);
                break;
            case "SUBSCRIBE":
                handleSubscribe(frame);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(frame);
                break;
            case "DISCONNECT":
                handleDisconnect(frame);
                break;
            default:
                connections.send(connectionId, "ERROR\nmessage:Unknown Command\n\n\u0000");
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private void handleConnect(StompFrame frame) {
        // TODO - THIS IS NOT PERMANENT
        String response = "CONNECTED\n" +
                "version:1.2\n" +
                "\n" +
                "\u0000";

        connections.send(connectionId, response);
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
                    body + "\n" +
                    "\u0000";

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
                    "\n" +
                    "\u0000";
            connections.send(connectionId, receiptFrame);
        }
    }
}