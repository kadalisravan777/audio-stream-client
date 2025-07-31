package com.audiostream.client;



import javax.sound.sampled.*;
import java.util.function.Consumer;

public class MicrophoneAudioCapture {
    private TargetDataLine microphoneLine;

    public void startCapturing(int sampleRate, Consumer<byte[]> onAudioChunk) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        microphoneLine = (TargetDataLine) AudioSystem.getLine(info);
        microphoneLine.open(format);
        microphoneLine.start();

        System.out.println("🎤 Microphone started successfully.");

        // Run capture in a separate thread
        new Thread(() -> {
            byte[] buffer = new byte[4096]; // ~250ms buffer
            try {
                while (!Thread.currentThread().isInterrupted() && microphoneLine.isOpen()) {
                    int bytesRead = microphoneLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        byte[] chunk = new byte[bytesRead];
                        System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                        onAudioChunk.accept(chunk); // send chunk in real-time
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stop();
            }
        }, "Mic-Capture-Thread").start();
    }

    public void stop() {
        if (microphoneLine != null) {
            microphoneLine.stop();
            microphoneLine.close();
            System.out.println("🎙️ Microphone stopped and closed.");
        }
    }
}
