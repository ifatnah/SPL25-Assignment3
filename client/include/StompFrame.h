#pragma once

#include <string>
#include <map>

class StompFrame
{
private:
    // Type of command
    std::string command;
    // Added info
    std::map<std::string, std::string> headers;
    // Msg content
    std::string body;

public:
    // Constructors
    StompFrame();
    StompFrame(const std::string &command);

    // Parse from string constructor
    StompFrame(const std::string &frameString, bool parse);

    // Getters
    const std::string &getCommand() const;
    const std::string &getBody() const;
    std::string getHeader(const std::string &key) const;
    const std::map<std::string, std::string> &getHeaders() const;

    // Setters
    void setCommand(const std::string &command);
    void setBody(const std::string &body);
    void addHeader(const std::string &key, const std::string &value);

    // Convert frame to string for sending
    std::string toString() const;

    // Parse a string into this frame
    void parse(const std::string &frameString);

    // Check if header exists
    bool hasHeader(const std::string &key) const;
};