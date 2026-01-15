package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;

public class StompFrame {
    private String command;
    private Map<String, String> headers = new HashMap<>();
    private String body;

    // Constructor
    private StompFrame() {}

    public static StompFrame parse(String message) {
        // Create a frame and array of strings
        StompFrame frame = new StompFrame();
        String[] lines = message.split("\n");
        
        if (lines.length > 0) {
            // Extracting command and trimming it
            frame.command = lines[0].trim();
            
            int i = 1;
            // Extract the headers key and values
            while (i < lines.length && !lines[i].trim().isEmpty()) {
                // Splitting by ":"
                String[] parts = lines[i].split(":", 2); 
                if (parts.length == 2) {
                    frame.headers.put(parts[0].trim(), parts[1].trim());
                }
                i++;
            }
            
            // Body builder
             StringBuilder bodyBuilder = new StringBuilder();
            for (i = i + 1; i < lines.length; i++) {
                bodyBuilder.append(lines[i]).append("\n");
            }
            frame.body = bodyBuilder.toString().trim();
        }
        return frame;
    }

    public String getCommand() { return command; }
    public String getHeader(String key) { return headers.get(key); }
    public String getBody() { return body; }
}