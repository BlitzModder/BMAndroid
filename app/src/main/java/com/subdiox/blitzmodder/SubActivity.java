package com.subdiox.blitzmodder;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.content.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.luizgrp.sectionedrecyclerviewadapter.*;

public class SubActivity extends AppCompatActivity {

    public static BMApplication app;
    public static UserSettings userSettings;
    public static ArrayList<String> repoArray;
    public static ArrayList<String> repoNameArray;
    public static int currentRepo;
    public static int section;
    public static int position;
    public static ArrayList<String> buttonArray;
    public static ArrayList<String> installedArray;
    public static ArrayList<String> modCategoryArray;
    public static ArrayList<ArrayList<String>> modNameArray;
    public static ArrayList<ArrayList<ArrayList<String>>> modDetailArray;
    public static String locale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.act_close_enter_anim, R.anim.act_close_exit_anim);
            }
        });

        // get locale
        locale = Locale.getDefault().getLanguage();

        // initialize sectionedRecyclerViewAdapter
        final SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();

        // get application variables
        app = (BMApplication)this.getApplication();
        modCategoryArray = app.modCategoryArray;
        modNameArray = app.modNameArray;
        modDetailArray = app.modDetailArray;

        // get intent data
        Intent intent = getIntent();
        section = intent.getIntExtra("section", 0);
        position = intent.getIntExtra("position", 0);

        // get user settings
        getUserSettings();

        // set title
        TextView titleView = (TextView)findViewById(R.id.textview_title);
        titleView.setText(getName(modNameArray.get(section).get(position)));

        // initialize section
        sectionAdapter.addSection(new ModDetailSection());

        // initialize recyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.subRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);
    }

    public void getUserSettings() {
        // get preference variables
        userSettings = UserSettings.getInstance(getApplicationContext());
        repoArray = userSettings.repoArray;
        repoNameArray = userSettings.repoNameArray;
        currentRepo = userSettings.currentRepo;
        buttonArray = userSettings.buttonArray;
        installedArray = userSettings.installedArray;
    }

    public void saveUserSettings() {
        // save preference variables
        userSettings.repoArray = repoArray;
        userSettings.repoNameArray = repoNameArray;
        userSettings.currentRepo = currentRepo;
        userSettings.buttonArray = buttonArray;
        userSettings.installedArray = installedArray;
        userSettings.saveInstance(getApplicationContext());
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

    public String getFullID(boolean b, int i, int j, int k) {
        if (b) { // with repo name
            return repoNameArray.get(currentRepo) + "." + getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        } else { // without repo name
            return getID(modCategoryArray.get(i)) + "." + getID(modNameArray.get(i).get(j)) + "." + getID(modDetailArray.get(i).get(j).get(k));
        }
    }

    class ModDetailSection extends StatelessSection {
        List<String> list;

        public ModDetailSection() {
            super(R.layout.section_header_sub, R.layout.section_item_sub);
            this.list = modDetailArray.get(section).get(position);
        }

        @Override
        public int getContentItemsTotal() {
            return list.size();
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new SubItemViewHolder(view);
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int subPosition) {
            final SubItemViewHolder itemHolder = (SubItemViewHolder) holder;
            getUserSettings();
            itemHolder.modDetail.setText(getName(list.get(subPosition)));
            if (installedArray.contains(getFullID(true, section, position, subPosition))) {
                itemHolder.installedText.setText(getResources().getString(R.string.installed));
                itemHolder.installedText.setTextColor(Color.BLUE);
            } else {
                itemHolder.installedText.setText(getResources().getString(R.string.not_installed));
                itemHolder.installedText.setTextColor(Color.GRAY);
            }
            itemHolder.installCheckBox.setOnCheckedChangeListener(null);
            itemHolder.installCheckBox.setChecked(buttonArray.contains(getFullID(true, section, position, subPosition)));
            itemHolder.installCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        buttonArray.add(getFullID(true, section, position, subPosition));
                    } else {
                        buttonArray.remove(getFullID(true, section, position, subPosition));
                    }
                    itemHolder.installCheckBox.setChecked(isChecked);
                    saveUserSettings();
                }
            });

            itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupWindow mPopupWindow = new PopupWindow(getApplicationContext());
                    final View popupView = getLayoutInflater().inflate(R.layout.popup_web, null);
                    mPopupWindow.setContentView(popupView);
                    mPopupWindow.setOutsideTouchable(true);
                    mPopupWindow.setFocusable(true);
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int screenWidth = size.x;
                    int screenHeight = size.y;
                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenWidth / 4, getResources().getDisplayMetrics());
                    float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenHeight / 4, getResources().getDisplayMetrics());
                    mPopupWindow.setWidth((int) width);
                    mPopupWindow.setHeight((int) height);
                    mPopupWindow.showAtLocation(itemHolder.itemView, Gravity.NO_GRAVITY, 150, 300);
                    WebView webview = (WebView)popupView.findViewById(R.id.webview);
                    webview.loadUrl(repoArray.get(currentRepo) + "/detail/html/" + getFullID(false, section, position, subPosition) + ".html");
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
}

class SubItemViewHolder extends RecyclerView.ViewHolder {
    public final TextView modDetail;
    public final TextView installedText;
    public final CheckBox installCheckBox;
    public SubItemViewHolder(View view) {
        super(view);
        modDetail = (TextView)itemView.findViewById(R.id.modDetail);
        installedText = (TextView)itemView.findViewById(R.id.installedText);
        installCheckBox = (CheckBox)itemView.findViewById(R.id.installCheckBox);
    }
}
