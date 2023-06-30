package chat.gpt.speakwise.gpt3.ai.chatbot.app.Activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils.Common;

public class BaseActivity extends AppCompatActivity {

    Context context;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        context = newBase;
        applyOverrideConfiguration(new Configuration());
        super.attachBaseContext(newBase);
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (context == null) return;
        Common common = new Common();
        Locale locale = new Locale(common.getLang(context));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            overrideConfiguration.setLocale(locale);
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }
}
