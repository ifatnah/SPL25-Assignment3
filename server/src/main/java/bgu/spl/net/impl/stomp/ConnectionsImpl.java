package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionsImpl<T> implements Connections<T> {

    // connectionId -> ConnectionHandler
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> handlers = new ConcurrentHashMap<>();

    // Channel name -> list of users
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> channels = new ConcurrentHashMap<>();

    // connectionId -> map of topic,subscriptionId
    private final ConcurrentHashMap<Integer, Map<String, Integer>> clientSubscriptions = new ConcurrentHashMap<>();

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

    @SuppressWarnings("unchecked")
    @Override
    public void send(String channel, T msg) {
        // Sending all users of a channel a message
        CopyOnWriteArrayList<Integer> users = channels.get(channel);
        if (users != null) {
            for (Integer connectionId : users) {

                Integer subscriptionId = null;
                Map<String, Integer> userSubs = clientSubscriptions.get(connectionId);

                if (userSubs != null) {
                    subscriptionId = userSubs.get(channel);
                }

                if (subscriptionId != null) {
                    String msgString = (String) msg;

                    String personalizedMsg = addSubscriptionHeader(msgString, subscriptionId);

                    send(connectionId, (T) personalizedMsg);

                }
            }
        }
    }

    // Auxiliary Function
    private String addSubscriptionHeader(String originalMsg, int subscriptionId) {

        String[] lines = originalMsg.split("\n", 2);
        if (lines.length < 2)
            return originalMsg;

        return lines[0] + "\nsubscription:" + subscriptionId + "\n" + lines[1];
    }

    @Override
    public void disconnect(int connectionId) {
        // Disconnect and remove the connectionId
        handlers.remove(connectionId);

        for (Map.Entry<String, CopyOnWriteArrayList<Integer>> entry : channels.entrySet()) {
            entry.getValue().remove(Integer.valueOf(connectionId));
        }

        clientSubscriptions.remove(connectionId);

    }

    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        // Add connection to handlers
        if (handlers.get(connectionId) == null) {
            handlers.put(connectionId, handler);
        }
    }

    public void subscribe(String channel, int connectionId, int subscriptionId) {

        // If the user dosent already exists, add it to channels
        channels.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>())
                .addIfAbsent(connectionId);

        // If the user dosent already exists, add it to clientSubscriptions
        clientSubscriptions.computeIfAbsent(connectionId, k -> new ConcurrentHashMap<>())
                .put(channel, subscriptionId);
    }

    public void unsubscribe(String idStr, int connectionId) {
        int subscriptionId = Integer.parseInt(idStr);
        Map<String, Integer> userSubs = clientSubscriptions.get(connectionId);

        if (userSubs != null) {
            String channelToRemove = null;

            // Look for the channel
            for (Map.Entry<String, Integer> entry : userSubs.entrySet()) {
                if (entry.getValue() == subscriptionId) {
                    channelToRemove = entry.getKey();
                    break;
                }
            }

            // Delete it if exists
            if (channelToRemove != null) {
                userSubs.remove(channelToRemove);

                // delete from the map
                CopyOnWriteArrayList<Integer> subscribers = channels.get(channelToRemove);
                if (subscribers != null) {
                    subscribers.remove(Integer.valueOf(connectionId));
                }
            }
        }
    }

    public boolean isSubscribed(int connectionId, String channel) {
        Map<String, Integer> userSubs = clientSubscriptions.get(connectionId);
        if (userSubs == null) {
            return false;
        }
        return userSubs.containsKey(channel);
    }

}
