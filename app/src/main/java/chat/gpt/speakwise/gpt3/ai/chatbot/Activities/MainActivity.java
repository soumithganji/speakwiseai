package chat.gpt.speakwise.gpt3.ai.chatbot.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import chat.gpt.speakwise.gpt3.ai.chatbot.Adapters.ChatAdapter;
import chat.gpt.speakwise.gpt3.ai.chatbot.BuildConfig;
import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    String supportEmail = "speakwiseai@gmail.com";
    String appPlayStoreLink;
    Common common = Common.getInstance();

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    boolean isChanged = data.getBooleanExtra("isChanged", false);
                    if (isChanged) {
                        initChatRecyclerView();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.primary_black));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("config").document("speakwiseai");

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            binding.spinKit.setVisibility(View.GONE);
            if (documentSnapshot.exists()) {
                boolean block_free_users = documentSnapshot.getBoolean("block_free_users");

                if (block_free_users) {
                    showBlockedDialog();
                    return;
                }

                try {
                    long free_max_tokens = documentSnapshot.getLong("free_max_tokens");
                    common.setFree_max_tokens(free_max_tokens);
                } catch (Exception e) {

                }
                try {
                    boolean free_unlimited_tokens = documentSnapshot.getBoolean("free_unlimited_tokens");
                    common.setFree_unlimited_tokens(free_unlimited_tokens);
                } catch (Exception e) {

                }
                try {
                    double temperature = documentSnapshot.getDouble("temperature");
                    common.setTemperature(temperature);
                } catch (Exception e) {

                }

                HashMap<String, String> manditoryUpdateMap = (HashMap<String, String>) documentSnapshot.get("manditoryUpdatesMap");
                init(manditoryUpdateMap);
            } else {
                Toast.makeText(getApplicationContext(), "Something Went Wrong. Please Try Again Later", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            binding.spinKit.setVisibility(View.GONE);
            if (!common.hasInternetConnection(this)) {
                Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Something Went Wrong. Please Try Again Later", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBlockedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
        builder.setView(customView);

        ((TextView)customView.findViewById(R.id.dialog_title)).setText("Oops!");

        ((TextView)customView.findViewById(R.id.dialog_message)).setText("We're temporarily down. Hold tight! We will be back as soon as we can.");

        customView.findViewById(R.id.llBottom).setVisibility(View.GONE);

        builder.setCancelable(false);

        builder.show();
    }

    private void init(HashMap<String, String> manditoryUpdateMap) {
        appPlayStoreLink = "https://play.google.com/store/apps/details?id=" + getPackageName();

        binding.rlNewChat.setOnClickListener(v -> initNewChat());

        binding.rlRateApp.setOnClickListener(v -> initRateApp());

        binding.rlFeedback.setOnClickListener(v -> initFeedBack());

        binding.rlShareApp.setOnClickListener(v -> initShareApp());

        initChatRecyclerView();

        if (!common.hasInternetConnection(this)) {
            Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        checkAppUpdate(manditoryUpdateMap);
    }

    private void initChatRecyclerView() {
        SharedPreferences prefs = getSharedPreferences("speakwise", MODE_PRIVATE);
        String dateString = prefs.getString("list", "");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();

        ArrayList<String> list = gson.fromJson(dateString, type);

        if (list == null) return;

        Collections.sort(list, (timestamp1, timestamp2) -> {
            long t1 = Long.parseLong(timestamp1);
            long t2 = Long.parseLong(timestamp2);
            return Long.compare(t2, t1);
        });

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);
        ChatAdapter chatAdapter = new ChatAdapter(this, list);
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerView.setAdapter(chatAdapter);
    }

    private void checkAppUpdate(HashMap<String, String> manditoryUpdateMap) {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                showUpdateDialog(manditoryUpdateMap);
            }
        });
    }

    private void showUpdateDialog(HashMap<String, String> manditoryUpdateMap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
        builder.setView(customView);

        customView.findViewById(R.id.cwUpdateNow).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appPlayStoreLink));
            startActivity(browserIntent);
        });

        if (manditoryUpdateMap.containsValue(String.valueOf(BuildConfig.VERSION_CODE))) {
            builder.setCancelable(false);
        }

        builder.show();
    }

    private void initNewChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        activityResultLauncher.launch(intent);
    }

    private void initRateApp() {
        Uri webUri = Uri.parse(appPlayStoreLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, webUri);
        startActivity(intent);
    }

    private void initShareApp() {
        String message = "Check out SpeakWise - a free AI chatbot app powered by GPT-3!" +
                " Perfect for interesting conversations on any topic." +
                " Download it for free on the Google Play Store and share with your friends!\n" +
                appPlayStoreLink;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void initFeedBack() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", supportEmail, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Email subject");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Email body");
            startActivity(Intent.createChooser(emailIntent, "Send email"));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Contact " + supportEmail, Toast.LENGTH_SHORT).show();
        }
    }
}