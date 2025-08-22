package com.audiostream.app.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.LineUnavailableException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.audiostream.app.dto.CloseMessage;
import com.audiostream.app.dto.ClosedMessage;
import com.audiostream.app.dto.OpenMessage;
import com.audiostream.app.dto.OpenedMessage;
import com.audiostream.app.dto.PauseMessage;
import com.audiostream.app.dto.ResumeMessage;
import com.audiostream.app.service.ClientTranscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AudioWebSocketClient extends WebSocketClient {

	private static final Logger log = LoggerFactory.getLogger(AudioWebSocketClient.class);
	private final String sessionId;
	private final String participantId;
	private final CountDownLatch openedLatch = new CountDownLatch(1);
	private OpenedMessage openedMessage;
	private CloseMessage closeMessage;
	private ClosedMessage closedMessage;
	private PauseMessage pauseMessage;
	private ResumeMessage resumeMessage;
	ObjectMapper mapper = new ObjectMapper();

	
	private ClientTranscriptionService clientTranscriptionService;
	
	public AudioWebSocketClient(String serverUri, String sessionId, String participantId,ClientTranscriptionService clientTranscriptionService) throws Exception {
		super(new URI(serverUri));
		this.sessionId = sessionId;
		this.participantId = participantId;
		this.clientTranscriptionService = clientTranscriptionService;
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		log.info("🔌 Connected to WebSocket");

		OpenMessage openMessage = null;
		try {

			ObjectMapper mapper = new ObjectMapper();
			ClassPathResource resource = new ClassPathResource("openMessage.json");
			openMessage = mapper.readValue(resource.getInputStream(), OpenMessage.class);
			String jsonString = mapper.writeValueAsString(openMessage);

			log.info("Sending JSON: " + jsonString);

			// ✅ Send valid JSON over WebSocket
			send(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void onMessage(String message) {
	    try {
	        log.debug("RAW: {}", message);  // always log raw server message

	        JSONObject response = new JSONObject(message);
	        String type = response.optString("type", null);

	        if (type == null || type.isBlank()) {
	            log.warn("⚠️ Received message without type: {}", message);
	            return;
	        }

	        log.info("📩 Received type: {}", type);

	        switch (type) {
	            case "opened"  -> handleOpened(message);
	            case "pong"    -> handlePong(message);
	            case "closed"  -> handleClosed(message);
	            case "paused"  -> handlePause(message);
	            case "resumed" -> handleResume(message);
	            default        -> log.warn("⚠️ Unknown message type: {}, raw: {}", type, message);
	        }

	    } catch (Exception e) {
	        log.error("❌ Error parsing server message: {}", message, e);
	    }
	}


	private void handlePause(String message) {
	    log.info("✅ Pause ACK received from server");
	    clientTranscriptionService.pauseMic(); // double ensure mic is stopped
	}

	private void handleResume(String message) {
	    log.info("✅ Resume ACK received from server");
	    try {
	        clientTranscriptionService.enableMic(); // start fresh thread
	    } catch (LineUnavailableException e) {
	        log.error("❌ Failed to restart mic on resume", e);
	    }
	}

	private void handleClosed(String message) {
		try {
			log.info("Received Closed Event from Server");
			ObjectMapper mapper = new ObjectMapper();
			closedMessage = mapper.readValue(message, ClosedMessage.class);
			log.info(mapper.writeValueAsString(closedMessage));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private Object handlePong(String message) {
		// TODO Auto-generated method stub
		return null;
	}

	private void handleOpened(String message) {
		try {
			log.info("✅ Stream opened by server.");
			openedMessage = mapper.readValue(message, OpenedMessage.class);
			log.info(mapper.writeValueAsString(openedMessage));
			openedLatch.countDown(); // allow audio to start
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		log.info("🔌 WebSocket connection closed: " + reason);
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
		try {
			ObjectMapper mapper = new ObjectMapper();
			ClassPathResource resource = new ClassPathResource("closeMessage.json");
			closeMessage = mapper.readValue(resource.getInputStream(), CloseMessage.class);
			String jsonString = mapper.writeValueAsString(closeMessage);
			log.info("Sending JSON: " + jsonString);
			// ✅ Send valid JSON over WebSocket
			send(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendPauseEventToServer() {
		ClassPathResource resource = new ClassPathResource("pauseMessage.json");
		try {
			pauseMessage = mapper.readValue(resource.getInputStream(), PauseMessage.class);
			String jsonString = mapper.writeValueAsString(pauseMessage);

			log.info("Sending Pause JSON: " + jsonString);

			send(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void sendResumeEventToServer() {
		ClassPathResource resource = new ClassPathResource("resumeMessage.json");
		try {
			resumeMessage = mapper.readValue(resource.getInputStream(), ResumeMessage.class);
			String jsonString = mapper.writeValueAsString(resumeMessage);

			log.info("Sending Resumed JSON: " + jsonString);

			send(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
