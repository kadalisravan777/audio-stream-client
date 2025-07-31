package com.audiostream.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client")
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private ClientTranscriptionService transcriptionService;

    @GetMapping("/start")
    public String startStreaming() {
        logger.info("📨 Received request to start audio streaming from client.");
        transcriptionService.startStreamingToApi();
        return "🎙️ Microphone streaming started.";
    }

    @GetMapping("/stop")
    public String stopStreaming() {
        logger.info("📨 Received request to stop audio streaming from client.");
        transcriptionService.stopStreaming();
        return "🛑 Microphone streaming stopped.";
    }
}
