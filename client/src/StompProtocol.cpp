#include "StompProtocol.h"
#include "StompFrame.h"
#include "../include/event.h"
#include <sstream>
#include <fstream>
#include <algorithm>

// Constructor
StompProtocol::StompProtocol() : subscriptionIdCounter(0),
                                 receiptIdCounter(0),
                                 topicToSubscriptionId(),
                                 receipts(),
                                 gameUpdates(),
                                 currentUserName(""),
                                 protocolMutex()
{
}

/*
Helper function for frames creation
*/

// Creates CONNECT frame for login
StompFrame StompProtocol::createConnectFrame(const std::string &login, const std::string &passcode)
{
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

    receipts[receiptID] = "DISCONNECTED";

    return disconnectionFrame;
}

// Creates SUBSCRIBE frame for joining a game channel
StompFrame StompProtocol::createSubscribeFrame(const std::string &gameName)
{
    // Increase subscriptionId and receiptID
    int subscriptionId = subscriptionIdCounter++;
    int receiptID = receiptIdCounter++;

    StompFrame subscriptionFrame = StompFrame("SUBSCRIBE");
    subscriptionFrame.addHeader("destination", "/" + gameName);
    subscriptionFrame.addHeader("id", std::to_string(subscriptionId));
    subscriptionFrame.addHeader("receipt", std::to_string(receiptID));

    // add subscriptionId to map of topicToSubscriptionId
    topicToSubscriptionId[gameName] = subscriptionId;

    receipts[receiptID] = "Joined channel - " + gameName;

    return subscriptionFrame;
}

// Creates UNSUBSCRIBE frame for leaving a game channel
StompFrame StompProtocol::createUnsubscribeFrame(const std::string &gameName)
{
    if (topicToSubscriptionId.find(gameName) == topicToSubscriptionId.end())
    {
        return StompFrame();
    }
    // Increase subscriptionId and receiptID
    int subscriptionId = topicToSubscriptionId[gameName];
    int receiptID = receiptIdCounter++;

    StompFrame unsubscriptionFrame = StompFrame("UNSUBSCRIBE");
    unsubscriptionFrame.addHeader("id", std::to_string(subscriptionId));
    unsubscriptionFrame.addHeader("receipt", std::to_string(receiptID));

    // erase gameName out of map of topicToSubscriptionId
    topicToSubscriptionId.erase(gameName);

    receipts[receiptID] = "Exited channel - " + gameName;

    return unsubscriptionFrame;
}

// Creates SEND frame for sending message to game channel
StompFrame StompProtocol::createSendFrame(const std::string &gameName, const std::string &messageBody)
{
    StompFrame sendFrame = StompFrame("SEND");
    sendFrame.addHeader("destination", "/" + gameName);

    sendFrame.setBody(messageBody);

    return sendFrame;
}

std::vector<StompFrame> StompProtocol::processKeyboardCommand(const std::string &line)
{
    // Creating a lock to prevent thread problems
    std::lock_guard<std::mutex> lock(protocolMutex);

    // Deconstructing the input from the user to seperate words
    std::stringstream ss(line);
    std::string word;
    std::vector<std::string> args;
    std::vector<StompFrame> frames;

    while (ss >> word)
    {
        args.push_back(word);
    }

    // First, check if args is empty
    if (args.empty())
    {
        frames.push_back(StompFrame());
    }
    std::string command = args[0];

    // Set Format According to the Command

    // Login command
    if (command == "login")
    {
        // Validity check
        if (args.size() < 4)
        {
            frames.push_back(StompFrame());
        }
        else
        {
            currentUserName = args[2];
            frames.push_back(createConnectFrame(currentUserName, args[3]));
        }
    }

    // Join command
    else if (command == "join")
    {
        // Validity check
        if (args.size() < 2)
        {
            frames.push_back(StompFrame());
        }
        frames.push_back(createSubscribeFrame(args[1]));
    }

    // Exit command
    else if (command == "exit")
    {
        // Validity check
        if (args.size() < 2)
        {
            frames.push_back(StompFrame());
        }
        frames.push_back(createUnsubscribeFrame(args[1]));
    }

    // Logout command
    else if (command == "logout")
    {
        frames.push_back(createDisconnectFrame());
    }

    // Report command
    else if (command == "report")
    {
        if (args.size() >= 2)
        {
            std::string jsonPath = args[1];
            std::vector<StompFrame> reportFrames = parseReportFromFile(jsonPath);
            frames.insert(frames.end(), reportFrames.begin(), reportFrames.end());
        }
    }

    // Summary command
    else if (command == "summary")
    {
        if (args.size() < 4)
        { // gameName, userName, fileName
            std::cerr << "Error: summary command requires 3 arguments" << std::endl;
            frames.push_back(StompFrame());
        }
        writeSummaryToFile(args[1], args[2], args[3]);
        frames.push_back(StompFrame());
    }

    return frames;
}

