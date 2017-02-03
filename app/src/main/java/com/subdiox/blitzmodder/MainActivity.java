package com.subdiox.blitzmodder;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    public static int currentRepo;
    public static ArrayList<String> buttonArray;
    public static ArrayList<String> installedArray;
    public static String blitzPath;
    public static ArrayList<String> modCategoryArray;
    public static ArrayList<ArrayList<String>> modNameArray;
    public static ArrayList<ArrayList<ArrayList<String>>> modDetailArray;
    public static String locale;
    public static boolean downloadFinished;
    public static AlertDialog.Builder alertDialogBuilder;
    public static AlertDialog alertDialog;
    public static AlertDialog sdcardAlertDialog;
    public static String blitzMessage;
    public static boolean internal;
    public static Handler handler;
    public static boolean connectionError;
    public static int REQUEST_CODE;
    public static Uri treeUri;
    public static String treeUriString;
    public static String SDPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        handler = new Handler();
        setTitle(getResources().getString(R.string.main_menu_title));

        // get locale
        locale = Locale.getDefault().getLanguage();
        if (!(locale.equals("en")) && !(locale.equals("ja")) && !(locale.equals("ru"))) {
            locale = "en";
        }

        // get application variables
        app = (BMApplication)this.getApplication();
        app.init();

        getUserSettings();

        // initialize sectionedRecyclerViewAdapter
        sectionAdapter = new SectionedRecyclerViewAdapter();

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
        findViewById(R.id.applyButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareAlertDialog();
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        }

        checkBlitzExists();
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
            // All Secondary SD-CARDs splited into array
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
                alertDialogBuilder.setTitle(getResources().getString(R.string.confirm_title));
                alertDialogBuilder.setMessage(getResources().getString(R.string.permission_required));
                alertDialogBuilder.setPositiveButton(
                        getResources().getString(R.string.recheck),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermission();
                            }
                        });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }

    // Android 5.0でSDカードのtree uriを取得
    @TargetApi(21)
    public void requestSdcardAccessPermission() {
        AlertDialog.Builder sdcardAlertDialogBuilder = new AlertDialog.Builder(this);
        sdcardAlertDialogBuilder.setTitle(getResources().getString(R.string.sdcard_permission_title));
        sdcardAlertDialogBuilder.setMessage(getResources().getString(R.string.sdcard_permission_message));
        sdcardAlertDialogBuilder.setPositiveButton(
                getResources().getString(R.string.okay),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                });
        sdcardAlertDialog = sdcardAlertDialogBuilder.create();
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
                            // initialize intent
                            Intent intent = new Intent();
                            intent.setClassName("com.subdiox.blitzmodder", "com.subdiox.blitzmodder.ProcessActivity");

                            // start intent
                            startActivity(intent);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // initialize intent
            Intent intent = new Intent();
            intent.setClassName("com.subdiox.blitzmodder", "com.subdiox.blitzmodder.SettingsActivity");

            // start intent
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkBlitzExists() {
        System.out.println("blitzPath: " + blitzPath);
        if (blitzPath.equals("")) {
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
                            blitzExternalCheck();
                        }
                    });
            alertDialog = alertDialogBuilder.create();
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
                System.out.println("1");
                if (treeUriString.equals("")) {
                    System.out.println("2");
                    requestSdcardAccessPermission();
                }
            } else {
                System.out.println("3");
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
                            getBlitzPath();
                        }
                    });
            internalAlertDialog.create().show();
        } else {
            String[] storages = getStorageDirectories();
            SDPath = storages[1];
            getBlitzPath();
        }
    }

    // download from url and save as a file
    public void download(final String urlString, final String path) {
        connectionError = false;
        downloadFinished = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                try {
                    final URL url = new URL(urlString);
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();
                    final int status = con.getResponseCode();
                    if (status == HttpURLConnection.HTTP_OK) {
                        final InputStream input = con.getInputStream();
                        final DataInputStream dataInput = new DataInputStream(input);
                        final FileOutputStream fileOutput = new FileOutputStream(path);
                        final DataOutputStream dataOut = new DataOutputStream(fileOutput);
                        final byte[] buffer = new byte[4096];
                        int readByte = 0;
                        while((readByte = dataInput.read(buffer)) != -1) {
                            dataOut.write(buffer, 0, readByte);
                        }
                        dataInput.close();
                        fileOutput.close();
                        dataInput.close();
                        input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionError = true;
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    downloadFinished = true;
                }
            }
        }).start();
    }

    // read file and return its string data
    private String readFile(String FileName) {
        File file = new File(getExternalCacheDir(), FileName);
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
        download("https://github.com/" + repoArray.get(0) + "/BMRepository/raw/master/" + locale + ".plist", getExternalCacheDir() + "/" + repoArray.get(0) + "_" + locale + ".plist");
        while (!downloadFinished) {}
        if (connectionError) {
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
                    alertDialog.show();
                }
            });
        }
        PListXMLParser parser = new PListXMLParser();
        PListXMLHandler handler = new PListXMLHandler();
        parser.setHandler(handler);
        try {
            parser.parse(readFile(repoArray.get(0) + "_" + locale + ".plist"));
            PList actualPList = ((PListXMLHandler) parser.getHandler()).getPlist();
            LinkedHashMap<String,PListObject> modCategoryPlist = ((Dict)actualPList.getRootElement()).getConfigMap();
            modCategoryArray = new ArrayList<>(modCategoryPlist.keySet());
            modNameArray = new ArrayList<>();
            modDetailArray = new ArrayList<>();
            for(int i = 0; i < modCategoryArray.size(); i++) {
                LinkedHashMap<String, PListObject> modNamePlist = ((Dict) modCategoryPlist.get(modCategoryArray.get(i))).getConfigMap();
                modNameArray.add(new ArrayList<>(modNamePlist.keySet()));
                ArrayList<ArrayList<String>> tempArray = new ArrayList<>();
                for (int j = 0; j < modNameArray.get(i).size(); j++) {
                    LinkedHashMap<String, PListObject> modDetailPlist = ((Dict) modNamePlist.get(modNameArray.get(i).get(j))).getConfigMap();
                    ArrayList<String> keysArray = new ArrayList<>(modDetailPlist.keySet());
                    ArrayList<String> valuesArray = new ArrayList<>();
                    for (String key : keysArray) {
                        valuesArray.add(((com.longevitysoft.android.xml.plist.domain.String)modDetailPlist.get(key)).getValue());
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
            return repoArray.get(currentRepo) + "." + getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        } else { // without repo name
            return getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        }
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
                    // initialize intent
                    Intent intent = new Intent();
                    intent.setClassName("com.subdiox.blitzmodder", "com.subdiox.blitzmodder.SubActivity");

                    // set intent extra data
                    intent.putExtra("section", section);
                    intent.putExtra("position", position);

                    // start intent
                    startActivity(intent);
                }
            });
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