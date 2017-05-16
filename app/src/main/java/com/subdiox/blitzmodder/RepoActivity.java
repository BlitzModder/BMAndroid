package com.subdiox.blitzmodder;

import android.accounts.NetworkErrorException;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.longevitysoft.android.xml.plist.PListXMLHandler;
import com.longevitysoft.android.xml.plist.PListXMLParser;
import com.longevitysoft.android.xml.plist.domain.Dict;
import com.longevitysoft.android.xml.plist.domain.PList;
import com.longevitysoft.android.xml.plist.domain.PListObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class RepoActivity extends AppCompatActivity {

    public static BMApplication app;
    public static UserSettings userSettings;
    public static ArrayList<String> repoArray;
    public static ArrayList<String> repoNameArray;
    public static ArrayList<String> repoVersionArray;
    public static ArrayList<String> languageArray;
    public static SectionedRecyclerViewAdapter sectionAdapter;
    public static RecyclerView recyclerView;
    public static int currentRepo;
    public static String locale;
    public static String downloadPath;
    public static Handler handler;
    public static boolean fileFound;
    public static boolean connectionError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo);
        handler = new Handler();

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.act_close_enter_anim, R.anim.act_close_exit_anim);
            }
        });

        findViewById(R.id.addRepo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editView = new EditText(RepoActivity.this);
                new AlertDialog.Builder(RepoActivity.this)
                        .setIcon(null)
                        .setTitle(getString(R.string.add_repository))
                        .setView(editView)
                        .setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String repoText = editView.getText().toString();
                                String repo = getFullRepo(repoText);
                                if (checkRepo(repo)) {
                                    if (getRepoInfo(repo)) {
                                        saveUserSettings();
                                    }
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            }
        });

        // get locale
        locale = Locale.getDefault().getLanguage();

        // initialize sectionedRecyclerViewAdapter
        sectionAdapter = new SectionedRecyclerViewAdapter();

        // get application variables
        app = (BMApplication)this.getApplication();

        // get user settings
        getUserSettings();

        // initialize section
        sectionAdapter.addSection(new RepoSection());

        // initialize recyclerView
        recyclerView = (RecyclerView) findViewById(R.id.repoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);
    }

    public boolean checkRepo(String repo) {
        ArrayList<String> tryArray = languageArray;
        int index = tryArray.indexOf(locale);
        if (index != -1) {
            tryArray.set(index, tryArray.get(0));
            tryArray.set(0, locale);
        }
        for (String tryLocale : tryArray) {
            String plistPath = getCacheDir() + "/" + removeHttp(repo) + "/plist/" + tryLocale + ".plist";
            File file = new File(plistPath);
            if (!file.getParentFile().exists()) {
                boolean result = file.getParentFile().mkdirs();
                if (result) {
                    System.out.println("Succeeded in creating directory!");
                } else {
                    Toast.makeText(this, "Failed to create directory.", Toast.LENGTH_LONG).show();
                }
            }
            try {
                downloadPath = getCacheDir() + "/" + removeHttp(repo) + "/plist/" + tryLocale + ".plist";
                System.out.println(repo + "/plist/" + tryLocale + ".plist");
                new RepoActivity.DownloadTask(this).execute(repo + "/plist/" + tryLocale + ".plist").get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            if (fileFound) {
                return true;
            } else if (tryLocale.equals(tryArray.get(tryArray.size() - 1))) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setTitle(getResources().getString(R.string.error));
                alertDialogBuilder.setMessage(getResources().getString(R.string.invalid_repository));
                alertDialogBuilder.setPositiveButton(
                        getResources().getString(R.string.okay),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
                return false;
            }
        }
        return false;
    }

    public boolean getRepoInfo(String repo) {
        String plistPath = getCacheDir() + "/" + removeHttp(repo) + "/info.plist";
        File file = new File(plistPath);
        if (!file.getParentFile().exists()) {
            boolean result = file.getParentFile().mkdirs();
            if (result) {
                System.out.println("Succeeded in creating directory!");
            } else {
                Toast.makeText(this, "Failed to create directory.", Toast.LENGTH_LONG).show();
            }
        }
        try {
            downloadPath = plistPath;
            new RepoActivity.DownloadTask(this).execute(repo + "/info.plist").get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        if (fileFound) {
            PListXMLParser parser = new PListXMLParser();
            PListXMLHandler handler = new PListXMLHandler();
            parser.setHandler(handler);
            try {
                if (new File(plistPath).exists()) {
                    parser.parse(readFile(plistPath));
                    PList actualPList = ((PListXMLHandler) parser.getHandler()).getPlist();
                    LinkedHashMap<String, PListObject> infoPlist = ((Dict) actualPList.getRootElement()).getConfigMap();
                    String name = ((com.longevitysoft.android.xml.plist.domain.String) infoPlist.get("name")).getValue();
                    repoArray.add(repo);
                    repoNameArray.add(name);
                    repoVersionArray.add("0.0.0");
                    sectionAdapter.notifyDataSetChanged();
                    return true;
                } else {
                    return false;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setTitle(getResources().getString(R.string.error));
            alertDialogBuilder.setMessage(getResources().getString(R.string.invalid_repository));
            alertDialogBuilder.setPositiveButton(
                    getResources().getString(R.string.okay),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
            return false;
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

    public void getUserSettings() {
        // get preference variables
        userSettings = UserSettings.getInstance(getApplicationContext());
        repoArray = userSettings.repoArray;
        currentRepo = userSettings.currentRepo;
        repoNameArray = userSettings.repoNameArray;
        repoVersionArray = userSettings.repoVersionArray;
    }

    public void saveUserSettings() {
        // save preference variables
        userSettings.repoArray = repoArray;
        userSettings.currentRepo = currentRepo;
        userSettings.repoNameArray = repoNameArray;
        userSettings.repoVersionArray = repoVersionArray;
        userSettings.saveInstance(getApplicationContext());
    }

    private String removeHttp(String string) {
        if (string.startsWith("http://")) {
            return string.substring("http://".length(), string.length()).replaceAll("/","_");
        } else if (string.startsWith("https://")) {
            return string.substring("https://".length(), string.length()).replaceAll("/","_");
        } else {
            return string.replaceAll("/","_");
        }
    }

    private String getFullRepo(String string) {
        String repo;
        if (string.startsWith("http://") || string.startsWith("https://")) {
            if (string.endsWith("/")) {
                repo = string.substring(0, string.length() - 1);
            } else {
                repo = string;
            }
        } else {
            String[] array = string.split("/");
            if (array.length == 1) {
                repo = "http://" + array[0] + ".github.io/BMRepository";
            } else if (array.length == 2) {
                repo = "http://" + array[0] + ".github.io/" + array[1];
            } else {
                repo = "error";
            }
        }
        return repo;
    }

    class RepoSection extends StatelessSection {

        public RepoSection() {
            super(R.layout.section_header_repo, R.layout.section_item_repo);
        }

        @Override
        public int getContentItemsTotal() {
            return repoArray.size();
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new RepoItemViewHolder(view);
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final RepoItemViewHolder itemHolder = (RepoItemViewHolder) holder;
            itemHolder.repoURL.setText(repoArray.get(position));
            itemHolder.repoName.setText(repoNameArray.get(position));
            if (position == 0) {
                itemHolder.deleteButton.setVisibility(View.INVISIBLE);
            }
            itemHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RepoActivity.this);
                    alertDialogBuilder.setTitle(getResources().getString(R.string.notice));
                    alertDialogBuilder.setMessage(getResources().getString(R.string.delete_repository));
                    alertDialogBuilder.setPositiveButton(
                            getResources().getString(R.string.okay),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    repoArray.remove(position);
                                    repoNameArray.remove(position);
                                    repoVersionArray.remove(position);
                                    sectionAdapter.notifyDataSetChanged();
                                    saveUserSettings();
                                }
                            });
                    alertDialogBuilder.setNegativeButton(
                            getResources().getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            });
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        } else {
            finish();
            overridePendingTransition(R.anim.act_close_enter_anim, R.anim.act_close_exit_anim);
            return false;
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
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
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

class RepoItemViewHolder extends RecyclerView.ViewHolder {
    public final TextView repoName;
    public final TextView repoURL;
    public final Button deleteButton;
    public RepoItemViewHolder(View view) {
        super(view);
        repoName = (TextView)itemView.findViewById(R.id.repoName);
        repoURL = (TextView)itemView.findViewById(R.id.repoURL);
        deleteButton = (Button)itemView.findViewById(R.id.deleteButton);
    }
}
