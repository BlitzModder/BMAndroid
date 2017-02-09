package com.subdiox.blitzmodder;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
        if (Build.VERSION.SDK_INT >= 21 && !internal) {
            if (!treeUriString.equals("")) {
                treeUri = Uri.parse(treeUriString);
            } else {
                Toast.makeText(getApplicationContext(), "Error: Tree URI is empty. Please reinstall BlitzModder.", Toast.LENGTH_LONG).show();
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
                        log("Downloading removal data of " + getFullID(false,i,j,k) + " ...");
                        try {
                            new DownloadTask(this).execute(repoArray.get(currentRepo) + "/remove/" + getFullID(false, i, j, k) + ".zip").get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        log("Done.\n");
                        log("Removing " + getFullID(false,i,j,k) + " ...");
                        installData(getFullID(false,i,j,k));
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
                        log("Downloading installation data of " + getFullID(false,i,j,k) + " ...");
                        try {
                            new DownloadTask(this).execute(repoArray.get(currentRepo) + "/install/" + getFullID(false, i, j, k) + ".zip").get();
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        log("Done.\n");
                        log("Installing " + getFullID(false,i,j,k) + " ...");
                        installData(getFullID(false,i,j,k));
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

    private String removeHttp(String string) {
        if (string.startsWith("http://")) {
            return string.substring("http://".length(), string.length()).replaceAll("/",":");
        } else if (string.startsWith("https://")) {
            return string.substring("https://".length(), string.length()).replaceAll("/",":");
        } else {
            return string.replaceAll("/",":");
        }
    }

    public void log(final String str) {
        handler.post(new Runnable() {
            public void run() {
                logView.append(str);
            }
        });
    }

    public void deleteSD(String filePath) {
        String internalPath = Environment.getExternalStorageDirectory().toString();
        String externalPath = "External Path";

        if (treeUri != null) {
            externalPath = FileUtil.getFullPathFromTreeUri(treeUri, this);
            if (externalPath == null) {
                System.out.println("externalPath is null");
                return;
            }
        }

        if (filePath.startsWith(internalPath) || Build.VERSION.SDK_INT <= 18) {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            if (file.isFile()) {
                if (file.delete()) {
                    System.out.println(file.getAbsolutePath() + " was deleted successfully.");
                }
            } else if (file.isDirectory()){
                File[] files = file.listFiles();
                for (File oneFile : files) {
                    deleteSD(oneFile.getAbsolutePath());
                }
                if (file.delete()) {
                    System.out.println(file.getAbsolutePath() + " was deleted successfully.");
                }
            }
        } else if (filePath.startsWith(externalPath)) {
            DocumentFile file = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
            String fileRelativePath = filePath.substring(externalPath.length() + 1, filePath.length());
            String[] filePathArray = fileRelativePath.split("/", -1);
            for (String filePathSegment : filePathArray) {
                file = file.findFile(filePathSegment);
            }
            if (file.delete()) {
                System.out.println("The specified file in SD card was deleted successfully.");
            } else {
                System.out.println("The specified file in SD card was not deleted.");
            }
        }
    }

    public void copySD(String sourcePath, String targetPath, boolean backup, String modID) throws IOException {
        String internalPath = Environment.getExternalStorageDirectory().toString();
        String externalPath = "External Path";

        if (treeUri != null) {
            externalPath = FileUtil.getFullPathFromTreeUri(treeUri, this);
            if (externalPath == null) {
                System.out.println("externalPath is null");
                return;
            }
        }
        if ((sourcePath.startsWith(internalPath) && targetPath.startsWith(internalPath)) || Build.VERSION.SDK_INT <= 18) {
            File sourceFile = new File(sourcePath);
            if (sourceFile.isDirectory()) {
                copyDirectorySD(sourcePath, targetPath, backup, modID);
            } else {
                copyFileSD(sourcePath, targetPath, backup, modID);
            }
        } else if (sourcePath.startsWith(externalPath) || targetPath.startsWith(externalPath)) {
            DocumentFile sourceFile = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
            if (sourcePath.startsWith(internalPath)) {
                sourceFile = DocumentFile.fromFile(new File(sourcePath));
            } else if (sourcePath.startsWith(externalPath)) {
                String sourceRelativePath = sourcePath.substring(externalPath.length() + 1, sourcePath.length());
                String[] sourceArray = sourceRelativePath.split("/", -1);
                for (String sourcePathSegment : sourceArray) {
                    sourceFile = sourceFile.findFile(sourcePathSegment);
                }
            }
            if (sourceFile.isDirectory()) {
                copyDirectorySD(sourcePath, targetPath, backup, modID);
            } else {
                copyFileSD(sourcePath, targetPath, backup, modID);
            }
        }
    }

    public void copyDirectorySD(String sourcePath, String targetPath, boolean backup, String modID) throws IOException {
        String internalPath = Environment.getExternalStorageDirectory().toString();
        String externalPath = "External Path";

        if (treeUri != null) {
            externalPath = FileUtil.getFullPathFromTreeUri(treeUri, this);
            if (externalPath == null) {
                System.out.println("externalPath is null");
                return;
            }
        }
        if ((sourcePath.startsWith(internalPath) && targetPath.startsWith(internalPath)) || Build.VERSION.SDK_INT <= 18) {
            File sourceFile = new File(sourcePath);
            File targetFile = new File(targetPath);
            if (!targetFile.exists()) {
                targetFile.mkdir();
            }
            for (String f : sourceFile.list()) {
                copySD(new File(sourceFile, f).getAbsolutePath(), new File(targetFile, f).getAbsolutePath(), backup, modID);
            }
        } else if (sourcePath.startsWith(externalPath) || targetPath.startsWith(externalPath)) {
            DocumentFile sourceFile = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
            DocumentFile targetFile = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
            if (sourcePath.startsWith(internalPath)) {
                sourceFile = DocumentFile.fromFile(new File(sourcePath));
            } else if (sourcePath.startsWith(externalPath)) {
                String sourceRelativePath = sourcePath.substring(externalPath.length() + 1, sourcePath.length());
                String[] sourceArray = sourceRelativePath.split("/", -1);
                for (String sourcePathSegment : sourceArray) {
                    sourceFile = sourceFile.findFile(sourcePathSegment);
                }
            }
            if (targetPath.startsWith(externalPath)) {
                String targetRelativePath = targetPath.substring(externalPath.length() + 1, targetPath.length());
                String[] targetArray = targetRelativePath.split("/", -1);
                for (String targetPathSegment : targetArray) {
                    DocumentFile tempTargetFile = targetFile.findFile(targetPathSegment);
                    if (tempTargetFile != null) {
                        targetFile = tempTargetFile;
                    } else {
                        targetFile.createDirectory(targetPathSegment);
                    }
                }
            }
            for (DocumentFile f : sourceFile.listFiles()) {
                System.out.println(sourcePath + "/" + f.getName() + ", " + targetPath + "/" + f.getName());
                copySD(sourcePath + "/" + f.getName(), targetPath + "/" + f.getName(), backup, modID);
            }
        }
    }

    public void copyFileSD(String sourcePath, String targetPath, boolean backup, String modID) throws IOException {
        String internalPath = Environment.getExternalStorageDirectory().toString();
        String externalPath = "External Path";

        if (treeUri != null) {
            externalPath = FileUtil.getFullPathFromTreeUri(treeUri, this);
            if (externalPath == null) {
                System.out.println("externalPath is null");
                return;
            }
        }

        InputStream in;
        OutputStream out;
        int DEFAULT_BUFFER_SIZE = 1024 * 4;
        if ((sourcePath.startsWith(internalPath) && targetPath.startsWith(internalPath)) || Build.VERSION.SDK_INT <= 18) {
            File sourceFile = new File(sourcePath);
            File targetFile = new File(targetPath);
            /*if (!sourceFile.exists()) {
                System.out.println("sourceFile not found!");
                log(getString(R.string.backup_lost));
                success = false;
                return;
            }*/
            if (!targetFile.exists()) {
                if (!targetFile.isDirectory() && !targetFile.getParentFile().exists()) {
                    if (targetFile.getParentFile().mkdirs()) {
                        System.out.println("Succeeded in creating parent directory of (" + targetPath + ")");
                    } else {
                        System.out.println("Failed to create parent directory of (" + targetPath + ")");
                    }
                } else if (targetFile.isDirectory()) {
                    if (targetFile.mkdirs()) {
                        System.out.println("Succeeded in creating directory (" + targetPath + ")");
                    } else {
                        System.out.println("Failed to create directory (" + targetPath + ")");
                    }
                }
            }/* else {
                if (backup) {
                    backupData(targetPath, modID);
                }
            }*/
            try {
                in = new FileInputStream(sourceFile);
                out = new FileOutputStream(targetFile);
                byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
                int length;
                while ((length = in.read(buf)) > 0) {
                    out.write(buf, 0, length);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (sourcePath.startsWith(externalPath) || targetPath.startsWith(externalPath)) {
            DocumentFile sourceFile = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
            DocumentFile targetFile = DocumentFile.fromTreeUri(getApplicationContext(), treeUri);
            String fileToCreate = "";
            if (sourcePath.startsWith(internalPath)) {
                sourceFile = DocumentFile.fromFile(new File(sourcePath));
            } else if (sourcePath.startsWith(externalPath)) {
                String sourceRelativePath = sourcePath.substring(externalPath.length() + 1, sourcePath.length());
                String[] sourceArray = sourceRelativePath.split("/", -1);
                for (String sourcePathSegment : sourceArray) {
                    sourceFile = sourceFile.findFile(sourcePathSegment);
                }
            }
            if (targetPath.startsWith(internalPath)) {
                targetFile = DocumentFile.fromFile(new File(targetPath));
                if (!targetFile.exists()) {
                    if (!targetFile.isDirectory() && !targetFile.getParentFile().exists()) {
                        if (new File(targetPath).getParentFile().mkdirs()) {
                            System.out.println("Succeeded in creating parent directory of (" + targetPath + ")");
                            fileToCreate = targetFile.getName();
                        } else {
                            System.out.println("Failed to create parent directory of (" + targetPath + ")");
                        }
                    } else if (targetFile.isDirectory()) {
                        if (new File(targetPath).mkdirs()) {
                            System.out.println("Succeeded in creating directory (" + targetPath + ")");
                        } else {
                            System.out.println("Failed to create directory (" + targetPath + ")");
                        }
                    }
                }
            } else if (targetPath.startsWith(externalPath)) {
                String targetRelativePath = targetPath.substring(externalPath.length() + 1, targetPath.length());
                String[] targetArray = targetRelativePath.split("/", -1);
                for (String targetPathSegment : targetArray) {
                    DocumentFile tempTargetFile = targetFile.findFile(targetPathSegment);
                    if (targetPathSegment.equals(targetArray[targetArray.length - 1]) && tempTargetFile == null) {
                        fileToCreate = targetPathSegment;
                    } else if (!targetPathSegment.equals(targetArray[targetArray.length - 1]) && tempTargetFile == null) {
                        targetFile.createDirectory(targetPathSegment);
                    } else {
                        targetFile = tempTargetFile;
                    }
                }
            }
            /*if (!sourceFile.exists()) {
                System.out.println("sourceFile not found!");
                log(getString(R.string.backup_lost));
                success = false;
                return;
            }*/
            in = getContentResolver().openInputStream(sourceFile.getUri());

            if (fileToCreate.equals("")) {
                /*if (backup) {
                    System.out.println("Start backup " + targetPath + " (" + modID + ")...");
                    backupData(targetPath, modID);
                }*/
                out = getContentResolver().openOutputStream(targetFile.getUri());
            } else {
                String targetMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(fileToCreate));
                if (targetPath.startsWith(internalPath)) {
                    out = new FileOutputStream(new File(targetPath));
                } else {
                    if (targetMimeType == null) {
                        out = getContentResolver().openOutputStream(targetFile.createFile("", fileToCreate).getUri());
                    } else {
                        out = getContentResolver().openOutputStream(targetFile.createFile(targetMimeType, getPrefix(fileToCreate)).getUri());
                    }
                }
            }
            try {
                byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
                int length;
                while ((length = in.read(buf)) > 0) {
                    out.write(buf, 0, length);
                }
                in.close();
                out.close();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Path format is incorrect!");
        }
    }

    /*// 元データをバックアップする
    public void backupData(String backupFilePath, String modID) {
        String internalPath = Environment.getExternalStorageDirectory().toString();
        String externalPath = "External Path";

        if (treeUri != null) {
            externalPath = FileUtil.getFullPathFromTreeUri(treeUri, this);
            if (externalPath == null) {
                System.out.println("externalPath is null");
                return;
            }
        }
        String backupFileRelativePath = "";
        if (backupFilePath.startsWith(internalPath)) {
            backupFileRelativePath = backupFilePath.substring(internalPath.length() + "/Android/data/net.wargaming.wot.blitz/files/".length(), backupFilePath.length());
        } else if (backupFilePath.startsWith(externalPath)) {
            backupFileRelativePath = backupFilePath.substring(externalPath.length() + "/Android/data/net.wargaming.wot.blitz/files/".length(), backupFilePath.length());
        }
        System.out.println("backupFileRelativePath: " + backupFileRelativePath);
        try {
            copySD(backupFilePath, getExternalCacheDir() + "/" + removeHttp(repoArray.get(currentRepo)) + "/" + modID + "/" + backupFileRelativePath, false, modID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    // 拡張子を取り除く
    public static String getPrefix(String fileName) {
        if (fileName == null)
            return null;
        int point = fileName.lastIndexOf(".");
        if (point != -1) {
            return fileName.substring(0, point);
        }
        return fileName;
    }

    // Modをインストール・削除
    public void installData(String modID) {
        handler.post(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(true);
            }
        });
        deleteSD(getExternalCacheDir() + "/Data");
        try {
            ZipFile zipFile = new ZipFile(getExternalCacheDir() + "/Data.zip");
            zipFile.extractAll(getExternalCacheDir().toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
        try {
            copySD(getExternalCacheDir() + "/Data", blitzPath, true, modID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler.post(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
    }

    /*// Modを削除
    public void removeData(String modID) {
        handler.post(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(true);
            }
        });
        try {
            copySD(getExternalCacheDir() + "/" + removeHttp(repoArray.get(currentRepo)) + "/" + modID + "/Data", blitzPath, false, modID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        deleteSD(getExternalCacheDir() + "/" + removeHttp(repoArray.get(currentRepo)) + "/" + modID + "/Data");
        handler.post(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
    }*/

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
            return removeHttp(repoArray.get(currentRepo)) + "." + getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        } else { // without repo name
            return getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
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

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
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
                System.out.println("File downloaded");
                //Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }
}