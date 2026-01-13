package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionsImpl<T> implements Connections<T> {

    // User -> ConnectionHandler
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> handlers = new ConcurrentHashMap<>();

    // Channel name -> list of users
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> channels = new ConcurrentHashMap<>();

    @Override
    public boolean send(int connectionId, T msg) {
        // Sending a message to a specific user
        ConnectionHandler<T> handler = handlers.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void send(String channel, T msg) {
        // Sending all users of a channel a message 
        CopyOnWriteArrayList<Integer> users = channels.get(channel);
        if (users != null) {
            for (Integer user : users) {
                send(user, msg);
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        // Disconnect and remove the connectionId
        handlers.remove(connectionId);

        for (Map.Entry<String, CopyOnWriteArrayList<Integer>> entry : channels.entrySet()) {
            entry.getValue().remove(Integer.valueOf(connectionId));
        }

    }

    public void addConnection(int connectionId, ConnectionHandler<T> handler){
        // Add connection to handlers
        if (handlers.get(connectionId) == null){
            handlers.put(connectionId, handler);
        }
    }
}
