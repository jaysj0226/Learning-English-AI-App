package com.cookandroid.justspeakapp.model;

public class ScenarioProgress {
    private String scenarioId;
    private String scenarioName;
    private int completedLessons;
    private int totalLessons;
    private int progressPercent;

    public ScenarioProgress(String scenarioId, String scenarioName, int completedLessons, int totalLessons) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.completedLessons = completedLessons;
        this.totalLessons = totalLessons;
        this.progressPercent = totalLessons > 0 ? (int) ((completedLessons / (float) totalLessons) * 100) : 0;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
        this.progressPercent = totalLessons > 0 ? (int) ((completedLessons / (float) totalLessons) * 100) : 0;
    }
}
