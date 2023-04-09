package chat.gpt.speakwise.gpt3.ai.chatbot.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import chat.gpt.speakwise.gpt3.ai.chatbot.R;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityChatBinding;
import chat.gpt.speakwise.gpt3.ai.chatbot.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}