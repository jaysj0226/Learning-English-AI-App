package com.cookandroid.justspeakapp.model;

import java.util.Date;

public class ConversationMessage {
    private String id;
    private String scenarioId;
    private String speaker; // "user" or "ai"
    private String text;
    private String audioPath;
    private Date timestamp;
    private PronunciationFeedback feedback;

    public ConversationMessage(String id, String scenarioId, String speaker, String text) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.speaker = speaker;
        this.text = text;
        this.timestamp = new Date();
    }

    // Getters
    public String getId() { return id; }
    public String getScenarioId() { return scenarioId; }
    public String getSpeaker() { return speaker; }
    public String getText() { return text; }
    public String getAudioPath() { return audioPath; }
    public Date getTimestamp() { return timestamp; }
    public PronunciationFeedback getFeedback() { return feedback; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public void setText(String text) { this.text = text; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setFeedback(PronunciationFeedback feedback) { this.feedback = feedback; }
}
