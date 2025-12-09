package com.cookandroid.justspeakapp.model;

public class GrammarError {
    private String errorType; // tense, article, subject-verb agreement, etc.
    private String incorrectText;
    private String correctedText;
    private String explanation;

    public GrammarError(String errorType, String incorrectText, String correctedText, String explanation) {
        this.errorType = errorType;
        this.incorrectText = incorrectText;
        this.correctedText = correctedText;
        this.explanation = explanation;
    }

    // Getters
    public String getErrorType() { return errorType; }
    public String getIncorrectText() { return incorrectText; }
    public String getCorrectedText() { return correctedText; }
    public String getExplanation() { return explanation; }

    // Setters
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public void setIncorrectText(String incorrectText) { this.incorrectText = incorrectText; }
    public void setCorrectedText(String correctedText) { this.correctedText = correctedText; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
