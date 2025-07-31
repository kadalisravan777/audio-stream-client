package com.audiostream.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

public class AudioWebSocketClient extends WebSocketClient {

    private final String sessionId;
    private final String participantId;
    private final CountDownLatch openedLatch = new CountDownLatch(1);

    public AudioWebSocketClient(String serverUri, String sessionId, String participantId) throws Exception {
        super(new URI(serverUri));
        this.sessionId = sessionId;
        this.participantId = participantId;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("🔌 Connected to WebSocket");

        // Send AudioHook-compliant "open" message
        JSONObject openMessage = new JSONObject();
        openMessage.put("event", "open");
        openMessage.put("sessionId", sessionId);
        openMessage.put("participantId", participantId);
        send(openMessage.toString());
    }

    @Override
    public void onMessage(String message) {
        // Listen for "opened" response from server
        try {
            JSONObject response = new JSONObject(message);
            String event = response.optString("event");
            if ("opened".equals(event)) {
                System.out.println("✅ Stream opened by server.");
                openedLatch.countDown(); // allow audio to start
            }
        } catch (Exception e) {
            System.err.println("⚠️ Non-JSON message: " + message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("🔌 WebSocket connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    // Send audio only after "opened"
    public void sendAudio(byte[] audioBytes) throws InterruptedException {
        openedLatch.await(); // wait for "opened" before sending
        send(ByteBuffer.wrap(audioBytes)); // send as binary
    }

    public void sendCloseEvent() {
        JSONObject closeMessage = new JSONObject();
        closeMessage.put("event", "close");
        closeMessage.put("sessionId", sessionId);
        closeMessage.put("participantId", participantId);
        send(closeMessage.toString());
    }
}
