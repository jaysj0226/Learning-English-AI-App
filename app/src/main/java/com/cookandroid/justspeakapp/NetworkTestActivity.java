package com.cookandroid.justspeakapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkTestActivity extends AppCompatActivity {
    private static final String TAG = "NetworkTest";
    private TextView tvResults;
    private Button btnTestHttp;
    private Button btnTestAzure;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);

        tvResults = findViewById(R.id.tv_results);
        btnTestHttp = findViewById(R.id.btn_test_http);
        btnTestAzure = findViewById(R.id.btn_test_azure);

        executorService = Executors.newSingleThreadExecutor();

        btnTestHttp.setOnClickListener(v -> testHttpConnection());
        btnTestAzure.setOnClickListener(v -> testAzureConnection());
    }

    private void testHttpConnection() {
        appendResult("\n=== Testing Basic HTTP ===\n");
        executorService.submit(() -> {
            try {
                // Test Google (reliable, fast)
                URL url = new URL("https://www.google.com");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                String result = "✅ Google: " + responseCode + " " + connection.getResponseMessage();
                appendResult(result + "\n");
                connection.disconnect();
                Log.d(TAG, result);
            } catch (Exception e) {
                String error = "❌ Google: " + e.getMessage();
                appendResult(error + "\n");
                Log.e(TAG, "HTTP test failed", e);
            }
        });
    }

    private void testAzureConnection() {
        appendResult("\n=== Testing Azure Endpoints ===\n");
        executorService.submit(() -> {
            // Test Azure Speech endpoint
            testEndpoint("https://southeastasia.api.cognitive.microsoft.com/", "Azure Speech API");

            // Test Azure Speech STT WebSocket endpoint (HTTP equivalent)
            testEndpoint("https://southeastasia.stt.speech.microsoft.com/", "Azure STT Endpoint");
        });
    }

    private void testEndpoint(String urlString, String name) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            String result = "✅ " + name + ": " + responseCode + " " + connection.getResponseMessage();
            appendResult(result + "\n");
            connection.disconnect();
            Log.d(TAG, result);
        } catch (java.net.UnknownHostException e) {
            String error = "❌ " + name + ": DNS resolution failed - No internet or DNS issue";
            appendResult(error + "\n");
            Log.e(TAG, error, e);
        } catch (java.net.SocketTimeoutException e) {
            String error = "❌ " + name + ": Timeout - Network too slow or blocked";
            appendResult(error + "\n");
            Log.e(TAG, error, e);
        } catch (Exception e) {
            String error = "❌ " + name + ": " + e.getMessage();
            appendResult(error + "\n");
            Log.e(TAG, name + " test failed", e);
        }
    }

    private void appendResult(String text) {
        runOnUiThread(() -> {
            String current = tvResults.getText().toString();
            tvResults.setText(current + text);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
