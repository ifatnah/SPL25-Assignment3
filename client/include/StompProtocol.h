#pragma once

#include "../include/ConnectionHandler.h"
#include "StompFrame.h"

class StompProtocol
{
private:
    int subscriptionIdCounter;
    int receiptIdCounter;
    std::map<std::string, int> topicToSubscriptionId; 

    /*
    Helper function for frames creation
    */
    StompFrame createConnectFrame(const std::string& login, const std::string& passcode);
    StompFrame createDisconnectFrame();
    StompFrame createSubscribeFrame(const std::string& gameName);
    StompFrame createUnsubscribeFrame(const std::string& gameName);
    StompFrame createSendFrame(const std::string& gameName, const std::string& messageBody);


public:
    StompProtocol(); 
    
    StompFrame processKeyboardCommand(const std::string& line);

    bool processServerFrame(const StompFrame& frame);
};