package chat.gpt.speakwise.gpt3.ai.chatbot.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import chat.gpt.speakwise.gpt3.ai.chatbot.BuildConfig;
import chat.gpt.speakwise.gpt3.ai.chatbot.Models.Message;
import chat.gpt.speakwise.gpt3.ai.chatbot.Adapters.MessageAdapter;
import chat.gpt.speakwise.gpt3.ai.chatbot.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityChatBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    ActivityChatBinding binding;
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private static final int REQUEST_CODE_PERMISSION = 200;
    List<Message> messageList = new ArrayList<>();
    MessageAdapter messageAdapter;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client;
    Common common = Common.getInstance();
    String timeStamp = "";
    boolean isChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initBannerChat();

        if (!common.hasInternetConnection(this)) {
            Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        timeStamp = getIntent().getStringExtra("timeStamp");
        if (timeStamp == null) timeStamp = "";

        client = new OkHttpClient().newBuilder()
                .readTimeout(50, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .build();


        initChatRecyclerView();

        binding.btnVoice.setOnClickListener(v -> initTextToSpeech());

        binding.sendBtn.setOnClickListener((v) -> sendMessage());
    }

    private void initBannerChat() {
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);

        Handler handler = new Handler();
        handler.postDelayed(this::initBannerChat, 30000);
    }

    private void initChatRecyclerView() {
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);
        messageAdapter = new MessageAdapter(this, messageList);
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerView.setAdapter(messageAdapter);

        String chatString = common.getChats(this, timeStamp);
        if (!chatString.isEmpty()) {
            binding.welcomeText.setVisibility(View.GONE);
            ArrayList<Message> tempList = new ArrayList<>(common.convertStringToObjectList(chatString));
            messageList.addAll(tempList);
        }
    }

    private void initTextToSpeech() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_CODE_PERMISSION);
        } else {
            startSpeechToText();
        }
    }

    private void sendMessage() {
        if (!common.hasInternetConnection(this)) {
            Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        String question = binding.messageEditText.getText().toString().trim();
        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessage().equals("Typing...")) {
            return;
        }
        if (question.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Input cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        addToChat(question, Message.SENT_BY_ME);
        binding.messageEditText.setText("");
        callAPI();
        binding.welcomeText.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                binding.messageEditText.setText(result.get(0));
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("isChanged", isChanged);
        setResult(Activity.RESULT_OK, intent);
        finish();
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

        if (!response.equals("Something went wrong, please try again later.")) {
            List<Message> tempList = new ArrayList<>(messageList);
            tempList.add(new Message(response, Message.SENT_BY_BOT, true));

            String date = "" + (new Date()).getTime();
            if (!timeStamp.isEmpty()) {
                date = timeStamp;
            } else {
                common.saveTimeStamp(this, date);
            }
            common.saveChats(this, common.convertObjectListToString(tempList), date);
        }
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
            jsonBody.put("temperature", common.getTemperature());
            jsonBody.put("messages", messages);
            if (!common.isFree_unlimited_tokens()) {
                jsonBody.put("max_tokens", common.getFree_max_tokens());
            }
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().log(e.toString());
            e.printStackTrace();
        }

        messageList.add(new Message("Typing...", Message.SENT_BY_BOT, false));

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.OPENAIKEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                FirebaseCrashlytics.getInstance().log(e.toString());
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
                        isChanged = true;
                        addResponse(result.trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    FirebaseCrashlytics.getInstance().log(response.message());
                    addResponse("Something went wrong, please try again later.");
                    runOnUiThread(() -> binding.bottomLayout.setVisibility(View.GONE));
                }
            }
        });

    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...");
        startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
    }

}
