package com.cookandroid.justspeakapp.service;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GeminiService {
    private static final String TAG = "GeminiService";
    private static final String MODEL_NAME = "gemini-2.0-flash";
    private static final int TIMEOUT_SECONDS = 30; // API 타임아웃
    private static final int MAX_RETRIES = 2; // 최대 재시도 횟수

    private GenerativeModelFutures model;
    private ChatFutures chat;
    private List<Content> history;
    private ExecutorService executor;
    private ScheduledExecutorService timeoutScheduler; // 타임아웃용 스케줄러 재사용
    private String apiKey;
    private String systemPrompt;
    private boolean isInitialized = false;

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public GeminiService(Context context, String apiKey) {
        this.apiKey = apiKey;
        this.executor = Executors.newSingleThreadExecutor();
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        this.history = new ArrayList<>();
        initializeModel();
    }

    private void initializeModel() {
        try {
            // API 키는 로그에 절대 노출하지 않음 (보안)
            Log.d(TAG, "Initializing Gemini service...");
            GenerativeModel gm = new GenerativeModel(MODEL_NAME, apiKey);
            model = GenerativeModelFutures.from(gm);
            isInitialized = true;
            Log.d(TAG, "Gemini model initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini model: " + e.getMessage(), e);
            isInitialized = false;
        }
    }

    public boolean isInitialized() {
        return isInitialized && model != null;
    }

    public void startConversation(String scenario, String userLevel) {
        // 시나리오별 시스템 프롬프트 설정
        systemPrompt = buildSystemPrompt(scenario, userLevel);

        // 채팅 세션 시작 - 시스템 프롬프트를 첫 메시지로 포함
        history.clear();

        // 시스템 프롬프트를 user 메시지로 추가
        Content.Builder userBuilder = new Content.Builder();
        userBuilder.setRole("user");
        userBuilder.addText(systemPrompt + "\n\nPlease start the conversation with a greeting.");
        Content systemContent = userBuilder.build();
        history.add(systemContent);

        // AI 응답을 model 메시지로 추가 (시나리오별 첫 인사)
        String initialGreeting = getInitialGreeting(scenario);
        Content.Builder modelBuilder = new Content.Builder();
        modelBuilder.setRole("model");
        modelBuilder.addText(initialGreeting);
        Content modelResponse = modelBuilder.build();
        history.add(modelResponse);

        // history를 포함하여 chat 시작
        chat = model.startChat(history);

        Log.d(TAG, "Conversation started for scenario: " + scenario + " with history size: " + history.size());
    }

    private String buildSystemPrompt(String scenario, String userLevel) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an English conversation teacher for Korean students. ");
        prompt.append("Your student's level is: ").append(userLevel).append(". ");

        switch (scenario) {
            // MainActivity에서 사용하는 시나리오 ID
            case "daily_conversation":
            case "daily":
            case "scenario_daily":
                prompt.append("Have a natural daily conversation. Topics to explore: ");
                prompt.append("morning routines, weekend plans, favorite foods, hobbies, family, pets, weather, movies/TV shows. ");
                prompt.append("Start with casual topics and ask follow-up questions. ");
                break;

            case "travel_english":
            case "travel":
            case "scenario_travel":
                prompt.append("Role-play travel scenarios. You can be: a hotel receptionist, airport staff, taxi driver, or tour guide. ");
                prompt.append("Topics: booking a room, asking for directions, ordering food at a restaurant, buying tickets, checking in/out. ");
                prompt.append("Use common travel phrases and help them practice real situations. ");
                break;

            case "interview_prep":
            case "interview":
            case "scenario_job_interview":
                prompt.append("Conduct a professional job interview. ");
                prompt.append("Topics: self-introduction, work experience, strengths/weaknesses, career goals, why this company, salary expectations. ");
                prompt.append("Ask typical interview questions and give constructive feedback. ");
                break;

            case "business":
            case "scenario_business":
                prompt.append("Discuss business topics in a professional setting. ");
                prompt.append("Topics: meetings, presentations, project updates, deadlines, negotiations, email etiquette, team collaboration. ");
                prompt.append("Use formal business English expressions. ");
                break;

            case "scenario_shopping":
                prompt.append("Role-play as a shop assistant or customer. ");
                prompt.append("Topics: asking for sizes/colors, comparing products, asking for discounts, returns/exchanges, payment methods. ");
                prompt.append("Practice common shopping phrases and expressions. ");
                break;

            case "scenario_restaurant":
                prompt.append("Role-play as a waiter/waitress or customer at a restaurant. ");
                prompt.append("Topics: making reservations, ordering food/drinks, asking about menu items, special requests, paying the bill. ");
                prompt.append("Practice restaurant vocabulary and polite expressions. ");
                break;

            case "scenario_hotel":
                prompt.append("Role-play hotel scenarios. You can be a receptionist or guest. ");
                prompt.append("Topics: check-in/check-out, room service, asking for amenities, reporting problems, extending stay. ");
                prompt.append("Practice hotel-specific vocabulary and polite requests. ");
                break;

            case "scenario_airport":
                prompt.append("Role-play airport scenarios. You can be airport staff or a traveler. ");
                prompt.append("Topics: check-in counter, security check, boarding, immigration, customs, lost luggage, flight delays. ");
                prompt.append("Practice airport announcements and common phrases. ");
                break;

            case "scenario_medical":
                prompt.append("Role-play medical situations. You can be a doctor, pharmacist, or patient. ");
                prompt.append("Topics: describing symptoms, making appointments, buying medicine, health insurance, emergency situations. ");
                prompt.append("Practice medical vocabulary and explaining health issues. ");
                break;

            case "scenario_phone":
                prompt.append("Practice phone conversation skills. ");
                prompt.append("Topics: making appointments, customer service calls, leaving voicemails, taking messages, conference calls. ");
                prompt.append("Practice phone etiquette and common expressions. ");
                break;

            case "scenario_presentation":
                prompt.append("Help practice presentation skills. ");
                prompt.append("Topics: opening/closing a presentation, explaining data/charts, handling Q&A, transitioning between topics. ");
                prompt.append("Give feedback on structure and expressions. ");
                break;

            case "scenario_meeting":
                prompt.append("Practice business meeting scenarios. ");
                prompt.append("Topics: setting agendas, giving opinions, agreeing/disagreeing politely, making suggestions, summarizing. ");
                prompt.append("Use formal meeting expressions and etiquette. ");
                break;

            case "scenario_negotiation":
                prompt.append("Practice negotiation skills. ");
                prompt.append("Topics: making offers, counteroffers, compromising, terms and conditions, closing deals. ");
                prompt.append("Use persuasive language and diplomatic expressions. ");
                break;

            case "scenario_email":
                prompt.append("Help write professional emails. ");
                prompt.append("Topics: formal greetings/closings, requesting information, apologizing, following up, scheduling. ");
                prompt.append("Practice email structure and professional tone. ");
                break;

            case "scenario_debate":
                prompt.append("Practice debate and discussion skills. ");
                prompt.append("Topics: current events, social issues, technology, environment, education. ");
                prompt.append("Practice expressing opinions, providing evidence, and respectful disagreement. ");
                break;

            case "scenario_networking":
                prompt.append("Practice networking and small talk. ");
                prompt.append("Topics: introducing yourself, exchanging business cards, industry talk, following up after events. ");
                prompt.append("Practice professional relationship building. ");
                break;

            default:
                prompt.append("Have a friendly English conversation. ");
                prompt.append("Adapt to what the student wants to talk about. ");
                break;
        }

        prompt.append("\nIMPORTANT RULES:\n");
        prompt.append("1. Keep responses SHORT (1-2 sentences max)\n");
        prompt.append("2. Use simple, natural English appropriate for their level\n");
        prompt.append("3. Ask follow-up questions to keep conversation flowing\n");
        prompt.append("4. Be encouraging and friendly\n");
        prompt.append("5. Respond in English only, no Korean\n");
        prompt.append("6. Stay in character for the scenario and stick to relevant topics\n");

        return prompt.toString();
    }

    private String getInitialGreeting(String scenario) {
        if (scenario == null) {
            return "Hi! Let's practice English together. How are you today?";
        }

        switch (scenario) {
            case "scenario_daily":
            case "daily_conversation":
            case "daily":
                return "Hi! Let's have a casual chat. How was your day today?";

            case "scenario_travel":
            case "travel_english":
            case "travel":
                return "Welcome! I'm here to help you practice travel English. Are you planning a trip soon?";

            case "scenario_shopping":
                return "Hello! Welcome to our store. Can I help you find something today?";

            case "scenario_restaurant":
                return "Good evening! Welcome to our restaurant. Would you like to see the menu?";

            case "scenario_business":
                return "Good morning. Let's practice some business English. What would you like to discuss today?";

            case "scenario_hotel":
                return "Welcome to our hotel! Do you have a reservation, or would you like to book a room?";

            case "scenario_airport":
                return "Hello! Welcome to the airport. May I see your passport and boarding pass?";

            case "scenario_medical":
                return "Hello, I'm the doctor. What brings you in today? How are you feeling?";

            case "scenario_phone":
                return "Hello, this is the customer service line. How may I help you today?";

            case "scenario_job_interview":
            case "interview_prep":
            case "interview":
                return "Hello, thank you for coming in today. Please have a seat. Can you tell me a little about yourself?";

            case "scenario_presentation":
                return "Let's practice your presentation skills. What topic would you like to present on?";

            case "scenario_meeting":
                return "Good morning everyone. Let's start our meeting. What's on the agenda today?";

            case "scenario_negotiation":
                return "Thank you for meeting with me today. Shall we discuss the terms of our agreement?";

            case "scenario_email":
                return "Let's practice writing professional emails. What kind of email do you need to write?";

            case "scenario_debate":
                return "Welcome to our discussion session. What topic would you like to debate today?";

            case "scenario_networking":
                return "Hi there! Nice to meet you. So, what brings you to this event?";

            default:
                return "Hi! Let's practice English together. What would you like to talk about?";
        }
    }

    public void sendMessage(String userMessage, GeminiCallback callback) {
        sendMessageWithRetry(userMessage, callback, 0);
    }

    private void sendMessageWithRetry(String userMessage, GeminiCallback callback, int retryCount) {
        if (!isInitialized()) {
            callback.onError("AI 서비스가 초기화되지 않았습니다. 인터넷 연결을 확인해주세요.");
            return;
        }

        if (chat == null) {
            callback.onError("대화가 시작되지 않았습니다. 다시 시도해주세요.");
            return;
        }

        // 사용자 메시지를 user role로 생성
        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(userMessage);
        Content content = contentBuilder.build();

        Log.d(TAG, "Sending user message: " + userMessage);
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(content);

        // 타임아웃 적용 (재사용되는 스케줄러 사용)
        ListenableFuture<GenerateContentResponse> timeoutFuture = Futures.withTimeout(
                response, TIMEOUT_SECONDS, TimeUnit.SECONDS, timeoutScheduler
        );

        Futures.addCallback(timeoutFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null && !text.isEmpty()) {
                    callback.onSuccess(text.trim());
                } else {
                    callback.onError("AI 응답이 비어있습니다.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error getting AI response (attempt " + (retryCount + 1) + ")", t);

                // 재시도 가능 여부 확인
                if (retryCount < MAX_RETRIES && isRetryableError(t)) {
                    Log.d(TAG, "Retrying... attempt " + (retryCount + 2));
                    sendMessageWithRetry(userMessage, callback, retryCount + 1);
                } else {
                    // 사용자 친화적인 에러 메시지
                    String errorMsg = getErrorMessage(t);
                    callback.onError(errorMsg);
                }
            }
        }, executor);
    }

    private boolean isRetryableError(Throwable t) {
        // 타임아웃, 네트워크 오류는 재시도 가능
        return t instanceof TimeoutException ||
               t.getMessage() != null && (
                   t.getMessage().contains("timeout") ||
                   t.getMessage().contains("network") ||
                   t.getMessage().contains("connection")
               );
    }

    private String getErrorMessage(Throwable t) {
        if (t instanceof TimeoutException) {
            return "AI 응답 시간이 초과되었습니다. 인터넷 연결을 확인해주세요.";
        } else if (t.getMessage() != null) {
            String msg = t.getMessage().toLowerCase();
            if (msg.contains("api key") || msg.contains("apikey")) {
                return "API 키가 유효하지 않습니다.";
            } else if (msg.contains("quota") || msg.contains("limit")) {
                return "API 사용량 한도에 도달했습니다. 잠시 후 다시 시도해주세요.";
            } else if (msg.contains("network") || msg.contains("connection")) {
                return "네트워크 연결 오류입니다. 인터넷을 확인해주세요.";
            }
        }
        return "AI 응답을 받지 못했습니다. 다시 시도해주세요.";
    }

    public void analyzeGrammar(String userText, GeminiCallback callback) {
        String grammarPrompt = "Analyze this English sentence for grammar errors. " +
                "If there are errors, list them briefly. If it's correct, say 'Good job!'\n\n" +
                "Sentence: \"" + userText + "\"\n\n" +
                "Response format:\n" +
                "- If correct: 'Good job! Your grammar is correct.'\n" +
                "- If errors: List 1-2 main errors only, very briefly.";

        // String을 Content로 변환
        Content content = new Content.Builder()
                .addText(grammarPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null && !text.isEmpty()) {
                    callback.onSuccess(text.trim());
                } else {
                    callback.onError("Empty grammar analysis");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error analyzing grammar", t);
                callback.onError("Failed to analyze grammar: " + t.getMessage());
            }
        }, executor);
    }

    public void getVocabularySuggestions(String userText, GeminiCallback callback) {
        String vocabPrompt = "Suggest 2-3 alternative words or phrases to make this sentence sound more natural or advanced:\n\n" +
                "\"" + userText + "\"\n\n" +
                "Keep suggestions brief and practical.";

        // String을 Content로 변환
        Content content = new Content.Builder()
                .addText(vocabPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null && !text.isEmpty()) {
                    callback.onSuccess(text.trim());
                } else {
                    callback.onError("Empty vocabulary suggestions");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error getting vocabulary suggestions", t);
                callback.onError("Failed to get suggestions: " + t.getMessage());
            }
        }, executor);
    }

    public void generateText(String prompt, GeminiCallback callback) {
        generateTextWithRetry(prompt, callback, 0);
    }

    private void generateTextWithRetry(String prompt, GeminiCallback callback, int retryCount) {
        if (!isInitialized()) {
            callback.onError("AI 서비스가 초기화되지 않았습니다. 인터넷 연결을 확인해주세요.");
            return;
        }

        // String을 Content로 변환
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        // 타임아웃 적용 (재사용되는 스케줄러 사용)
        ListenableFuture<GenerateContentResponse> timeoutFuture = Futures.withTimeout(
                response, TIMEOUT_SECONDS, TimeUnit.SECONDS, timeoutScheduler
        );

        Futures.addCallback(timeoutFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                if (text != null && !text.isEmpty()) {
                    callback.onSuccess(text.trim());
                } else {
                    callback.onError("AI 응답이 비어있습니다.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error generating text (attempt " + (retryCount + 1) + ")", t);

                // 재시도 가능 여부 확인
                if (retryCount < MAX_RETRIES && isRetryableError(t)) {
                    Log.d(TAG, "Retrying generateText... attempt " + (retryCount + 2));
                    generateTextWithRetry(prompt, callback, retryCount + 1);
                } else {
                    String errorMsg = getErrorMessage(t);
                    callback.onError(errorMsg);
                }
            }
        }, executor);
    }

    public void shutdown() {
        // ExecutorService 적절히 종료
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            executor = null;
        }

        // ScheduledExecutorService 적절히 종료
        if (timeoutScheduler != null) {
            timeoutScheduler.shutdown();
            try {
                if (!timeoutScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    timeoutScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutScheduler.shutdownNow();
            }
            timeoutScheduler = null;
        }

        chat = null;
        if (history != null) {
            history.clear();
        }
        isInitialized = false;
        Log.d(TAG, "GeminiService shutdown completed");
    }
}
