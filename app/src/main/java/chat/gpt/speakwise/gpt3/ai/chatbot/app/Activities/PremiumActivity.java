package chat.gpt.speakwise.gpt3.ai.chatbot.app.Activities;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.RequiresApi;

import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.CallBacks.SubscriptionSuccessCallBack;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.BillingClientLifecycle;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.Common;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityPremiumBinding;

public class PremiumActivity extends BaseActivity {

    ActivityPremiumBinding binding;

    final String PLAN_WEEKLY = "weekly";
    final String PLAN_MONTHLY = "monthly";
    final String PLAN_YEARLY = "yearly";

    String selectedPlan = PLAN_WEEKLY;

    private BillingClientLifecycle billingClientLifecycle;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPremiumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.primary_black));
        }

        billingClientLifecycle = BillingClientLifecycle.getInstance(getApplication());
        billingClientLifecycle.setOnSubSuccessCallBack((SubscriptionSuccessCallBack) () -> {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(getPackageName());
            finishAffinity();
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        getLifecycle().addObserver(billingClientLifecycle);

        billingClientLifecycle.basicSubProductWithProductDetails.observe(this, productDetails -> {
            binding.llLoading.setVisibility(View.GONE);
            initButtonListeners();
        });

        binding.imClose.setOnClickListener(v -> finish());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initButtonListeners() {
        binding.tvWeekly.setText(billingClientLifecycle.getPlanPrice(Common.BASIC_WEEKLY_PLAN) + " " + getString(R.string.per_week));
        binding.tvMonthly.setText(billingClientLifecycle.getPlanPrice(Common.BASIC_MONTHLY_PLAN) + " " + getString(R.string.per_month));
        binding.tvYearly.setText(billingClientLifecycle.getPlanPrice(Common.BASIC_YEARLY_PLAN) + " " + getString(R.string.per_year));

        binding.cwWeekly.setOnClickListener(v -> {
            binding.cwWeekly.setCardBackgroundColor(getColor(R.color.primary_green));
            binding.cwMonthly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwYearly.setCardBackgroundColor(getColor(R.color.primary_white));

            selectedPlan = PLAN_WEEKLY;
        });

        binding.cwMonthly.setOnClickListener(v -> {
            binding.cwWeekly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwMonthly.setCardBackgroundColor(getColor(R.color.primary_green));
            binding.cwYearly.setCardBackgroundColor(getColor(R.color.primary_white));

            selectedPlan = PLAN_MONTHLY;
        });

        binding.cwYearly.setOnClickListener(v -> {
            binding.cwWeekly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwMonthly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwYearly.setCardBackgroundColor(getColor(R.color.primary_green));

            selectedPlan = PLAN_YEARLY;
        });

        binding.cwContinue.setOnClickListener(v -> chargePlan(selectedPlan));
    }

    private void chargePlan(String selectedPlan) {
        String tag = Common.BASIC_WEEKLY_PLAN;
        switch (selectedPlan) {
            case PLAN_WEEKLY: {
                tag = Common.BASIC_WEEKLY_PLAN;
                break;
            }
            case PLAN_MONTHLY: {
                tag = Common.BASIC_MONTHLY_PLAN;
                break;
            }
            case PLAN_YEARLY: {
                tag = Common.BASIC_YEARLY_PLAN;
                break;
            }
        }
        billingClientLifecycle.buyBasePlans(this, tag);
    }
}