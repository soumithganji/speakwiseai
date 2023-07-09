package chat.gpt.speakwise.gpt3.ai.chatbot.app.Activities;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import com.onesignal.OneSignal;

import chat.gpt.speakwise.gpt3.ai.chatbot.app.Application.MyApplication;
import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.BillingClientLifecycle;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

    ActivitySplashBinding binding;
    Common common = new Common();
    private static final String ONESIGNAL_APP_ID = "6a096862-307e-41c9-b467-937ac9b51987";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.primary_white));
        }

        initOneSignal();

        getPurchases();
    }

    private void initOneSignal() {
        OneSignal.initWithContext(getApplicationContext());
        OneSignal.setAppId(ONESIGNAL_APP_ID);
    }

    private void getPurchases() {
        BillingClientLifecycle billingClientLifecycle = BillingClientLifecycle.getInstance(getApplication());
        getLifecycle().addObserver(billingClientLifecycle);
        billingClientLifecycle.purchases.observe(this, purchases -> {
            common.setUserPaid(!purchases.isEmpty());
            Application application = getApplication();
            if (!(application instanceof MyApplication)) {
                startMainActivity();
                return;
            }
            ((MyApplication) application).showAd(SplashActivity.this, () -> startMainActivity());
        });
    }

    public void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}