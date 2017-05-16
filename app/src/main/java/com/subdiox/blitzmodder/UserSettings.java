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
import java.util.Arrays;

public class UserSettings {
    public ArrayList<String> repoArray;
    public int currentRepo;
    public ArrayList<String> buttonArray;
    public ArrayList<String> installedArray;
    public String blitzPath;
    public boolean internal;
    public String treeUriString;
    public String locale;
    public ArrayList<String> repoNameArray;
    public ArrayList<String> repoVersionArray;

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
        instance.repoArray = new ArrayList<>();
        instance.repoArray.add("http://subdiox.com/repo");
        instance.currentRepo = 0;
        instance.buttonArray = new ArrayList<>();
        instance.installedArray = new ArrayList<>();
        instance.blitzPath = "";
        instance.internal = true;
        instance.treeUriString = "";
        instance.locale = "";
        instance.repoNameArray = new ArrayList<>();
        instance.repoNameArray.add("BlitzModder");
        instance.repoVersionArray = new ArrayList<>();
        instance.repoVersionArray.add("0.0.0");
        return instance;
    }

    public void saveInstance(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        prefs.edit().putString(USER_SETTING_PREF_KEY, gson.toJson(this)).apply();
    }
}