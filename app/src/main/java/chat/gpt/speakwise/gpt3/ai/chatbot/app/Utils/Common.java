package chat.gpt.speakwise.gpt3.ai.chatbot.app.Utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import chat.gpt.speakwise.gpt3.ai.chatbot.app.CallBacks.OnPreferencesClearedListener;
import chat.gpt.speakwise.gpt3.ai.chatbot.app.Models.Message;

public class Common {
    private static Common single_instance = null;
    private static long free_max_tokens = 3000;
    private static boolean free_unlimited_tokens = false;
    private static double temperature = 1;

    public static Common getInstance() {
        if (single_instance == null)
            single_instance = new Common();

        return single_instance;
    }

    public long getFree_max_tokens() {
        return free_max_tokens;
    }

    public void setFree_max_tokens(long free_max_tokens) {
        Common.free_max_tokens = free_max_tokens;
    }

    public boolean isFree_unlimited_tokens() {
        return free_unlimited_tokens;
    }

    public void setFree_unlimited_tokens(boolean free_unlimited_tokens) {
        Common.free_unlimited_tokens = free_unlimited_tokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        Common.temperature = temperature;
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

    public void deleteAllChats(Activity activity, OnPreferencesClearedListener listener) {
        SharedPreferences preferences = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        new Handler().post(listener::onPreferencesCleared);
    }

    public void deleteChat(Activity activity, String key, OnPreferencesClearedListener listener) {
        SharedPreferences preferences = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("chats_" + key);
        editor.apply();

        deleteTimeStampFromList(activity, key, listener);
        deleteChatName(activity, key);
    }

    public void incrementAppOpenCount(Activity activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences("speakwise", MODE_PRIVATE).edit();
        editor.putInt("app_open_count", getAppOpenCount(activity) + 1);
        editor.apply();
    }

    public int getAppOpenCount(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        return prefs.getInt("app_open_count", 0);
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


    public void saveTimeStamp(Activity activity, String date, String message) {
        SharedPreferences prefs = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        String dateString = prefs.getString("list", "");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();

        ArrayList<String> list = gson.fromJson(dateString, type);
        if (list == null) list = new ArrayList<>();
        list.add(date);

        SharedPreferences.Editor editor = activity.getSharedPreferences("speakwise", MODE_PRIVATE).edit();
        editor.putString("list", gson.toJson(list));
        editor.apply();

        saveChatName(activity, date, message);
    }

    public String getChatName(Activity activity, String timeStamp) {
        SharedPreferences prefs = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        return prefs.getString("chat_name_" + timeStamp, "");
    }

    public void saveChatName(Activity activity, String timeStamp, String chatName) {
        SharedPreferences.Editor editor = activity.getSharedPreferences("speakwise", MODE_PRIVATE).edit();
        editor.putString("chat_name_" + timeStamp, chatName);
        editor.apply();
    }

    public void deleteChatName(Activity activity, String timeStamp) {
        SharedPreferences preferences = activity.getSharedPreferences("speakwise", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("chat_name_" + timeStamp);
        editor.apply();
    }

    private void deleteTimeStampFromList(Activity activity, String key, OnPreferencesClearedListener listener) {
        SharedPreferences prefs = activity.getSharedPreferences("speakwise", MODE_PRIVATE);
        String dateString = prefs.getString("list", "");

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();


        ArrayList<String> list = gson.fromJson(dateString, type);

        if (list != null && !list.isEmpty()) {
            list.remove(key);
        }


        SharedPreferences.Editor editor = activity.getSharedPreferences("speakwise", MODE_PRIVATE).edit();
        editor.putString("list", gson.toJson(list));
        editor.apply();

        new Handler().post(listener::onPreferencesCleared);
    }
}