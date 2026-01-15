package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.stomp.StompFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    };

    @Override
    public void process(String message) {
        // Parse the message
        StompFrame frame = StompFrame.parse(message);

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
                // If command is not valid
                connections.send(connectionId, "ERROR\nmessage:Unknown Command\n\n\u0000");
                break;
        }

    };

    @Override
    public boolean shouldTerminate() {
        return true;
    };

    private void handleConnect(StompFrame frame) {
        // Get login and passcode
        String login = frame.getHeader("login");
        String passcode = frame.getHeader("passcode");
        // TODO: check login status within the sql connection and the database
    }

    private void handleSend(StompFrame frame) {
        // Get channel and body
        String destination = frame.getHeader("destination");
        String message = frame.getBody();

        // Send message to all users in the channel
        connections.send(destination, message);
        // TODO: check login status within the sql connection and the database
    }

    private void handleSubscribe(StompFrame frame) {
        // Get channel to subscribe to, id and receiptId
        String destination = frame.getHeader("destination");
        String id = frame.getHeader("id");
        String receipt = frame.getHeader("receipt");

        String message = "receipt -id :" + receipt;

        // Send receiptId
        connections.send(id, message);
    }

    private void handleUnsubscribe(StompFrame frame) {
        // Get id and receiptId
        String id = frame.getHeader("id");
        String receipt = frame.getHeader("receipt");

        String message = "receipt -id :" + receipt;

        // Send receiptId
        connections.send(id, message);
    }

    private void handleDisconnect(StompFrame frame) {
        // Get id and receiptId
        String id = frame.getHeader("id");
        String receipt = frame.getHeader("receipt");

        String message = "receipt -id :" + receipt;

        // Send receiptId
        connections.send(id, message);
    }
}
