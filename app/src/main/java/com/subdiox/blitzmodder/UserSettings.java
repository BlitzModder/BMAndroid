package com.subdiox.blitzmodder;

/**
 * Created by subdiox on 2016/12/16.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.util.ArrayList;

public class UserSettings {
    public ArrayList<String> repoArray;
    public int currentRepo;
    public ArrayList<String> buttonArray;
    public ArrayList<String> installedArray;
    public String blitzPath;
    public boolean internal;
    public String treeUriString;

    private static final String USER_SETTING_PREF_KEY="USER_SETTING";

    public static UserSettings getInstance(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String userSettingsString = prefs.getString(USER_SETTING_PREF_KEY, "");
        UserSettings instance;
        if (!TextUtils.isEmpty(userSettingsString)) {
            instance = gson.fromJson(userSettingsString, UserSettings.class);
        } else {
            instance = getDefaultInstance();
        }
        return instance;
    }

    public static UserSettings getDefaultInstance(){
        UserSettings instance = new UserSettings();
        instance.repoArray = new ArrayList<String>();
        instance.repoArray.add("http://subdiox.com/repo");
        instance.currentRepo = 0;
        instance.buttonArray = new ArrayList<String>();
        instance.installedArray = new ArrayList<String>();
        instance.blitzPath = "";
        instance.internal = true;
        instance.treeUriString = "";
        return instance;
    }

    public void saveInstance(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        prefs.edit().putString(USER_SETTING_PREF_KEY, gson.toJson(this)).apply();
    }
}