bool StompProtocol::processServerFrame(const StompFrame &frame)
{
    // Creating a lock to prevent thread problems
    std::lock_guard<std::mutex> lock(protocolMutex);

    // Check the command
    const std::string &command = frame.getCommand();

    // Assign according to command case

    // Connecected case
    if (command == "CONNECTED")
    {
        std::cout << "Login successful" << std::endl;
    }

    else if (command == "MESSAGE")
    {
        // get the header of the destination and the msg body
        std::string gameName = frame.getHeader("destination");
        std::string body = frame.getBody();

        if (gameName.size() > 0 && gameName[0] == '/')
        {
            gameName = gameName.substr(1);
        }

        // Parse the message
        GameEvent event = parseEventBody(body);

        std::string user = "";
        size_t userPos = body.find("user:");
        if (userPos != std::string::npos)
        {
            size_t endLine = body.find('\n', userPos);
            user = body.substr(userPos + 5, endLine - (userPos + 5));
        }

        if (!user.empty() && user != currentUserName)
        {
            gameUpdates[gameName][user].push_back(event);
            // Sort events by time immediately after insertion
            std::sort(gameUpdates[gameName][user].begin(), gameUpdates[gameName][user].end(), StompProtocol::eventComparator);
        }
        // Print according to format
        std::cout << gameName << ": " << body << std::endl;
    }

    // Reciept case
    else if (command == "RECEIPT")
    {
        // Getting the reciept ID
        std::string receiptIdStr = frame.getHeader("receipt-id");
        if (!receiptIdStr.empty())
        {
            int receiptId = std::stoi(receiptIdStr);

            // Checking if we waited for this reciept
            if (receipts.count(receiptId))
            {
                std::string action = receipts[receiptId];

                // Printing Action
                std::cout << action << std::endl;

                // Check if can discconected
                if (action == "DISCONNECT")
                {
                    // Close the connection
                    return false;
                }

                // Map Cleaning
                receipts.erase(receiptId);
            }
        }
    }

    // Error case
    else if (command == "ERROR")
    {

        std::cout << "Error received from server:" << std::endl;
        std::cout << frame.toString() << std::endl;

        return false;
    }

    return true;
}

GameEvent StompProtocol::parseEventBody(const std::string &body)
{
    GameEvent event;
    std::stringstream ss(body);
    std::string line;

    std::string currentSection = "";

    while (std::getline(ss, line))
    {
        // Pass empty lines
        if (line.empty())
            continue;

        // Split the Key and Values
        size_t colonPos = line.find(':');
        if (colonPos != std::string::npos)
        {
            std::string key = line.substr(0, colonPos);
            std::string value = line.substr(colonPos + 1);

            // Trimming non neccecary spaces
            if (value.size() > 0 && value[0] == ' ')
                value = value.substr(1);

            // Initializing event
            if (key == "team a")
                event.team_a_name = value;
            else if (key == "team b")
                event.team_b_name = value;
            else if (key == "event name")
                event.event_name = value;
            else if (key == "time")
                event.time = std::stoi(value);
            else if (key == "general game updates")
                currentSection = "general";
            else if (key == "team a updates")
                currentSection = "team_a";
            else if (key == "team b updates")
                currentSection = "team_b";
            else if (key == "description")
            {
                currentSection = "description";
                event.description = value;
            }
            else
            {
                // updates
                if (currentSection == "general")
                    event.general_game_updates[key] = value;
                else if (currentSection == "team_a")
                    event.team_a_updates[key] = value;
                else if (currentSection == "team_b")
                    event.team_b_updates[key] = value;
            }
        }
        else if (currentSection == "description")
        {
            event.description += "\n" + line;
        }
    }
    return event;
}

