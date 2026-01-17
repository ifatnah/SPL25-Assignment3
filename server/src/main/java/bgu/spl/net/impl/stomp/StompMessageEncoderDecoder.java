package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// Implements the encoder-decoder for STOMP frames using '\0' as delimiter
public class StompMessageEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] buffer = new byte[1024];
    private int endByte = 0;

    public String decodeNextByte(byte nextByte) {
        // Checks for null terminator to finalize message
        if (nextByte == '\0') {
            String result = new String(buffer, 0, endByte, StandardCharsets.UTF_8);
            endByte = 0;
            return result;
        } else {
            // Resizes buffer if necessary
            if (endByte >= buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }
            buffer[endByte++] = nextByte;
            return null;
        }
    }

    // Encodes the message by appending the null character
    public byte[] encode(String message) {
        return (message + "\u0000").getBytes(StandardCharsets.UTF_8);
    }
}