package com.subdiox.blitzmodder;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.content.*;

import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import com.longevitysoft.android.xml.plist.domain.PListObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import io.github.luizgrp.sectionedrecyclerviewadapter.*;

import static android.os.Environment.getExternalStorageDirectory;
import static java.lang.Math.pow;

public class MainActivity extends AppCompatActivity {

    public static BMApplication app;
    public static UserSettings userSettings;
    public static SectionedRecyclerViewAdapter sectionAdapter;
    public static ArrayList<String> repoArray;
    public static ArrayList<String> repoNameArray;
    public static ArrayList<String> repoVersionArray;
    public static ArrayList<String> languageArray;
    public static int currentRepo;
    public static ArrayList<String> buttonArray;
    public static ArrayList<String> installedArray;
    public static String blitzPath;
    public static ArrayList<String> modCategoryArray;
    public static ArrayList<ArrayList<String>> modNameArray;
    public static ArrayList<ArrayList<ArrayList<String>>> modDetailArray;
    public static String locale;
    public static AlertDialog.Builder alertDialogBuilder;
    public static AlertDialog alertDialog;
    public static AlertDialog sdcardAlertDialog;
    public static String blitzMessage;
    public static boolean internal;
    public static boolean fileFound;
    public static Handler handler;
    public static int REQUEST_CODE;
    public static Uri treeUri;
    public static String treeUriString;
    public static String SDPath;
    public static String downloadPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userSettings = UserSettings.getInstance(getApplicationContext());
        if (!userSettings.repoArray.get(0).equals("http://subdiox.com/repo")) {
            userSettings.repoArray.remove(0);
            userSettings.repoArray.add("http://subdiox.com/repo");
            userSettings.saveInstance(getApplicationContext());
        }

        getUserSettings();

        languageArray = new ArrayList<>();
        languageArray.addAll(Arrays.asList("en", "ja", "ru", "zh"));

        System.out.println("locale: " + Locale.getDefault().getLanguage());
        if (locale == null || locale.isEmpty()) {
            locale = Locale.getDefault().getLanguage();
            System.out.println("locale: " + locale);
            if (!languageArray.contains(locale)) {
                locale = "en";
            }
        }

        setLocale(locale);

        if (repoNameArray == null) {
            repoNameArray = new ArrayList<>();
            repoNameArray.add("BlitzModder");
        }

        setContentView(R.layout.activity_main);

        // get application variables
        app = (BMApplication)this.getApplication();
        app.init();

        // initialize sectionedRecyclerViewAdapter
        sectionAdapter = new SectionedRecyclerViewAdapter();

