package com.cookandroid.justspeakapp.model;

import java.util.Date;

public class LearningProgress {
    private String userId;
    private int dailyGoal;
    private int dailyCompleted;
    private int totalSessions;
    private float averagePronunciationScore;
    private float averageGrammarScore;
    private String currentLevel; // beginner, intermediate, advanced
    private Date lastPracticeDate;

    public LearningProgress(String userId) {
        this.userId = userId;
        this.dailyGoal = 5;
        this.dailyCompleted = 0;
        this.totalSessions = 0;
        this.averagePronunciationScore = 0;
        this.averageGrammarScore = 0;
        this.currentLevel = "beginner";
        this.lastPracticeDate = new Date();
    }

    // Getters
    public String getUserId() { return userId; }
    public int getDailyGoal() { return dailyGoal; }
    public int getDailyCompleted() { return dailyCompleted; }
    public int getTotalSessions() { return totalSessions; }
    public float getAveragePronunciationScore() { return averagePronunciationScore; }
    public float getAverageGrammarScore() { return averageGrammarScore; }
    public String getCurrentLevel() { return currentLevel; }
    public Date getLastPracticeDate() { return lastPracticeDate; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDailyGoal(int dailyGoal) { this.dailyGoal = dailyGoal; }
    public void setDailyCompleted(int dailyCompleted) { this.dailyCompleted = dailyCompleted; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
    public void setAveragePronunciationScore(float averagePronunciationScore) {
        this.averagePronunciationScore = averagePronunciationScore;
    }
    public void setAverageGrammarScore(float averageGrammarScore) {
        this.averageGrammarScore = averageGrammarScore;
    }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public void setLastPracticeDate(Date lastPracticeDate) { this.lastPracticeDate = lastPracticeDate; }

    public void incrementDailyCompleted() {
        this.dailyCompleted++;
        this.totalSessions++;
    }

    public float getProgressPercentage() {
        return (float) dailyCompleted / dailyGoal * 100;
    }
}
