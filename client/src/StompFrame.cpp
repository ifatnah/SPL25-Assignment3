#include "StompFrame.h"
#include <sstream>

// Empty Constructor
StompFrame::StompFrame() : command(""), headers(), body("") {}

// Constructor with command
StompFrame::StompFrame(const std::string &command) : command(command), headers(), body("") {}

// Constructor with command 
StompFrame::StompFrame(const std::string &command, bool parse) : command(command), headers(), body("")
{
    if (parse)
    {
        this->parse(command);
    }
}

// Getters
const std::string &StompFrame ::getCommand() const
{
    return this->command;
}

const std::string &StompFrame ::getBody() const
{
    return this->body;
}

std::string StompFrame ::getHeader(const std::string &key) const
{
    auto header = headers.find(key);
    if (header != headers.end())
    {
        return header->second;
    }
    return "";
}

const std::map<std::string, std::string> &StompFrame ::getHeaders() const
{
    return this->headers;
}

// Setters
void StompFrame ::setCommand(const std::string &command)
{
    this->command = command;
}

void StompFrame ::setBody(const std::string &body)
{
    this->body = body;
}

void StompFrame ::addHeader(const std::string &key, const std::string &value)
{
    headers[key] = value;
}

// Parse frame to string
std::string StompFrame::toString() const
{
    std::stringstream ss;

    // Command line
    ss << command << "\n";

    // Headers
    for (const auto &pair : headers)
    {
        ss << pair.first << ":" << pair.second << "\n";
    }

    // Empty line separates headers from body
    ss << "\n";

    // Body
    ss << body;

    // Null terminator (^@)
    ss << '\0';

    return ss.str();
}

// Parse a string into this frame
void StompFrame::parse(const std::string &frameString)
{
    // Clear existing data
    command = "";
    headers.clear();
    body = "";

    std::istringstream stream(frameString);
    std::string line;

    // First line is the command
    if (std::getline(stream, line))
    {
        // Remove \r if present (for Windows line endings)
        if (!line.empty() && line.back() == '\r')
        {
            line.pop_back();
        }
        command = line;
    }

    // Read headers until empty line
    while (std::getline(stream, line))
    {
        // Remove \r if present
        if (!line.empty() && line.back() == '\r')
        {
            line.pop_back();
        }

        // Empty line marks end of headers
        if (line.empty())
        {
            break;
        }

        // Parse header (key:value)
        size_t colonPos = line.find(':');
        if (colonPos != std::string::npos)
        {
            std::string key = line.substr(0, colonPos);
            std::string value = line.substr(colonPos + 1);
            headers[key] = value;
        }
    }

    // Rest is the body
    std::stringstream bodyStream;
    bool firstLine = true;
    while (std::getline(stream, line))
    {
        if (!firstLine)
        {
            bodyStream << "\n";
        }
        // Remove \r if present
        if (!line.empty() && line.back() == '\r')
        {
            line.pop_back();
        }
        bodyStream << line;
        firstLine = false;
    }
    body = bodyStream.str();
}

// Check if header exists
bool StompFrame ::hasHeader(const std::string &key) const
{
    return (headers.find(key) != headers.end());
}
