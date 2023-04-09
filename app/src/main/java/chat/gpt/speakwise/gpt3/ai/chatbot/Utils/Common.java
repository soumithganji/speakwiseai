package chat.gpt.speakwise.gpt3.ai.chatbot.Utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import chat.gpt.speakwise.gpt3.ai.chatbot.Models.Message;

public class Common {
    private static Common single_instance = null;

    public static Common getInstance() {
        if (single_instance == null)
            single_instance = new Common();

        return single_instance;
    }

    public boolean hasInternetConnection(Activity activity) {
        boolean isOnline = false;
        ConnectivityManager manager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
                isOnline = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } else {
                NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
                isOnline = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOnline;
    }

    public String getChats(Activity activity, String timeStamp) {
        SharedPreferences prefs = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        return prefs.getString("chats_" + timeStamp, "");
    }

    public void saveChats(Activity activity, String chats, String timeStamp) {
        SharedPreferences.Editor editor = activity.getSharedPreferences("speakwise", MODE_PRIVATE).edit();
        editor.putString("chats_" + timeStamp, chats);
        editor.apply();
    }

    public String convertObjectListToString(List<Message> objectList) {
        Gson gson = new Gson();
        return gson.toJson(objectList);
    }

    public ArrayList<Message> convertStringToObjectList(String jsonString) {
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Message>>() {
        }.getType();
        return gson.fromJson(jsonString, type);
    }


    public void saveTimeStamp(Activity activity, String date) {
        SharedPreferences prefs = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        String dateString = prefs.getString("list", "");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {}.getType();

        ArrayList<String> list = gson.fromJson(dateString, type);
        if(list == null) list = new ArrayList<>();
        list.add(date);

        SharedPreferences.Editor editor = activity.getSharedPreferences("speakwise", MODE_PRIVATE).edit();
        editor.putString("list", gson.toJson(list));
        editor.apply();
    }
}