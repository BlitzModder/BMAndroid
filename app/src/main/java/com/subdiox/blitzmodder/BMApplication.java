package com.subdiox.blitzmodder;

import android.app.Application;
import java.util.ArrayList;

public class BMApplication extends Application {
    public static ArrayList<String> modCategoryArray;
    public static ArrayList<ArrayList<String>> modNameArray;
    public static ArrayList<ArrayList<ArrayList<String>>> modDetailArray;

    public void init() {
        modCategoryArray = null;
        modNameArray = null;
        modDetailArray = null;
    }
}