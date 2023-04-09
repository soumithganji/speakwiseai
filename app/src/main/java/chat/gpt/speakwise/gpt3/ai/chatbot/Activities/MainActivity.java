package chat.gpt.speakwise.gpt3.ai.chatbot.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    String supportEmail = "speakwiseai@gmail.com";
    String appPlayStoreLink;
    Common common = Common.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.primary_black));
        }

        appPlayStoreLink = "https://play.google.com/store/apps/details?id=" + getPackageName();

        binding.rlNewChat.setOnClickListener(v -> initNewChat());

        binding.rlRateApp.setOnClickListener(v -> initRateApp());

        binding.rlFeedback.setOnClickListener(v -> initFeedBack());

        binding.rlShareApp.setOnClickListener(v -> initShareApp());

        if (!common.hasInternetConnection(this)) {
            Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        //see if the particular release is mandatory or not from backend
        checkAppUpdate();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.app_update_dialog, null);
        builder.setView(customView);

        customView.findViewById(R.id.cwUpdateNow).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appPlayStoreLink));
            startActivity(browserIntent);
        });

//        builder.setCancelable(false);

        builder.show();
    }

    private void initNewChat() {
        Intent intent = new Intent(this, ChatActivity.class);
//        intent.putExtra("timeStamp", "1681042693773");
        startActivity(intent);
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