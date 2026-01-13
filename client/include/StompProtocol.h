#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/event.h"
#include "StompFrame.h"
#include <mutex>

// Structure that stores events by the client

struct GameEvent
{
    std::string team_a_name;
    std::string team_b_name;
    std::string event_name;
    int time;
    std::map<std::string, std::string> general_game_updates;
    std::map<std::string, std::string> team_a_updates;
    std::map<std::string, std::string> team_b_updates;
    std::string description;
};

class StompProtocol
{
private:
    int subscriptionIdCounter;
    int receiptIdCounter;
    std::map<std::string, int> topicToSubscriptionId;
    std::map<int, std::string> receipts;
    std::map<std::string, std::map<std::string, std::vector<GameEvent>>> gameUpdates;
    std::string currentUserName;
    std::mutex protocolMutex;

    /*
    Helper function for frames creation
    */
    StompFrame createConnectFrame(const std::string &login, const std::string &passcode);

    StompFrame createDisconnectFrame();

    StompFrame createSubscribeFrame(const std::string &gameName);

    StompFrame createUnsubscribeFrame(const std::string &gameName);

    StompFrame createSendFrame(const std::string &gameName, const std::string &messageBody);

    void writeSummaryToFile(const std::string &gameName, const std::string &userName, const std::string &fileName);

    std::vector<StompFrame> parseReportFromFile(const std::string &jsonFilePath);

    GameEvent parseEventBody(const std::string &body);

public:
    StompProtocol();

    std::vector<StompFrame> processKeyboardCommand(const std::string &line);

    bool processServerFrame(const StompFrame &frame);

};