        // initialize handler
        handler = new Handler();

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        } else {
            initializeApp();
        }
    }

    public void initializeApp() {
        // load mods list
        refreshModsList();

        app.modCategoryArray = modCategoryArray;
        app.modNameArray = modNameArray;
        app.modDetailArray = modDetailArray;

        // initialize sections
        for (int i = 0; i < modCategoryArray.size(); i++) {
            sectionAdapter.addSection(new ModSection(i));
        }

        // initialize recyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);

        // initialize apply button
        findViewById(R.id.apply_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareAlertDialog();
                alertDialog = alertDialogBuilder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        });

        // initialize repo list button
        findViewById(R.id.textview_mod_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = repoArray.toArray(new String[0]);
                int defaultItem = currentRepo;
                final List<Integer> checkedItems = new ArrayList<>();
                checkedItems.add(defaultItem);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.select_repository))
                        .setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkedItems.clear();
                                checkedItems.add(which);
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!checkedItems.isEmpty()) {
                                    currentRepo = checkedItems.get(0);
                                    saveUserSettings();
                                    finish();
                                    startActivity(getIntent());
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }
        });

        // initialize settings button
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.act_close_enter_anim, R.anim.act_close_exit_anim);
            }
        });

        TextView repoNameText = (TextView) findViewById(R.id.textview_repo_name);

        repoNameText.setText(repoNameArray.get(currentRepo));

        checkBlitzExists();

        if (!isNetworkAvailable(getApplicationContext())) {
            alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setTitle(getResources().getString(R.string.error));
            alertDialogBuilder.setMessage(getResources().getString(R.string.connection_error_message));
            alertDialogBuilder.setPositiveButton(
                    getResources().getString(R.string.recheck),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            refreshModsList();
                        }
                    });
            handler.post(new Runnable() {
                public void run() {
                    alertDialog = alertDialogBuilder.create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            });
        }
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");

    public static String[] getStorageDirectories() {
        // Final set of paths
        final Set<String> rv = new HashSet<>();
        // Primary physical SD-CARD (not emulated)
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        // Primary emulated SD-CARD
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if(TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if(TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                rv.add("/storage/sdcard0");
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rawUserId = "";
            } else {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] folders = DIR_SEPARATOR.split(path);
                final String lastFolder = folders[folders.length - 1];
                boolean isDigit = false;
                try {
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                } catch (NumberFormatException ignored) {
                }
                rawUserId = isDigit ? lastFolder : "";
            }
            // /storage/emulated/0[1,2,...]
            if(TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget);
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storages
        if(!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            // All Secondary SD-CARDs splitted into array
            final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorages);
        }
        return rv.toArray(new String[rv.size()]);
    }

    // Android 6.0でストレージへの書き込み権限を取得
    @TargetApi(23)
    public void checkPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(getString(R.string.confirm_title));
                alertDialogBuilder.setMessage(getString(R.string.permission_required));
                alertDialogBuilder.setPositiveButton(
                        getResources().getString(R.string.recheck),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermission();
                            }
                        });
                alertDialog = alertDialogBuilder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        } else {
            initializeApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            initializeApp();
        }
    }

    // Android 5.0でSDカードのtree uriを取得
    @TargetApi(21)
    public void requestSdcardAccessPermission() {
        AlertDialog.Builder sdcardAlertDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.dialog_sdcard, null);
        sdcardAlertDialogBuilder.setTitle(getResources().getString(R.string.sdcard_permission_title));
        sdcardAlertDialogBuilder.setMessage(getResources().getString(R.string.sdcard_permission_message));
        sdcardAlertDialogBuilder.setView(view);
        sdcardAlertDialogBuilder.setPositiveButton(
                getResources().getString(R.string.okay),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                });
        sdcardAlertDialog = sdcardAlertDialogBuilder.create();
        sdcardAlertDialog.setCancelable(false);
        sdcardAlertDialog.show();
    }

    @TargetApi(21)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK) {
            treeUri = resultData.getData();
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            treeUriString = treeUri.toString();
            if (treeUri != null) {
                SDPath = FileUtil.getFullPathFromTreeUri(treeUri, this);
            }
            getBlitzPath();
        }
    }

    @TargetApi(19)
    public void getBlitzPath() {
        if (SDPath != null) {
            if (!SDPath.equals("")) {
                Toast.makeText(getApplicationContext(), "SD path: " + SDPath, Toast.LENGTH_LONG).show();
                File blitz = new File(SDPath + "/Android/data/net.wargaming.wot.blitz");
                if (blitz.exists()) {
                    File blitzData = new File(SDPath + "/Android/data/net.wargaming.wot.blitz/files/Data");
                    if (blitzData.exists()) {
                        blitzPath = SDPath + "/Android/data/net.wargaming.wot.blitz/files/Data";
                        Toast.makeText(getApplicationContext(), "Blitz Data path: " + blitzPath, Toast.LENGTH_LONG).show();
                    } else {
                        blitzPath = "error";
                        userSettings.saveInstance(getApplicationContext());
                        blitzMessage = getResources().getString(R.string.blitz_not_initialized);
                        checkBlitzExists();
                    }
                } else {
                    blitzPath = "error";
                    userSettings.saveInstance(getApplicationContext());
                    blitzMessage = getResources().getString(R.string.blitz_not_installed);
                    checkBlitzExists();
                }
            } else {
                alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(getResources().getString(R.string.error));
                alertDialogBuilder.setMessage(getResources().getString(R.string.some_error_occurred));
                alertDialogBuilder.setPositiveButton(
                        getResources().getString(R.string.okay),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                alertDialog = alertDialogBuilder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
            saveUserSettings();
        }
    }

    // UserSettingsを取得
    public void getUserSettings() {
        // get preference variables
        userSettings = UserSettings.getInstance(getApplicationContext());
        repoArray = userSettings.repoArray;
        currentRepo = userSettings.currentRepo;
        buttonArray = userSettings.buttonArray;
        installedArray = userSettings.installedArray;
        blitzPath = userSettings.blitzPath;
        internal = userSettings.internal;
        treeUriString = userSettings.treeUriString;
        locale = userSettings.locale;
        repoNameArray = userSettings.repoNameArray;
        repoVersionArray = userSettings.repoVersionArray;
    }

    // UserSettingsを保存
    public void saveUserSettings() {
        // save preference variables
        userSettings.repoArray = repoArray;
        userSettings.currentRepo = currentRepo;
        userSettings.buttonArray = buttonArray;
        userSettings.installedArray = installedArray;
        userSettings.blitzPath = blitzPath;
        userSettings.internal = internal;
        userSettings.treeUriString = treeUriString;
        userSettings.locale = locale;
        userSettings.repoNameArray = repoNameArray;
        userSettings.repoVersionArray = repoVersionArray;
        userSettings.saveInstance(getApplicationContext());
    }

    // アラートダイアログを準備
    public void prepareAlertDialog() {
        // initialize alert dialog
        getUserSettings();
        alertDialogBuilder = new AlertDialog.Builder(this);
        String message = getResources().getString(R.string.confirm_message) + "\n\n";
        boolean installWritten = false;
        boolean removeWritten = false;
        boolean applyNeeded = false;
        for (int i = 0; i < modCategoryArray.size(); i++) {
            for (int j = 0; j < modNameArray.get(i).size(); j++) {
                for (int k = 0; k < modDetailArray.get(i).get(j).size(); k++) {
                    if (buttonArray.contains(getFullID(true,i,j,k)) && !installedArray.contains(getFullID(true,i,j,k))) {
                        applyNeeded = true;
                        if (!installWritten) {
                            message += getResources().getString(R.string.install);
                            installWritten = true;
                        }
                        message += "\n";
                        message += " - " + getName(modDetailArray.get(i).get(j).get(k));
                    }
                }
            }
        }
        for (int i = 0; i < modCategoryArray.size(); i++) {
            for (int j = 0; j < modNameArray.get(i).size(); j++) {
                for (int k = 0; k < modDetailArray.get(i).get(j).size(); k++) {
                    if (!buttonArray.contains(getFullID(true,i,j,k)) && installedArray.contains(getFullID(true,i,j,k))) {
                        applyNeeded = true;
                        if (!removeWritten) {
                            if (installWritten) {
                                message += "\n\n";
                            }
                            message += getResources().getString(R.string.remove);
                            removeWritten = true;
                        }
                        message += "\n";
                        message += " - " + getName(modDetailArray.get(i).get(j).get(k));
                    }
                }
            }
        }

        if (applyNeeded) {
            alertDialogBuilder.setTitle(getResources().getString(R.string.confirm_title));
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(), ProcessActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.act_open_enter_anim, R.anim.act_open_exit_anim);
                        }
                    });
            alertDialogBuilder.setNegativeButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        } else {
            alertDialogBuilder.setTitle(getResources().getString(R.string.no_changes_title));
            alertDialogBuilder.setMessage(getResources().getString(R.string.no_changes_message));
            alertDialogBuilder.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        }
    }

    public void checkBlitzExists() {
        System.out.println("blitzPath: " + blitzPath);
        if (blitzPath == null || blitzPath.equals("")) {
            alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getResources().getString(R.string.blitz_check_title));
            blitzMessage = getResources().getString(R.string.blitz_initial_check);
            alertDialogBuilder.setMessage(blitzMessage);
            blitzMessage = "";
            alertDialogBuilder.setNegativeButton(
                    getResources().getString(R.string.internal),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            blitzInternalCheck();
                        }
                    });
            alertDialogBuilder.setPositiveButton(
                    getResources().getString(R.string.external),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                            alertDialogBuilder.setTitle(getResources().getString(R.string.notice));
                            alertDialogBuilder.setMessage(getResources().getString(R.string.sdcard_notice));
                            alertDialogBuilder.setPositiveButton(
                                    getResources().getString(R.string.okay),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            blitzExternalCheck();
                                        }
                                    });
                            alertDialogBuilder.setNegativeButton(
                                    getResources().getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            checkBlitzExists();
                                        }
                                    });
                            alertDialogBuilder.create().show();
                        }
                    });
            alertDialog = alertDialogBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        } else if (blitzPath.equals("error")) {
            alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getResources().getString(R.string.error));
            alertDialogBuilder.setMessage(blitzMessage);
            alertDialogBuilder.setPositiveButton(
                    getResources().getString(R.string.recheck),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            blitzPath = "";
                            checkBlitzExists();
                        }
                    });
            alertDialog = alertDialogBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        } else {
            if (internal) {
                blitzInternalCheck();
            } else {
                blitzExternalCheck();
            }
        }
    }

    // 内部ストレージのBlitzデータ存在確認
    public void blitzInternalCheck() {
        internal = true;
        File blitz = new File(getExternalStorageDirectory() + "/Android/data/net.wargaming.wot.blitz/files");
        if (blitz.exists()) {
            File blitzData = new File(getExternalStorageDirectory() + "/Android/data/net.wargaming.wot.blitz/files/Data");
            if (blitzData.exists()) {
                blitzPath = getExternalStorageDirectory() + "/Android/data/net.wargaming.wot.blitz/files/Data";
                Toast.makeText(getApplicationContext(), "Blitz Data path: " + blitzPath, Toast.LENGTH_LONG).show();
            } else {
                blitzPath = "error";
                blitzMessage = getResources().getString(R.string.blitz_not_initialized);
                checkBlitzExists();
            }
        } else {
            blitzPath = "error";
            blitzMessage = getResources().getString(R.string.blitz_not_installed);
            checkBlitzExists();
        }
        saveUserSettings();
    }

    // 外部ストレージのBlitzデータ存在確認
    public void blitzExternalCheck() {
        internal = false;
        System.out.println("treeUriString: " + treeUriString);
        if (Build.VERSION.SDK_INT >= 21) {
            if (treeUriString != null) {
                if (treeUriString.equals("")) {
                    requestSdcardAccessPermission();
                }
            } else {
                requestSdcardAccessPermission();
            }

        } else if (Build.VERSION.SDK_INT == 19) {
            internal = true;
            SDPath = "";
            AlertDialog.Builder internalAlertDialog = new AlertDialog.Builder(this);
            internalAlertDialog.setTitle(getResources().getString(R.string.error));
            internalAlertDialog.setMessage(getResources().getString(R.string.sdcard_not_supported_kitkat));
            internalAlertDialog.setPositiveButton(
                    getResources().getString(R.string.okay),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (treeUriString != null) {
                                if (treeUriString.equals("")) {
                                    requestSdcardAccessPermission();
                                }
                            } else {
                                requestSdcardAccessPermission();
                            }
                        }
                    });
            AlertDialog internalAlert = internalAlertDialog.create();
            internalAlert.setCancelable(false);
            internalAlert.show();
        } else {
            String[] storages = getStorageDirectories();
            SDPath = storages[1];
            getBlitzPath();
        }
    }

    // read file and return its string data
    private String readFile(String FileName) {
        File file = new File(FileName);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    // refresh mods list
    private void refreshModsList() {
        String dirPath = getCacheDir() + "/" + repoNameArray.get(currentRepo) + "/plist";
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        ArrayList<String> tryArray = languageArray;
        int index = tryArray.indexOf(locale);
        if (index != -1) {
            tryArray.set(index, tryArray.get(0));
            tryArray.set(0, locale);
        }
        System.out.println("tryArray: " + tryArray);
        String okLocale = tryArray.get(0);
        for (String tryLocale : tryArray) {
            try {
                downloadPath = getCacheDir() + "/" + repoNameArray.get(currentRepo) + "/plist/" + tryLocale + ".plist";
                new DownloadTask(this).execute(repoArray.get(currentRepo) + "/plist/" + tryLocale + ".plist").get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            if (fileFound) {
                okLocale = tryLocale;
                break;
            }
        }
        PListXMLParser parser = new PListXMLParser();
        PListXMLHandler handler = new PListXMLHandler();
        parser.setHandler(handler);
        try {
            String plistPath = getCacheDir() + "/" + repoNameArray.get(currentRepo) + "/plist/" + okLocale + ".plist";
            if (new File(plistPath).exists()) {
                parser.parse(readFile(plistPath));
                PList actualPList = ((PListXMLHandler) parser.getHandler()).getPlist();
                LinkedHashMap<String, PListObject> modCategoryPlist = ((Dict) actualPList.getRootElement()).getConfigMap();
                modCategoryArray = new ArrayList<>(modCategoryPlist.keySet());
                modNameArray = new ArrayList<>();
                modDetailArray = new ArrayList<>();
                for (int i = 0; i < modCategoryArray.size(); i++) {
                    LinkedHashMap<String, PListObject> modNamePlist = ((Dict) modCategoryPlist.get(modCategoryArray.get(i))).getConfigMap();
                    modNameArray.add(new ArrayList<>(modNamePlist.keySet()));
                    ArrayList<ArrayList<String>> tempArray = new ArrayList<>();
                    for (int j = 0; j < modNameArray.get(i).size(); j++) {
                        LinkedHashMap<String, PListObject> modDetailPlist = ((Dict) modNamePlist.get(modNameArray.get(i).get(j))).getConfigMap();
                        ArrayList<String> keysArray = new ArrayList<>(modDetailPlist.keySet());
                        ArrayList<String> valuesArray = new ArrayList<>();
                        for (String key : keysArray) {
                            valuesArray.add(((com.longevitysoft.android.xml.plist.domain.String) modDetailPlist.get(key)).getValue());
                        }
                        int current = 0;
                        int removed = 0;
                        while (current < valuesArray.size()) {
                            if (!checkValidate(valuesArray.get(current))) {
                                keysArray.remove(current - removed);
                                removed += 1;
                            }
                            current += 1;
                        }
                        if (keysArray.size() == 0) {
                            modNameArray.get(i).remove(j);
                            j--;
                        } else {
                            tempArray.add(keysArray);
                        }
                    }
                    modDetailArray.add(tempArray);
                }
            } else {
                modCategoryArray = new ArrayList<>();
                modNameArray = new ArrayList<>();
                modDetailArray = new ArrayList<>();
            }

            sectionAdapter.notifyDataSetChanged();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public boolean checkValidate(String string) {
        String[] array = string.split(":",-1);
        if (array.length == 2) {
            if (convertVersion(array[0]) >= convertVersion(getBlitzVersion())) {
                if (array[1].matches(".*a.*")) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public String getBlitzVersion() {
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo("net.wargaming.wot.blitz", 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "0.0.0";
        }
        String[] array = versionName.split("\\.");
        return array[0] + "." + array[1] + "." + array[2];
    }

    public double convertVersion(String version) {
        String[] versionArray = version.split("\\.");
        double converted = 0;
        for (int i = 0; i < versionArray.length; i++) {
            converted += Double.parseDouble(versionArray[i]) * pow(10.0, -(double)i);
        }
        return converted;
    }

    public String getID(String string) {
        String[] array = string.split(":");
        if (array.length == 2) {
            return array[1];
        } else {
            return "error";
        }
    }

    public String getName(String string) {
        String[] array = string.split(":");
        if (array.length == 2) {
            return array[0];
        } else {
            return "error";
        }
    }

    public String getFullID(boolean b, int i, int j, int k) {
        if (b) { // with repo name
            return repoNameArray.get(currentRepo) + "." + getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        } else { // without repo name
            return getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        }
    }

    public void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        //Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
        //startActivity(refresh);
        //finish();
    }

    class ModSection extends StatelessSection {
        String title;
        List<String> list;
        int section;

        public ModSection(int section) {
            super(R.layout.section_header, R.layout.section_item);
            this.title = modCategoryArray.get(section);
            this.list = modNameArray.get(section);
            this.section = section;
        }

        @Override
        public int getContentItemsTotal() {
            return list.size();
        }

        @Override
        public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
            return new com.subdiox.blitzmodder.HeaderViewHolder(view);
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new com.subdiox.blitzmodder.ItemViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
            com.subdiox.blitzmodder.HeaderViewHolder headerHolder = (com.subdiox.blitzmodder.HeaderViewHolder) holder;
            headerHolder.modCategory.setText(getName(title));
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final com.subdiox.blitzmodder.ItemViewHolder itemHolder = (com.subdiox.blitzmodder.ItemViewHolder) holder;
            itemHolder.modName.setText(getName(list.get(position)));
            itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), SubActivity.class);
                    intent.putExtra("section", section);
                    intent.putExtra("position", position);
                    startActivity(intent);
                    overridePendingTransition(R.anim.act_open_enter_anim, R.anim.act_open_exit_anim);
                }
            });
        }
    }

    public class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        private DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                fileFound = true;
                int statusCode = connection.getResponseCode();
                // check 20x
                if (statusCode != HttpURLConnection.HTTP_OK
                        && statusCode != HttpURLConnection.HTTP_CREATED) {
                    // check 30x
                    if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                            || statusCode == HttpURLConnection.HTTP_MOVED_PERM
                            || statusCode == HttpURLConnection.HTTP_SEE_OTHER) {

                        String newUrlString = connection.getHeaderField("Location");
                        connection = (HttpURLConnection) new URL(newUrlString).openConnection();
                    } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        System.out.println("File Not Found");
                        fileFound = false;
                    } else {
                        // 20x or 30x系でないので例外を送出
                        throw new IOException("Response code is " + Integer.toString(statusCode) + " " + connection.getResponseMessage());
                    }
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(downloadPath);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
        }
    }
}

class HeaderViewHolder extends RecyclerView.ViewHolder {
    public final TextView modCategory;
    public HeaderViewHolder(View view) {
        super(view);
        modCategory = (TextView)itemView.findViewById(R.id.modCategory);
    }
}

class ItemViewHolder extends RecyclerView.ViewHolder {
    public final TextView modName;
    public ItemViewHolder(View view) {
        super(view);
        modName = (TextView)itemView.findViewById(R.id.modDetail);
    }
}
