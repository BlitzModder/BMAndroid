package com.subdiox.blitzmodder;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

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

    public static String[] repoArray = {"BlitzModder"};
    public static ArrayList<String> modCategoryArray;
    public static ArrayList<ArrayList<String>> modNameArray;
    public static ArrayList<ArrayList<ArrayList<String>>> modDetailArray;
    public static String locale;
    public static boolean downloadFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get locale
        locale = Locale.getDefault().getLanguage();

        // initialize sectionedRecyclerViewAdapter
        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();

        // load mods list
        refreshModsList();

        // initialize sections
        for (int i = 0; i < modCategoryArray.size(); i++) {
            sectionAdapter.addSection(new ModSection(modCategoryArray.get(i), modNameArray.get(i)));
        }

        // initialize recyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        download("https://github.com/" + repoArray[0] + "/BMRepository/raw/master/" + locale + ".plist", getFilesDir() + "/" + repoArray[0] + "_" + locale + ".plist", getApplicationContext());
        while (!downloadFinished) {}

        PListXMLParser parser = new PListXMLParser();
        PListXMLHandler handler = new PListXMLHandler();
        parser.setHandler(handler);
        try {
            parser.parse(readFile(repoArray[0] + "_" + locale + ".plist"));
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
}

class ModSection extends StatelessSection {
    String title;
    List<String> list;

    public ModSection(String title, List<String> list) {
        super(R.layout.section_header, R.layout.section_item);
        this.title = title;
        this.list = list;
    }

    @Override
    public int getContentItemsTotal() {
        return list.size();
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new HeaderViewHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
        headerHolder.modCategory.setText(getName(title));
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ItemViewHolder itemHolder = (ItemViewHolder) holder;
        itemHolder.modName.setText(getName(list.get(position)));
    }

    public String getName(String string) {
        String[] array = string.split(":",-1);
        if (array.length == 2) {
            return array[0];
        } else {
            return "error";
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
        modName = (TextView)itemView.findViewById(R.id.modName);
    }
}
