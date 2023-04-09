package chat.gpt.speakwise.gpt3.ai.chatbot.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    String supportEmail = "speakwiseai@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.primary_black));
        }

        binding.rlRateApp.setOnClickListener(v -> initRateApp());

        binding.rlFeedback.setOnClickListener(v -> initFeedBack());

        binding.rlShareApp.setOnClickListener(v -> initShareApp());

    }

    private void initRateApp() {
        Uri webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
        Intent webRateIntent = new Intent(Intent.ACTION_VIEW, webUri);
        startActivity(webRateIntent);
    }

    private void initShareApp() {
        String message = "Check out SpeakWise - a free AI chatbot app powered by GPT-3!" +
                " Perfect for interesting conversations on any topic." +
                " Download it for free on the Google Play Store and share with your friends!\n" +
                "https://play.google.com/store/apps/details?id=" + getPackageName();

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