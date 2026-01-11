#include "StompProtocol.h"
#include "StompFrame.h"

// Constructor
StompProtocol::StompProtocol() : 
    subscriptionIdCounter(0),
    receiptIdCounter(0),
    topicToSubscriptionId(),
    receipts()
{
}

/*
Helper function for frames creation
*/
// Creates CONNECT frame for login
StompFrame StompProtocol::createConnectFrame(const std::string &login, const std::string &passcode)    {
        StompFrame loginFrame = StompFrame("CONNECT");
        loginFrame.addHeader("accept-version", "1.2");
        loginFrame.addHeader("host", "stomp.cs.bgu.ac.il");
        loginFrame.addHeader("login", login);
        loginFrame.addHeader("passcode", passcode);

        return loginFrame;
    }

// Creates DISCONNECT frame with receipt
StompFrame StompProtocol::createDisconnectFrame()
    {
        int receiptID = receiptIdCounter++;
        StompFrame disconnectionFrame = StompFrame("DISCONNECT");
        disconnectionFrame.addHeader("receipt", std::to_string(receiptID));

        receipts[receiptID] = "DISCONNECT";

        return disconnectionFrame;
    }

// Creates SUBSCRIBE frame for joining a game channel
StompFrame StompProtocol::createSubscribeFrame(const std::string& gameName){
        // Increase subscriptionId and receiptID
        int subscriptionId = subscriptionIdCounter++;
        int receiptID = receiptIdCounter++;

        StompFrame subscriptionFrame = StompFrame("SUBSCRIBE");
        subscriptionFrame.addHeader("destination", "/" + gameName);
        subscriptionFrame.addHeader("id", std::to_string(subscriptionId));
        subscriptionFrame.addHeader("receipt", std::to_string(receiptID));

        // add subscriptionId to map of topicToSubscriptionId
        topicToSubscriptionId[gameName] = subscriptionId;

        return subscriptionFrame;
    }

// Creates UNSUBSCRIBE frame for leaving a game channel
StompFrame StompProtocol::createUnsubscribeFrame(const std::string& gameName){
        // Increase subscriptionId and receiptID
        int subscriptionId = topicToSubscriptionId[gameName];
        int receiptID = receiptIdCounter++;

        StompFrame unsubscriptionFrame = StompFrame("UNSUBSCRIBE");
        unsubscriptionFrame.addHeader("id", std::to_string(subscriptionId));
        unsubscriptionFrame.addHeader("receipt", std::to_string(receiptID));

        // erase gameName out of map of topicToSubscriptionId
        topicToSubscriptionId.erase(gameName);

        return unsubscriptionFrame;
    }

// Creates SEND frame for sending message to game channel
StompFrame StompProtocol::createSendFrame(const std::string& gameName, const std::string& messageBody){
    StompFrame sendFrame = StompFrame("SEND");
    sendFrame.addHeader("destination", "/" + gameName);

    sendFrame.setBody(messageBody);

    return sendFrame;
}
