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

import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
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
    String supportEmail = "chat.speakwiseai@gmail.com";
    String appPlayStoreLink;
    Common common = Common.getInstance();

    private static final String AD_MANAGER_AD_UNIT_ID = "/6499/example/native";
    private NativeAd nativeAd;

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

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);
        ChatAdapter chatAdapter = new ChatAdapter(this, list, chatTimeStamp -> showDeleteChatDialog(chatTimeStamp));
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
            common.deleteChat(MainActivity.this, chatTimeStamp, () -> initChatRecyclerView());
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
        String message = "Check out SpeakWise AI- a free AI chatbot app powered by GPT-3!" +
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
        AdLoader.Builder builder = new AdLoader.Builder(this, AD_MANAGER_AD_UNIT_ID);

        builder.forNativeAd(nativeAd -> {
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
            boolean isDestroyed;
            isDestroyed = isDestroyed();
            if (isDestroyed || isFinishing() || isChangingConfigurations()) {
                nativeAd.destroy();
                return;
            }
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            if (MainActivity.this.nativeAd != null) {
                MainActivity.this.nativeAd.destroy();
            }
            MainActivity.this.nativeAd = nativeAd;
            FrameLayout frameLayout = findViewById(R.id.fl_adplaceholder);
            NativeAdView adView =
                    (NativeAdView)
                            getLayoutInflater().inflate(R.layout.ad_unified, frameLayout, false);
            populateNativeAdView(nativeAd, adView);
            frameLayout.removeAllViews();
            frameLayout.addView(adView);
        });

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {

            }
        }).build();
        adLoader.loadAd(new AdManagerAdRequest.Builder().build());

    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
        adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.GONE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.GONE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.GONE);
        } else {
            ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Updates the UI to say whether or not this ad has a video asset.
        if (nativeAd.getMediaContent() != null && nativeAd.getMediaContent().hasVideoContent()) {
            VideoController vc = nativeAd.getMediaContent().getVideoController();
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    super.onVideoEnd();
                }
            });
        }
    }
}