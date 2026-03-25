package com.example.pokemonbattle.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class ChatManager {

    private final VBox chatMessagesContainer;
    private final ScrollPane chatScrollPane;
    private final BlockingQueue<ChatMessage> messageQueue;
    private final Thread chatProcessorThread;
    private volatile boolean running;
    private final List<String> chatHistory;
    private static final int MAX_MESSAGES = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Represents a chat message with metadata
     */
    private static class ChatMessage {
        final String text;
        final MessageType type;
        final String senderName;
        final long timestamp;

        ChatMessage(String text, MessageType type, String senderName) {
            this.text = text;
            this.type = type;
            this.senderName = senderName;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Types of chat messages
     */
    public enum MessageType {
        PLAYER,      // Message from player
        OPPONENT,    // Message from opponent (online mode)
        SYSTEM       // System messages
    }

    /**
     * Predefined quick messages
     */
    public static final String[] QUICK_MESSAGES = {
        "Well played!",
        "Good luck!",
        "Better luck next time!",
        "Nice move!"
    };

    public ChatManager(VBox chatMessagesContainer, ScrollPane chatScrollPane) {
        this.chatMessagesContainer = chatMessagesContainer;
        this.chatScrollPane = chatScrollPane;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.chatHistory = new ArrayList<>();
        this.running = true;

        // Start the chat processor thread
        this.chatProcessorThread = new Thread(this::processChatMessages, "ChatProcessor");
        this.chatProcessorThread.setDaemon(true);
        this.chatProcessorThread.start();
    }

    /**
     * Add a message to the chat queue
     */
    public void addMessage(String text, MessageType type) {
        addMessage(text, type, null);
    }

    /**
     * Add a message to the chat queue with optional sender name.
     */
    public void addMessage(String text, MessageType type, String senderName) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        messageQueue.offer(new ChatMessage(text.trim(), type, senderName));
    }

    /**
     * Add a player message
     */
    public void sendPlayerMessage(String text) {
        addMessage(text, MessageType.PLAYER);
    }

    /**
     * Add an opponent message (for online battles)
     */
    public void receiveOpponentMessage(String text) {
        addMessage(text, MessageType.OPPONENT, "Opponent");
    }

    /**
     * Add an opponent message with explicit sender name.
     */
    public void receiveOpponentMessage(String senderName, String text) {
        String resolvedSender = (senderName == null || senderName.isBlank()) ? "Opponent" : senderName.trim();
        addMessage(text, MessageType.OPPONENT, resolvedSender);
    }

    /**
     * Add a system message
     */
    public void addSystemMessage(String text) {
        addMessage(text, MessageType.SYSTEM);
    }

    /**
     * Process messages from the queue in a separate thread
     */
    private void processChatMessages() {
        while (running) {
            try {
                ChatMessage message = messageQueue.take();

                // Add to history
                chatHistory.add(String.format("[%s] %s: %s",
                    LocalTime.now().format(TIME_FORMATTER),
                    message.type,
                    message.text));

                // Trim history if too large
                if (chatHistory.size() > MAX_MESSAGES) {
                    chatHistory.remove(0);
                }

                // Update UI on JavaFX thread
                Platform.runLater(() -> displayMessage(message));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Display a message in the chat UI (must be called on JavaFX thread)
     */
    private void displayMessage(ChatMessage message) {
        Label messageLabel = createMessageLabel(message);
        chatMessagesContainer.getChildren().add(messageLabel);

        // Trim old messages if container gets too large
        if (chatMessagesContainer.getChildren().size() > MAX_MESSAGES) {
            chatMessagesContainer.getChildren().remove(0);
        }

        // Auto-scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Create a styled label for a chat message
     */
    private Label createMessageLabel(ChatMessage message) {
        String timeStr = LocalTime.now().format(TIME_FORMATTER);
        String displayText;
        String styleClass;

        switch (message.type) {
            case PLAYER:
                displayText = String.format("[%s] You: %s", timeStr, message.text);
                styleClass = "chat-message-player";
                break;
            case OPPONENT:
                String sender = (message.senderName == null || message.senderName.isBlank())
                        ? "Opponent"
                        : message.senderName;
                displayText = String.format("[%s] %s: %s", timeStr, sender, message.text);
                styleClass = "chat-message-opponent";
                break;
            case SYSTEM:
                displayText = String.format("[%s] %s", timeStr, message.text);
                styleClass = "chat-message-system";
                break;
            default:
                displayText = message.text;
                styleClass = "chat-message";
        }

        Label label = new Label(displayText);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add(styleClass);

        return label;
    }

    /**
     * Clear all messages from chat
     */
    public void clearChat() {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().clear();
            chatHistory.clear();
        });
    }

    /**
     * Get chat history as list of strings
     */
    public List<String> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    /**
     * Shutdown the chat manager and stop the processing thread
     */
    public void shutdown() {
        running = false;
        if (chatProcessorThread != null && chatProcessorThread.isAlive()) {
            chatProcessorThread.interrupt();
        }
    }
}
