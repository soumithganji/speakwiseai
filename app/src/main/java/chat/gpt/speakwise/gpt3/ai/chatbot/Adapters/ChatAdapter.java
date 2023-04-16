package chat.gpt.speakwise.gpt3.ai.chatbot.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import chat.gpt.speakwise.gpt3.ai.chatbot.Activities.ChatActivity;
import chat.gpt.speakwise.gpt3.ai.chatbot.CallBacks.DeleteChatCallBack;
import chat.gpt.speakwise.gpt3.ai.chatbot.R;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyViewHolder> {
    List<String> chatList;
    Activity activity;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM hh:mm a");
    DeleteChatCallBack deleteChatCallBack;

    public ChatAdapter(Activity activity, List<String> chatList, DeleteChatCallBack deleteChatCallBack) {
        this.activity = activity;
        this.chatList = chatList;
        this.deleteChatCallBack = deleteChatCallBack;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View chatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, null);
        MyViewHolder myViewHolder = new MyViewHolder(chatView);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String chatTimeStamp = chatList.get(position);
        String formattedTimeStamp = simpleDateFormat.format(new Date(Long.parseLong(chatTimeStamp)));
        holder.tvLabel.setText(formattedTimeStamp);
        holder.llRoot.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ChatActivity.class);
            intent.putExtra("timeStamp", chatTimeStamp);
            activity.startActivity(intent);
        });

        holder.llRoot.setOnLongClickListener(v -> {
            deleteChatCallBack.onDeleteClicked(chatTimeStamp);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel;
        LinearLayout llRoot;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tvLabel);
            llRoot = itemView.findViewById(R.id.llRoot);
        }
    }
}
