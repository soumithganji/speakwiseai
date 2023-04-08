
package com.example.easychatgpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.easychatgpt.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    List<Message> messageList;
    MessageAdapter messageAdapter;
    ActivityMainBinding binding;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        messageList = new ArrayList<>();

        client = new OkHttpClient().newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();


        //while loading second time(i.e chat instances, do not load messages with something went wrong)


//        messageList.add(new Message("Hi! How can I help you!", Message.SENT_BY_BOT, false));

        messageAdapter = new MessageAdapter(this, messageList);
        binding.recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);

        binding.sendBtn.setOnClickListener((v) -> {
            String question = binding.messageEditText.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Input cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            addToChat(question, Message.SENT_BY_ME);
            binding.messageEditText.setText("");
            callAPI();
            binding.welcomeText.setVisibility(View.GONE);
        });
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            if (sentBy == Message.SENT_BY_ME || message.equals("Something went wrong, please try again later.")) {
                messageList.add(new Message(message.trim(), sentBy, false));
            } else {
                messageList.add(new Message(message.trim(), sentBy, true));
            }
            messageAdapter.notifyDataSetChanged();
            binding.recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI() {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");

            JSONArray messages = new JSONArray();

            for (Message m : messageList) {
                JSONObject message = new JSONObject();
                message.put("role", m.getSentBy());
                message.put("content", m.getMessage());
                messages.put(message);
            }

            jsonBody.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT, false));

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.OPENAIKEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("abc", e.toString());
                Log.d("abc", e.getMessage());
                addResponse("Something went wrong, please try again later.");
                runOnUiThread(() -> binding.bottomLayout.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        JSONObject message = jsonArray.getJSONObject(0).getJSONObject("message");
                        String result = message.getString("content");
                        addResponse(result.trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("abc", response.message());
                    Log.d("abc", response.body().string());
                    addResponse("Something went wrong, please try again later.");
                    runOnUiThread(() -> binding.bottomLayout.setVisibility(View.GONE));
                }
            }
        });

    }
}