void StompProtocol::writeSummaryToFile(const std::string &gameName, const std::string &userName, const std::string &fileName)
{

    if (gameUpdates.find(gameName) == gameUpdates.end() ||
        gameUpdates[gameName].find(userName) == gameUpdates[gameName].end())
    {
        std::cerr << "No updates found for " << gameName << " from user " << userName << std::endl;
        return;
    }

    const std::vector<GameEvent> &events = gameUpdates[gameName][userName];
    if (events.empty())
    {
        return;
    }

    std::ofstream file(fileName);
    if (!file.is_open())
    {
        std::cerr << "Error opening file: " << fileName << std::endl;
        return;
    }

    file << events[0].team_a_name << " vs " << events[0].team_b_name << "\n";

    file << "Game stats:\n";
    file << "General stats:\n";

    std::map<std::string, std::string> finalGeneralStats;
    std::map<std::string, std::string> finalTeamAStats;
    std::map<std::string, std::string> finalTeamBStats;

    for (const auto &event : events)
    {
        for (auto const &[key, val] : event.general_game_updates)
            finalGeneralStats[key] = val;
        for (auto const &[key, val] : event.team_a_updates)
            finalTeamAStats[key] = val;
        for (auto const &[key, val] : event.team_b_updates)
            finalTeamBStats[key] = val;
    }

    for (auto const &[key, val] : finalGeneralStats)
        file << key << ": " << val << "\n";

    file << events[0].team_a_name << " stats:\n";
    for (auto const &[key, val] : finalTeamAStats)
        file << key << ": " << val << "\n";

    file << events[0].team_b_name << " stats:\n";
    for (auto const &[key, val] : finalTeamBStats)
        file << key << ": " << val << "\n";

    file << "Game event reports:\n";
    for (const auto &event : events)
    {
        file << event.time << " - " << event.event_name << ":\n";
        file << event.description << "\n\n";
    }

    file.close();
    std::cout << "Summary created: " << fileName << std::endl;
}

// Auxiliary function for report case in processKeybordCommand
std::vector<StompFrame> StompProtocol::parseReportFromFile(const std::string &jsonFilePath)
{
    std::vector<StompFrame> frames;

    // Parse the JSON file using the provided parser
    names_and_events NE = parseEventsFile(jsonFilePath);

    // Construct the Game Name
    std::string gameName = NE.team_a_name + "_" + NE.team_b_name;

    // Iterate over the events
    for (const auto &event : NE.events)
    {
        // Save to client's memory by creating a GameEvent
        GameEvent gameEvent;
        gameEvent.team_a_name = NE.team_a_name;
        gameEvent.team_b_name = NE.team_b_name;
        gameEvent.event_name = event.get_name();
        gameEvent.time = event.get_time();
        gameEvent.general_game_updates = event.get_game_updates();
        gameEvent.team_a_updates = event.get_team_a_updates();
        gameEvent.team_b_updates = event.get_team_b_updates();
        gameEvent.description = event.get_discription();

        // Add to the map under the current user
        gameUpdates[gameName][currentUserName].push_back(gameEvent);

        // Create SEND Frame
        std::string body = "user:" + currentUserName + "\n";
        body += "team a:" + NE.team_a_name + "\n";
        body += "team b:" + NE.team_b_name + "\n";
        body += "event name:" + event.get_name() + "\n";
        body += "time:" + std::to_string(event.get_time()) + "\n";

        body += "general game updates:\n";
        for (const auto &pair : event.get_game_updates())
        {
            body += pair.first + ":" + pair.second + "\n";
        }

        body += "team a updates:\n";
        for (const auto &pair : event.get_team_a_updates())
        {
            body += pair.first + ":" + pair.second + "\n";
        }

        body += "team b updates:\n";
        for (const auto &pair : event.get_team_b_updates())
        {
            body += pair.first + ":" + pair.second + "\n";
        }

        body += "description:\n" + event.get_discription();

        StompFrame frame = createSendFrame(gameName, body);
        frame.addHeader("file-name", jsonFilePath);
        frames.push_back(frame);
    }

    // Sort the events in memory to ensure correct order
    if (gameUpdates[gameName].count(currentUserName))
    {
        std::sort(gameUpdates[gameName][currentUserName].begin(),
                  gameUpdates[gameName][currentUserName].end(),
                  StompProtocol::eventComparator);
    }

    return frames;
}

// Comparator ensures correct order considering by using a 'before halftime' flag
bool StompProtocol::eventComparator(const GameEvent &a, const GameEvent &b)
{
    bool a_has_flag = a.general_game_updates.count("before halftime");
    bool b_has_flag = b.general_game_updates.count("before halftime");

    if (a_has_flag && b_has_flag)
    {
        std::string a_val = a.general_game_updates.at("before halftime");
        std::string b_val = b.general_game_updates.at("before halftime");

        if (a_val == "true" && b_val == "false")
            return true;
        if (a_val == "false" && b_val == "true")
            return false;
    }
    return a.time < b.time;
}