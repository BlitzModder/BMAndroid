package com.subdiox.blitzmodder;

import android.content.Context;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import io.github.luizgrp.sectionedrecyclerviewadapter.*;

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
    public static AlertDialog.Builder alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.pref_general, false);

        // get locale
        locale = Locale.getDefault().getLanguage();
        if (!(locale.equals("en")) && !(locale.equals("ja"))) {
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
                alertDialog.create().show();
            }
        });
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

    public void prepareAlertDialog() {
        // initialize alert dialog
        getUserSettings();
        alertDialog = new AlertDialog.Builder(this);
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
                    } else if (!buttonArray.contains(getFullID(true,i,j,k)) && installedArray.contains(getFullID(true,i,j,k))) {
                        applyNeeded = true;
                        if (!removeWritten) {
                            if (installWritten) {
                                message += "\n";
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
            alertDialog.setTitle(getResources().getString(R.string.confirm_title));
            alertDialog.setMessage(message);
            alertDialog.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // OK ボタンクリック処理
                        }
                    });
            alertDialog.setNegativeButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
        } else {
            alertDialog.setTitle(getResources().getString(R.string.no_changes_title));
            alertDialog.setMessage(getResources().getString(R.string.no_changes_message));
            alertDialog.setPositiveButton(
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

    private static void checkBlitzExists() {

    }

    // download from url and save as a file
    private static void download(final String urlString, final String path, final Context c) {
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
        File file = new File(getFilesDir(),FileName);
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
        download("https://github.com/" + repoArray.get(0) + "/BMRepository/raw/master/" + locale + ".plist", getFilesDir() + "/" + repoArray.get(0) + "_" + locale + ".plist", getApplicationContext());
        while (!downloadFinished) {}

        PListXMLParser parser = new PListXMLParser();
        PListXMLHandler handler = new PListXMLHandler();
        parser.setHandler(handler);
        try {
            parser.parse(readFile(repoArray.get(0) + "_" + locale + ".plist"));
            PList actualPList = ((PListXMLHandler) parser.getHandler()).getPlist();
            LinkedHashMap<String,PListObject> modCategoryPlist = ((Dict)actualPList.getRootElement()).getConfigMap();
            modCategoryArray = new ArrayList<String>(modCategoryPlist.keySet());
            modNameArray = new ArrayList<ArrayList<String>>();
            modDetailArray = new ArrayList<ArrayList<ArrayList<String>>>();
            for(int i = 0; i < modCategoryArray.size(); i++) {
                LinkedHashMap<String,PListObject> modNamePlist = ((Dict)modCategoryPlist.get(modCategoryArray.get(i))).getConfigMap();
                modNameArray.add(new ArrayList<String>(modNamePlist.keySet()));
                ArrayList<ArrayList<String>> tempArray = new ArrayList<ArrayList<String>>();
                for (int j = 0; j < modNameArray.get(i).size(); j++) {
                    LinkedHashMap<String,PListObject> modDetailPlist = ((Dict)modNamePlist.get(modNameArray.get(i).get(j))).getConfigMap();
                    tempArray.add(new ArrayList<String>(modDetailPlist.keySet()));
                }
                modDetailArray.add(tempArray);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public String getID(String string) {
        String[] array = string.split(":",-1);
        if (array.length == 2) {
            return array[1];
        } else {
            return "error";
        }
    }

    public String getName(String string) {
        String[] array = string.split(":",-1);
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