
package com.example.easychatgpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.easychatgpt.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

//        init();

        //while loading second time(i.e chat instances, do not load messages with something went wrong)


//        messageList.add(new Message("Hi", Message.SENT_BY_ME));
        messageList.add(new Message("Hi! How can I help you!", Message.SENT_BY_BOT));

        messageAdapter = new MessageAdapter(messageList);
        binding.recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);

        binding.sendBtn.setOnClickListener((v) -> {
            String question = binding.messageEditText.getText().toString().trim();
            addToChat(question, Message.SENT_BY_ME);
            binding.messageEditText.setText("");
            callAPI(question);
            binding.welcomeText.setVisibility(View.GONE);
        });
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            binding.recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question) {

        HashMap<String, String> map = new HashMap<>();
        map.put("content", question);
        HashMap<String, String>[] myArray = new HashMap[1];
        myArray[0] = map;

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
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT));

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("abc", e.toString());
                Log.d("abc", e.getMessage());
                addResponse("Something went wrong, please try again later.");
                binding.bottomLayout.setVisibility(View.GONE);
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
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("abc", response.message().toString());
                    Log.d("abc", response.body().string());
                    addResponse("Something went wrong, please try again later.");
                    binding.bottomLayout.setVisibility(View.GONE);
                }
            }
        });


    }

    void init() {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer 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")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        "{\n" +
                                "  \"model\": \"gpt-3.5-turbo\",\n" +
                                "  \"messages\": [{\"role\": \"user\", \"content\": \"Can u write code!\"}],\n" +
                                "  \"temperature\": 0.7\n" +
                                "}"
                ))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the failure
                Log.d("abc", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Handle the response
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // Do something with the response body
                    Log.d("abc", responseBody.toString());

                } else {
                    // Handle the error response
                    Log.d("abc", response.message());
                }
            }
        });

    }


}
