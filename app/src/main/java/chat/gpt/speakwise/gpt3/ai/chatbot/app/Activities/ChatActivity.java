package chat.gpt.speakwise.gpt3.ai.chatbot.app.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Models.Message;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Adapters.MessageAdapter;
import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityChatBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends BaseActivity {
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
    InterstitialAd interstitialAd;
    private static final String AD_UNIT_ID_FULLSCREEN = "ca-app-pub-4125120108950748/6526406286";

    private Handler handlerFullScreen;
    private Handler handlerBanner;
//    private StartAppAd startAppAd = new StartAppAd(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= android.os.Build.VERSION_CODES.P) {
            overridePendingTransition(R.anim.enter, R.anim.exit);
        }
        setContentView(binding.getRoot());

        handlerFullScreen = new Handler();
        handlerBanner = new Handler();

//        loadFullScreenAd();
        initBannerChat();

        timeStamp = getIntent().getStringExtra("timeStamp");
        if (timeStamp == null) timeStamp = "";

        if (!common.hasInternetConnection(this)) {
            Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            initChatRecyclerView();
            return;
        }

        if (timeStamp.isEmpty()) {
            Common.showKeyboard(this, binding.messageEditText);
        }

        client = new OkHttpClient().newBuilder()
                .readTimeout(50, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .build();


        initChatRecyclerView();

        binding.btnVoice.setOnClickListener(v -> initTextToSpeech());


        if (getIntent().getBooleanExtra("fromWidget", false)) {
            FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("widget_clicked", null);
            checkAppUpdate();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("config").document("speakwiseai");

            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    boolean block_free_users = documentSnapshot.getBoolean("block_free_users");
                    boolean temp_unblock_all = true;

                    try {
                        temp_unblock_all = documentSnapshot.getBoolean("temp_unblock_all");
                    } catch (Exception e) {

                    }
                    if (!temp_unblock_all) {
                        if (block_free_users) {
                            common.showBlockedDialog(ChatActivity.this);
                            return;
                        }
                    }

                    try {
                        long free_max_tokens = documentSnapshot.getLong("free_max_tokens");
                        common.setFree_max_tokens(free_max_tokens);
                    } catch (Exception ignored) {

                    }
                    try {
                        boolean free_unlimited_tokens = documentSnapshot.getBoolean("free_unlimited_tokens");
                        common.setFree_unlimited_tokens(free_unlimited_tokens);
                    } catch (Exception ignored) {

                    }
                    try {
                        double temperature = documentSnapshot.getDouble("temperature");
                        common.setTemperature(temperature);
                    } catch (Exception ignored) {

                    }
                    try {
                        String key = documentSnapshot.getString("key");
                        common.setKey(common.decrypt(BuildConfig.SECRET, key));
                    } catch (Exception ignored) {

                    }

                    binding.sendBtn.setOnClickListener((v) -> sendMessage());
                } else {
                    Toast.makeText(getApplicationContext(), "Something Went Wrong. Please Try Again Later", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                if (!common.hasInternetConnection(this)) {
                    Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Something Went Wrong. Please Try Again Later", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            binding.sendBtn.setOnClickListener((v) -> sendMessage());
        }

//        (new Handler()).postDelayed(() -> startAppAd.showAd(), 1000);
    }

    private void checkAppUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                showUpdateDialog();
            }
        });
    }

    private void showUpdateDialog() {
        String appPlayStoreLink = "https://play.google.com/store/apps/details?id=" + getPackageName();
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
            builder.setView(customView);

            customView.findViewById(R.id.cwYes).setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appPlayStoreLink));
                startActivity(browserIntent);
            });

            builder.setCancelable(false);

            builder.show();
        } catch (Exception ignored) {

        }
    }

    private void loadFullScreenAd() {
        handlerFullScreen.postDelayed(() -> {
            loadInterstitialAd();
            loadFullScreenAd();
        }, 40000);
    }

    private void initBannerChat() {
//        binding.adView.loadAd();
//        AdRequest adRequest = new AdRequest.Builder().build();
//        binding.adView.loadAd(adRequest);
//        handlerBanner.postDelayed(this::initBannerChat, 30000);
    }

    private void initChatRecyclerView() {
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);
        binding.recyclerView.setItemAnimator(null);
        messageAdapter = new MessageAdapter(this, messageList);
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

        if (!common.isUserPaid() && common.getResponseCount(this) >= 5) {
            common.showSubscriptionPage(this);
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

        loadInterstitialAd();
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
            if (sentBy == Message.SENT_BY_ME || isErrorMessage(message)) {
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

        if (!isErrorMessage(response)) {
            List<Message> tempList = new ArrayList<>(messageList);
            tempList.add(new Message(response, Message.SENT_BY_BOT, true));

            String date = "" + (new Date()).getTime();
            if (!timeStamp.isEmpty()) {
                date = timeStamp;
            } else {
                timeStamp = date;
                common.saveTimeStamp(this, date, messageList.get(0).getMessage());
            }
            common.saveChats(this, common.convertObjectListToString(tempList), date);
            common.increaseResponseCount(this);
        }
        addToChat(response, Message.SENT_BY_BOT);
    }

    private boolean isErrorMessage(String message) {
        return (message.equals("Something went wrong, please try again later.")
                || message.equals("The maximum chat length is reached, please start a new chat."));
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
            if (!common.isUserPaid()) {
                if (!common.isFree_unlimited_tokens()) {
                    jsonBody.put("max_tokens", common.getFree_max_tokens());
                }
            }
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }

        messageList.add(new Message("Typing...", Message.SENT_BY_BOT, false));

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + common.getKey())
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
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
                        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("response_received", null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String responseBody = response.body().string();
                    try {
                        throw new Exception("Api Response Error");
                    } catch (Exception e) {
                        FirebaseCrashlytics.getInstance().log(responseBody);
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                    if (responseBody.contains("tokens")) {
                        addResponse("The maximum chat length is reached, please start a new chat.");
                    } else {
                        addResponse("Something went wrong, please try again later.");
                    }
                    runOnUiThread(() -> binding.bottomLayout.setVisibility(View.GONE));
                }
            }
        });

    }

    private void startSpeechToText() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...");
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadInterstitialAd() {
//        startAppAd.showAd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(null);
        }
        handlerFullScreen.removeCallbacksAndMessages(null);
        handlerBanner.removeCallbacksAndMessages(null);
    }

}
