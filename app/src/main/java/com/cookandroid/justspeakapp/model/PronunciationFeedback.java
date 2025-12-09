package com.cookandroid.justspeakapp.model;

import java.util.List;

public class PronunciationFeedback {
    private float accuracyScore; // 0-100
    private float fluencyScore; // 0-100
    private float completenessScore; // 0-100
    private float prosodyScore; // 억양 점수 (Azure)
    private float overallScore; // 전체 점수
    private List<String> mispronunciations;
    private List<GrammarError> grammarErrors;
    private String suggestion;
    private String problematicWords; // 발음이 어려운 단어들
    private String wordDetails; // 단어별 상세 피드백 (Azure)

    public PronunciationFeedback() {
        this.accuracyScore = 0;
        this.fluencyScore = 0;
        this.completenessScore = 0;
        this.prosodyScore = 0;
        this.overallScore = 0;
    }

    // Getters
    public float getAccuracyScore() { return accuracyScore; }
    public float getFluencyScore() { return fluencyScore; }
    public float getCompletenessScore() { return completenessScore; }
    public float getProsodyScore() { return prosodyScore; }
    public float getOverallScore() {
        if (overallScore > 0) {
            return overallScore;
        }
        return (accuracyScore + fluencyScore + completenessScore) / 3;
    }
    public List<String> getMispronunciations() { return mispronunciations; }
    public List<GrammarError> getGrammarErrors() { return grammarErrors; }
    public String getSuggestion() { return suggestion; }
    public String getProblematicWords() { return problematicWords; }
    public String getWordDetails() { return wordDetails; }

    // Setters
    public void setAccuracyScore(float accuracyScore) { this.accuracyScore = accuracyScore; }
    public void setFluencyScore(float fluencyScore) { this.fluencyScore = fluencyScore; }
    public void setCompletenessScore(float completenessScore) { this.completenessScore = completenessScore; }
    public void setProsodyScore(float prosodyScore) { this.prosodyScore = prosodyScore; }
    public void setOverallScore(float overallScore) { this.overallScore = overallScore; }
    public void setMispronunciations(List<String> mispronunciations) { this.mispronunciations = mispronunciations; }
    public void setGrammarErrors(List<GrammarError> grammarErrors) { this.grammarErrors = grammarErrors; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public void setProblematicWords(String problematicWords) { this.problematicWords = problematicWords; }
    public void setWordDetails(String wordDetails) { this.wordDetails = wordDetails; }
}
