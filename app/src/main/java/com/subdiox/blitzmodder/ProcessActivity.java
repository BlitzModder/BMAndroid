package com.subdiox.blitzmodder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

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
    public static boolean internal;
    public static String treeUriString;
    public static Uri treeUri;
    public TextView logView;
    public ScrollView scrollView;
    public ProgressBar progressBar;
    public Button backButton;
    public Handler handler;

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

        // initialize progress bar
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        modCategoryArray = app.modCategoryArray;
        modNameArray = app.modNameArray;
        modDetailArray = app.modDetailArray;

        handler = new Handler();

        getUserSettings();
        if (Build.VERSION.SDK_INT >= 21) {
            if (!treeUriString.equals("")) {
                treeUri = Uri.parse(treeUriString);
            } else {
                Toast.makeText(getApplicationContext(), "Error: Tree URI is empty", Toast.LENGTH_LONG).show();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                startProcess();
            }
        }).start();
    }

    // get preference variables
    public void getUserSettings() {
        userSettings = UserSettings.getInstance(getApplicationContext());
        repoArray = userSettings.repoArray;
        currentRepo = userSettings.currentRepo;
        buttonArray = userSettings.buttonArray;
        installedArray = userSettings.installedArray;
        blitzPath = userSettings.blitzPath;
        internal = userSettings.internal;
        treeUriString = userSettings.treeUriString;
    }

    // save preference variables
    public void saveUserSettings() {
        userSettings.repoArray = repoArray;
        userSettings.currentRepo = currentRepo;
        userSettings.buttonArray = buttonArray;
        userSettings.installedArray = installedArray;
        userSettings.blitzPath = blitzPath;
        userSettings.internal = internal;
        userSettings.treeUriString = treeUriString;
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
                        final int fi = i;
                        final int fj = j;
                        final int fk = k;
                        try {
                            new DownloadTask(this).execute("https://github.com/" + repoArray.get(currentRepo) + "/BMRepository/raw/master/Remove/" + getFullID(false,i,j,k) + ".zip").get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        log("Done.\n");
                        log("Removing " + getFullID(false,fi,fj,fk) + " ...");
                        installData();
                        getUserSettings();
                        installedArray.remove(getFullID(true,fi,fj,fk));
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
                        final int fi = i;
                        final int fj = j;
                        final int fk = k;
                        try {
                            new DownloadTask(this).execute("https://github.com/" + repoArray.get(currentRepo) + "/BMRepository/raw/master/Install/" + getFullID(false, i, j, k) + ".zip").get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        log("Done.\n");
                        log("Installing " + getFullID(false,fi,fj,fk) + " ...");
                        installData();
                        getUserSettings();
                        installedArray.add(getFullID(true,fi,fj,fk));
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
        File dataDir = new File(getExternalCacheDir() + "/Data");
        delete(dataDir);
        try {
            ZipFile zipFile = new ZipFile(getExternalCacheDir() + "/Data.zip");
            zipFile.extractAll(getExternalCacheDir().toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
        File blitzData = new File(blitzPath);
        try {
            copyDirectory(dataDir, blitzData);
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

    public class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
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

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(getExternalCacheDir() + "/Data.zip");

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
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }
}