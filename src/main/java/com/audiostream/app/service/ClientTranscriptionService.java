package com.audiostream.app.service;

import javax.sound.sampled.LineUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.audiostream.app.client.AudioWebSocketClient;
import com.audiostream.app.helper.MicrophoneAudioCapture;

@Service
public class ClientTranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(ClientTranscriptionService.class);

    private AudioWebSocketClient wsClient;
    private MicrophoneAudioCapture mic;

    private final String sessionId = "demo-session-001";
    private final String participantId = "user-123";
    
    public synchronized void startStreamingToApi() {
        try {
            logger.info("🎤 Starting mic capture...");

            wsClient = new AudioWebSocketClient("ws://localhost:8080/audiohook/ws", sessionId, participantId,this);
            wsClient.connectBlocking();

            enableMic();

            logger.info("✅ Streaming started for session {}", sessionId);

        } catch (Exception e) {
            logger.error("❌ Error during startStreamingToApi: ", e);
        }
    }

	public void enableMic() throws LineUnavailableException {
		mic = new MicrophoneAudioCapture();
		mic.startCapturing(8000, audioChunk -> {
		    try {
		        wsClient.sendAudio(audioChunk);
		    } catch (Exception e) {
		        logger.error("❌ WebSocket send failed: ", e);
		    }
		});
	}

    public synchronized void stopStreaming() {
        try {
            if (mic != null) {
                mic.stop();
                logger.info("🎤 Microphone stopped.");
            }

            if (wsClient != null && wsClient.isOpen()) {
                wsClient.sendCloseEvent(); // Inform server stream is done
                wsClient.close();
                logger.info("🔌 WebSocket closed for session {}", sessionId);
            }

            logger.info("🛑 Streaming session {} finished.", sessionId);

        } catch (Exception e) {
            logger.error("❌ Error during stopStreaming: ", e);
        }
    }
    public void pauseEvent() {
        logger.info("⏸️ Requesting pause...");
        // 1) stop mic immediately
        pauseMic();
        // 2) inform server
        wsClient.sendPauseEventToServer();
    }

    public synchronized void pauseMic() {
        if (mic != null) {
            logger.info("🎙️ Stopping microphone locally...");
            mic.stop();
            mic = null;
        } else {
            logger.warn("⚠️ pauseMic() called but mic is already null.");
        }
    }

    public void resumeEvent() {
        logger.info("▶️ Requesting resume...");
        wsClient.sendResumeEventToServer();
    }

}
