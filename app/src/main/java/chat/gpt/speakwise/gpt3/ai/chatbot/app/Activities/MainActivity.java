package chat.gpt.speakwise.gpt3.ai.chatbot.app.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import chat.gpt.speakwise.gpt3.ai.chatbot.app.Adapters.ChatAdapter;
import chat.gpt.speakwise.gpt3.ai.chatbot.BuildConfig;
import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.LinearLayoutManagerWrapper;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityMainBinding;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.LanguageChangeDialogBinding;

public class MainActivity extends BaseActivity {
    ActivityMainBinding binding;
    String supportEmail = "chat.speakwiseai@gmail.com";
    String appPlayStoreLink;
    Common common = Common.getInstance();
    StartAppAd startAppAd = new StartAppAd(this);
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

        loadAd();

        FirebaseApp.initializeApp(this);
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);

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

        initGooglePlayAppReviewDialog();
        (new Handler()).postDelayed(() -> startAppAd.showAd(), 1000);
    }

    private void initGooglePlayAppReviewDialog() {
        common.incrementAppOpenCount(this);
        if (common.getAppOpenCount(this) % 5 != 0) return;

        ReviewManager reviewManager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task1 -> {

                });
            }
        });
    }

    private void loadAd() {
        Handler handler = new Handler();
        handler.postDelayed(this::loadAd, 30000);

        loadNativeAd();
    }

    private void showBlockedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
        builder.setView(customView);

        ((TextView) customView.findViewById(R.id.dialog_title)).setText("Oops!");

        ((TextView) customView.findViewById(R.id.dialog_message)).setText("We're temporarily down. Hold tight! We will be back as soon as we can.");

        customView.findViewById(R.id.llBottom).setVisibility(View.GONE);

        builder.setCancelable(false);

        builder.show();
    }

    private void init(HashMap<String, String> manditoryUpdateMap) {
        appPlayStoreLink = "https://play.google.com/store/apps/details?id=" + getPackageName();

        binding.cwBuyMeACoffee.setOnClickListener(v -> initBuyMeACoffee());

        binding.cwLanguage.setOnClickListener(v -> initLanguageChange());

        binding.rlNewChat.setOnClickListener(v -> initNewChat());

        binding.rlRateApp.setOnClickListener(v -> initRateApp());

        binding.rlFeedback.setOnClickListener(v -> initFeedBack());

        binding.rlShareApp.setOnClickListener(v -> initShareApp());

        binding.rlClearChats.setOnClickListener(v -> showDeleteAllChatsDialog());

        initChatRecyclerView();

        if (!common.hasInternetConnection(this)) {
            Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        checkAppUpdate(manditoryUpdateMap);
    }

    private void initLanguageChange() {
        Dialog dialog = new Dialog(this);
        LanguageChangeDialogBinding binding = LanguageChangeDialogBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        dialog.show();

        String lang = common.getLang(getApplicationContext());
        switch (lang) {
            case "en":
                binding.rbEnglish.setChecked(true);
                break;
            case "zh":
                binding.rbChinese.setChecked(true);
        }

        binding.rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.rbEnglish:
                    common.saveLang(context, "en");
                    break;
                case R.id.rbChinese:
                    common.saveLang(context, "zh");
            }

            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(getPackageName());
            finishAffinity();
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

    }

    private void initBuyMeACoffee() {
        String buyMeACoffee = "https://www.buymeacoffee.com/speakwiseai";
        Uri webUri = Uri.parse(buyMeACoffee);
        Intent intent = new Intent(Intent.ACTION_VIEW, webUri);
        startActivity(intent);
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("buy_me_a_coffee_clicked", null);
    }

    private void initChatRecyclerView() {
        SharedPreferences prefs = getSharedPreferences("speakwise", MODE_PRIVATE);
        String dateString = prefs.getString("list", "");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();

        ArrayList<String> list = gson.fromJson(dateString, type);

        if (list == null) list = new ArrayList<>();

        Collections.sort(list, (timestamp1, timestamp2) -> {
            long t1 = Long.parseLong(timestamp1);
            long t2 = Long.parseLong(timestamp2);
            return Long.compare(t2, t1);
        });

        LinearLayoutManager llm = new LinearLayoutManagerWrapper(this, LinearLayoutManager.VERTICAL, false);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);
        ChatAdapter chatAdapter = new ChatAdapter(this, list, this::showDeleteChatDialog);
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerView.setAdapter(chatAdapter);
    }

    private void showDeleteChatDialog(String chatTimeStamp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
        builder.setView(customView);

        AlertDialog dialog = builder.create();

        ((TextView) customView.findViewById(R.id.dialog_title)).setText("Clear Conversation");
        ((TextView) customView.findViewById(R.id.tvYes)).setText("Yes");
        ((TextView) customView.findViewById(R.id.dialog_message)).setText("Are you sure you want to clear the conversation?");

        customView.findViewById(R.id.cwYes).setOnClickListener(v -> {
            common.deleteChat(MainActivity.this, chatTimeStamp, this::initChatRecyclerView);
            dialog.dismiss();
        });

        dialog.show();
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
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
            builder.setView(customView);

            customView.findViewById(R.id.cwYes).setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appPlayStoreLink));
                startActivity(browserIntent);
            });

            if (manditoryUpdateMap.containsValue(String.valueOf(BuildConfig.VERSION_CODE))) {
                builder.setCancelable(false);
            }

            builder.show();
        } catch (Exception ignored) {

        }
    }

    private void initNewChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        activityResultLauncher.launch(intent);
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("new_chat_clicked", null);
    }

    private void initRateApp() {
        Uri webUri = Uri.parse(appPlayStoreLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, webUri);
        startActivity(intent);
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("rate_app_clicked", null);
    }

    private void initShareApp() {
        String message = "Check out SpeakWise AI- a free AI chatbot app powered by GPT-3!" +
                " Perfect for interesting conversations on any topic." +
                " Download it for free on the Google Play Store and share with your friends!\n" +
                appPlayStoreLink;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, "Share via"));
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("share_app_clicked", null);
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
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("provide_feedback_clicked", null);
    }

    private void showDeleteAllChatsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
        builder.setView(customView);

        AlertDialog dialog = builder.create();

        ((TextView) customView.findViewById(R.id.dialog_title)).setText("Clear All Conversations");
        ((TextView) customView.findViewById(R.id.tvYes)).setText("Yes");
        ((TextView) customView.findViewById(R.id.dialog_message)).setText("Are you sure you want to clear all conversations?");

        customView.findViewById(R.id.cwYes).setOnClickListener(v -> common.deleteAllChats(this, () -> {
            initChatRecyclerView();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void loadNativeAd() {
        StartAppNativeAd startAppNativeAd = new StartAppNativeAd(this);

        NativeAdPreferences nativePrefs = new NativeAdPreferences()
                .setAdsNumber(1)
                .setAutoBitmapDownload(true)
                .setPrimaryImageSize(2);
        AdEventListener adListener = new AdEventListener() {
            @Override
            public void onReceiveAd(Ad arg0) {
                ArrayList<NativeAdDetails> ads = startAppNativeAd.getNativeAds();

                if (ads.isEmpty()) {
                    loadNativeAd();
                    return;
                }

                binding.adLayout.adView.setVisibility(View.VISIBLE);

                NativeAdDetails nativeAdDetails = ads.get(0);

                binding.adLayout.adHeadline.setText(nativeAdDetails.getTitle());
                binding.adLayout.adBody.setText(nativeAdDetails.getDescription());
                binding.adLayout.adAppIcon.setImageBitmap(nativeAdDetails.getImageBitmap());
                binding.adLayout.adCallToAction.setText(nativeAdDetails.isApp() ? "Install" : "Open");

                nativeAdDetails.registerViewForInteraction(binding.adLayout.adView);
                nativeAdDetails.registerViewForInteraction(binding.adLayout.adCallToAction);
            }

            @Override
            public void onFailedToReceiveAd(Ad arg0) {
                loadNativeAd();
            }
        };

        startAppNativeAd.loadAd(nativePrefs, adListener);
    }
}