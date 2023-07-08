package chat.gpt.speakwise.gpt3.ai.chatbot.app.Adapters;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import chat.gpt.speakwise.gpt3.ai.chatbot.app.Models.Message;
import chat.gpt.speakwise.gpt3.ai.chatbot.R;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    List<Message> messageList;
    Activity activity;

    public MessageAdapter(Activity activity, List<Message> messageList) {
        this.activity = activity;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View chatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, null);
        return new MyViewHolder(chatView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Message message;
        try {
            message = messageList.get(position);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            activity.finish();
            return;
        }
        if (message.getSentBy().equals(Message.SENT_BY_ME)) {
            holder.botChatView.setVisibility(View.GONE);
            holder.userChatView.setVisibility(View.VISIBLE);
            holder.userTextView.setText(message.getMessage());
        } else {
            holder.userChatView.setVisibility(View.GONE);
            holder.botChatView.setVisibility(View.VISIBLE);
            holder.botTextView.setText(message.getMessage());
        }

        if (message.showCopyOptions()) {
            holder.llCopy.setVisibility(View.VISIBLE);
        } else {
            holder.llCopy.setVisibility(View.GONE);
        }

        Message finalMessage = message;
        holder.imCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", finalMessage.getMessage());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(activity, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
//            startAppAd.showAd();
        });

        holder.imShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, finalMessage.getMessage());
            activity.startActivity(Intent.createChooser(intent, "Share via"));
//            startAppAd.showAd();
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout botChatView, userChatView;
        LinearLayout llCopy;
        TextView botTextView, userTextView;
        ImageView imCopy, imShare, imLogo;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            botChatView = itemView.findViewById(R.id.bot_chat_view);
            userChatView = itemView.findViewById(R.id.user_chat_view);
            botTextView = itemView.findViewById(R.id.bot_chat_text_view);
            userTextView = itemView.findViewById(R.id.user_chat_text_view);
            imCopy = itemView.findViewById(R.id.imCopy);
            imShare = itemView.findViewById(R.id.imShare);
            llCopy = itemView.findViewById(R.id.llCopy);
            imLogo = itemView.findViewById(R.id.imLogo);
        }
    }
}
