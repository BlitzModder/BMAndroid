package com.subdiox.blitzmodder;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;

public class ProcessActivity extends AppCompatActivity {

    public static BMApplication app;
    public static UserSettings userSettings;
    public static ArrayList<String> repoArray;
    public static int currentRepo;
    public static ArrayList<String> buttonArray;
    public static ArrayList<String> installedArray;
    public static String blitzPath;
    public static ArrayList<String> modCategoryArray;
    public static ArrayList<ArrayList<String>> modNameArray;
    public static ArrayList<ArrayList<ArrayList<String>>> modDetailArray;
    public static TextView logView;
    public static ScrollView scrollView;
    public static ProgressBar progressBar;
    public static Button backButton;
    public static boolean downloadFinished;
    public static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);
        setTitle(getResources().getString(R.string.process_menu_title));

        // initialize back button
        backButton = (Button)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        backButton.setVisibility(View.INVISIBLE);

        // initialize progress bar
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setMax(100);

        // initialize scroll/log view
        scrollView = (ScrollView)findViewById(R.id.scrollView);
        logView = (TextView)findViewById(R.id.logView);
        logView.setText("");

        scrollView.post(new Runnable() {
            public void run() {
                scrollView.scrollTo(0, scrollView.getBottom());
            }
        });

        // get application variables
        app = (BMApplication)this.getApplication();

        modCategoryArray = app.modCategoryArray;
        modNameArray = app.modNameArray;
        modDetailArray = app.modDetailArray;

        handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startProcess();
            }
        }).start();
    }

    public void getUserSettings() {
        // get preference variables
        userSettings = UserSettings.getInstance(getApplicationContext());
        repoArray = userSettings.repoArray;
        currentRepo = userSettings.currentRepo;
        buttonArray = userSettings.buttonArray;
        installedArray = userSettings.installedArray;
        blitzPath = userSettings.blitzPath;
    }

    public void saveUserSettings() {
        // save preference variables
        userSettings.repoArray = repoArray;
        userSettings.currentRepo = currentRepo;
        userSettings.buttonArray = buttonArray;
        userSettings.installedArray = installedArray;
        userSettings.blitzPath = blitzPath;
        userSettings.saveInstance(getApplicationContext());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction()== KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void startProcess() {
        getUserSettings();
        for (int i = 0; i < modCategoryArray.size(); i++) {
            for (int j = 0; j < modNameArray.get(i).size(); j++) {
                for (int k = 0; k < modDetailArray.get(i).get(j).size(); k++) {
                    if (!buttonArray.contains(getFullID(true,i,j,k)) && installedArray.contains(getFullID(true,i,j,k))) {
                        log("Downloading the removal data of " + getFullID(false,i,j,k) + " ...");
                        downloadData(false,i,j,k);
                        while (!downloadFinished) {}
                        log("Done.\n");
                        log("Removing " + getFullID(false,i,j,k) + " ...");
                        installData();
                        getUserSettings();
                        installedArray.remove(getFullID(true,i,j,k));
                        saveUserSettings();
                        log("Done.\n");
                    }
                }
            }
        }
        for (int i = 0; i < modCategoryArray.size(); i++) {
            for (int j = 0; j < modNameArray.get(i).size(); j++) {
                for (int k = 0; k < modDetailArray.get(i).get(j).size(); k++) {
                    if (buttonArray.contains(getFullID(true,i,j,k)) && !installedArray.contains(getFullID(true,i,j,k))) {
                        log("Downloading " + getFullID(false,i,j,k) + " ...");
                        downloadData(true,i,j,k);
                        while (!downloadFinished) {}
                        log("Done.\n");
                        log("Installing " + getFullID(false,i,j,k) + " ...");
                        installData();
                        getUserSettings();
                        installedArray.add(getFullID(true,i,j,k));
                        saveUserSettings();
                        log("Done.\n");
                    }
                }
            }
        }
        handler.post(new Runnable() {
            public void run() {
                progressBar.setVisibility(View.INVISIBLE);
                backButton.setVisibility(View.VISIBLE);
            }
        });
    }

    public void log(final String str) {
        handler.post(new Runnable() {
            public void run() {
                logView.append(str);
            }
        });
    }

    // download from url and save as a file
    public void download(final String urlString, final String path) {
        downloadFinished = false;
        handler.post(new Runnable() {
            public void run() {
                progressBar.setProgress(0);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                try {
                    final URL url = new URL(urlString);
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();
                    int fileLength = con.getContentLength();
                    final int status = con.getResponseCode();
                    if (status == HttpURLConnection.HTTP_OK) {
                        final InputStream input = con.getInputStream();
                        final DataInputStream dataInput = new DataInputStream(input);
                        final FileOutputStream fileOutput = new FileOutputStream(path);
                        final DataOutputStream dataOut = new DataOutputStream(fileOutput);
                        final byte[] buffer = new byte[4096];
                        int readByte;
                        int total = 0;
                        while((readByte = dataInput.read(buffer)) != -1) {
                            total += readByte;
                            final int progress = total * 100 / fileLength;
                            handler.post(new Runnable() {
                                public void run() {
                                    progressBar.setProgress(progress);
                                }
                            });

                            dataOut.write(buffer, 0, readByte);
                        }
                        dataInput.close();
                        fileOutput.close();
                        dataInput.close();
                        input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    downloadFinished = true;
                    handler.post(new Runnable() {
                        public void run() {
                            progressBar.setProgress(100);
                        }
                    });
                }
            }
        }).start();
    }

    public void downloadData(boolean install, int i, int j, int k) {
        if (install) {
            download("https://github.com/" + repoArray.get(currentRepo) + "/BMRepository/raw/master/Install/" + getFullID(false,i,j,k) + ".zip", getCacheDir() + "/Data.zip");
        } else {
            download("https://github.com/" + repoArray.get(currentRepo) + "/BMRepository/raw/master/Remove/" + getFullID(false,i,j,k) + ".zip", getCacheDir() + "/Data.zip");
        }
    }

    private static void delete (File f) {
        if (!f.exists()) {
            return;
        }
        if (f.isFile()) {
            f.delete();
        } else if (f.isDirectory()){
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
            f.delete();
        }
    }

    public void copy(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            copyFile(sourceLocation, targetLocation);
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdir();
        }

        for (String f : source.list()) {
            copy(new File(source, f), new File(target, f));
        }
    }

    private void copyFile(File source, File target) throws IOException {
        InputStream in;
        OutputStream out;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void installData() {
        handler.post(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(true);
            }
        });
        File dataDir = new File(getCacheDir() + "/Data");
        delete(dataDir);
        try {
            ZipFile zipFile = new ZipFile(getCacheDir() + "/Data.zip");
            zipFile.extractAll(getCacheDir().toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
        File modData = new File(getCacheDir() + "/Data");;
        File blitzData = new File(blitzPath);
        try {
            copyDirectory(modData, blitzData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler.post(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
    }

    public String getID(String string) {
        String[] array = string.split(":", -1);
        if (array.length == 2) {
            return array[1];
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
}