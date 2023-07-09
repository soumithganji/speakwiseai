package chat.gpt.speakwise.gpt3.ai.chatbot.app.Activities;


import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.RequiresApi;

import chat.gpt.speakwise.gpt3.ai.chatbot.R;
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
        getLifecycle().addObserver(billingClientLifecycle);

        billingClientLifecycle.basicSubProductWithProductDetails.observe(this, productDetails -> {
            binding.llLoading.setVisibility(View.GONE);
            initButtonListeners();
        });

        binding.imClose.setOnClickListener(v -> finish());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initButtonListeners() {
        binding.tvWeekly.setText("First 3 days free,\nthen " + billingClientLifecycle.getPlanPrice(Common.BASIC_WEEKLY_PLAN) + " /week");
        binding.tvMonthly.setText(billingClientLifecycle.getPlanPrice(Common.BASIC_MONTHLY_PLAN) + " /month");
        binding.tvYearly.setText(billingClientLifecycle.getPlanPrice(Common.BASIC_YEARLY_PLAN) + " /year");

        binding.cwWeekly.setOnClickListener(v -> {
            binding.cwWeekly.setCardBackgroundColor(getColor(R.color.primary_green));
            binding.cwMonthly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwYearly.setCardBackgroundColor(getColor(R.color.primary_white));

            //show  plan text as '3 days free trial, {price}/week later' if free trial is available for user
            //show 'continue' button text as Start Free Trial Only if free trial is available for user
            binding.tvContinue.setText("Start Free Trial");
            selectedPlan = PLAN_WEEKLY;
        });

        binding.cwMonthly.setOnClickListener(v -> {
            binding.cwWeekly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwMonthly.setCardBackgroundColor(getColor(R.color.primary_green));
            binding.cwYearly.setCardBackgroundColor(getColor(R.color.primary_white));

            binding.tvContinue.setText("Continue");
            selectedPlan = PLAN_MONTHLY;
        });

        binding.cwYearly.setOnClickListener(v -> {
            binding.cwWeekly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwMonthly.setCardBackgroundColor(getColor(R.color.primary_white));
            binding.cwYearly.setCardBackgroundColor(getColor(R.color.primary_green));

            binding.tvContinue.setText("Continue");
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