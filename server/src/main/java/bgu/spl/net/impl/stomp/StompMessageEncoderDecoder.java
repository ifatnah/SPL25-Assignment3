package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StompMessageEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] buffer = new byte[1024];
    private int endByte = 0;

    public String decodeNextByte(byte nextByte) {
        if (nextByte == '\0') {
            String result = new String(buffer, 0, endByte, StandardCharsets.UTF_8);
            endByte = 0;
            return result;
        } else {
            if (endByte >= buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }
            buffer[endByte++] = nextByte;
            return null;
        }
    }

    public byte[] encode(String message) {
        return (message + "\u0000").getBytes(StandardCharsets.UTF_8);
    }
}
