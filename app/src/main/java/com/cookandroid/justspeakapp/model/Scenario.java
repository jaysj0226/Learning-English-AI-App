package com.cookandroid.justspeakapp.model;

public class Scenario {
    private String id;
    private String title;
    private String emoji;
    private String description;
    private String difficulty; // beginner, intermediate, advanced
    private String category; // daily, travel, interview, business

    public Scenario(String id, String title, String emoji, String description, String difficulty, String category) {
        this.id = id;
        this.title = title;
        this.emoji = emoji;
        this.description = description;
        this.difficulty = difficulty;
        this.category = category;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
    public String getDifficulty() { return difficulty; }
    public String getCategory() { return category; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public void setDescription(String description) { this.description = description; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setCategory(String category) { this.category = category; }
}
