package com.subdiox.blitzmodder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class SettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SectionedRecyclerViewAdapter sectionAdapter;

    @Override
    protected void onCreate(Bundle saveduserSettingsState) {
        super.onCreate(saveduserSettingsState);
        setContentView(R.layout.activity_settings);
        sectionAdapter = new SectionedRecyclerViewAdapter();
        for (int i = 0; i < 5; i ++) {
            sectionAdapter.addSection(new SettingsSection(i));
        }
        // initialize recyclerView
        recyclerView = (RecyclerView) findViewById(R.id.settingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);

        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.act_open_enter_anim, R.anim.act_open_exit_anim);
            }
        });
    }

    class SettingsSection extends StatelessSection {
        int section;

        public SettingsSection(int section) {
            super(R.layout.section_header_settings, R.layout.section_item_settings);

            this.section = section;
        }

        @Override
        public int getContentItemsTotal() {
            if (section == 0) {
                return 1;
            } else if (section == 1) {
                return 1;
            } else if (section == 2) {
                return 1;
            } else if (section == 3) {
                return 3;
            } else if (section == 4) {
                return 2;
            }
            return 0;
        }

        @Override
        public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
            return new com.subdiox.blitzmodder.SettingsHeaderViewHolder(view);
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new com.subdiox.blitzmodder.SettingsItemViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
            com.subdiox.blitzmodder.SettingsHeaderViewHolder headerHolder = (com.subdiox.blitzmodder.SettingsHeaderViewHolder) holder;
            if (section == 0) {
                headerHolder.settingsCategory.setText(getString(R.string.language));
            } else if (section == 1) {
                headerHolder.settingsCategory.setText(getString(R.string.storage));
            } else if (section == 2) {
                headerHolder.settingsCategory.setText(getString(R.string.repository));
            } else if (section == 3) {
                headerHolder.settingsCategory.setText(getString(R.string.trouble_shooting));
            } else if (section == 4) {
                headerHolder.settingsCategory.setText(getString(R.string.contact));
            }
        }

        public void setLocale(String lang) {
            Locale myLocale = new Locale(lang);
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = myLocale;
            res.updateConfiguration(conf, dm);
            Intent refresh = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(refresh);
            finish();
        }

        public void resetBlitzPath() {
            UserSettings userSettings = UserSettings.getInstance(getApplicationContext());
            userSettings.blitzPath = "";
            userSettings.saveInstance(getApplicationContext());
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
            alertDialogBuilder.setTitle(getResources().getString(R.string.notice));
            alertDialogBuilder.setMessage(getResources().getString(R.string.reset_internal_message));
            alertDialogBuilder.setPositiveButton(
                    getResources().getString(R.string.okay),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final com.subdiox.blitzmodder.SettingsItemViewHolder itemHolder = (com.subdiox.blitzmodder.SettingsItemViewHolder) holder;
            final UserSettings userSettings = UserSettings.getInstance(getApplicationContext());
            if (section == 0) {
                if (position == 0) {
                    String locales[] = {"English", "日本語", "русский", "简体中文"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, locales);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    itemHolder.spinner.setAdapter(adapter);
                    itemHolder.spinner.setVisibility(View.VISIBLE);
                    final ArrayList<String> languageArray = new ArrayList<>();
                    languageArray.addAll(Arrays.asList("en", "ja", "ru", "zh"));
                    int localeIndex = languageArray.indexOf(userSettings.locale);
                    System.out.println("localeIndex: " + localeIndex);
                    if (localeIndex != -1) {
                        itemHolder.spinner.setSelection(localeIndex, false);
                    } else {
                        userSettings.locale = "en";
                        userSettings.saveInstance(getApplicationContext());
                        itemHolder.spinner.setSelection(0, false);
                    }
                    itemHolder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            System.out.println("Item Selcted: " + position);
                            userSettings.locale = languageArray.get(position);
                            setLocale(userSettings.locale);
                            userSettings.saveInstance(getApplicationContext());
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                    itemHolder.settingsText.setVisibility(View.GONE);
                    itemHolder.settingsDetailText.setVisibility(View.GONE);
                }
            } else if (section == 1) {
                if (position == 0) {
                    itemHolder.settingsText.setText(getString(R.string.storage_title));
                    if (userSettings.internal) {
                        itemHolder.settingsDetailText.setText(getString(R.string.internal_full));
                    } else {
                        itemHolder.settingsDetailText.setText(getString(R.string.external_full));
                    }
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
                            alertDialogBuilder.setTitle(getResources().getString(R.string.warning));
                            alertDialogBuilder.setMessage(getResources().getString(R.string.confirm_reset_storage));
                            alertDialogBuilder.setPositiveButton(
                                    getResources().getString(R.string.okay),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            resetBlitzPath();
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
            } else if (section == 2) {
                if (position == 0) {
                    itemHolder.settingsText.setText(getString(R.string.repository_settings));
                    itemHolder.settingsDetailText.setText(getString(R.string.repository_settings_detail));
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getApplicationContext(), RepoActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.act_open_enter_anim, R.anim.act_open_exit_anim);
                        }
                    });
                }
            } else if (section == 3) {
                if (position == 0) {
                    itemHolder.settingsText.setText(getString(R.string.website));
                    itemHolder.settingsDetailText.setText(getString(R.string.website_detail));
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://subdiox.com/blitzmodder"));
                            startActivity(intent);
                        }
                    });
                } else if (position == 1) {
                    itemHolder.settingsText.setText(getString(R.string.reset_installation));
                    itemHolder.settingsDetailText.setText(getString(R.string.reset_installation_detail));
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
                            alertDialogBuilder.setTitle(getResources().getString(R.string.warning));
                            alertDialogBuilder.setMessage(getResources().getString(R.string.reset_installation_message));
                            alertDialogBuilder.setPositiveButton(
                                    getResources().getString(R.string.okay),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            userSettings.installedArray = new ArrayList<>();
                                            userSettings.saveInstance(getApplicationContext());
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
                } else if (position == 2) {
                    itemHolder.settingsText.setText(getString(R.string.reset_all_settings));
                    itemHolder.settingsText.setTextColor(Color.RED);
                    itemHolder.settingsDetailText.setText(getString(R.string.reset_all_settings_detail));
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
                            alertDialogBuilder.setTitle(getResources().getString(R.string.warning));
                            alertDialogBuilder.setMessage(getResources().getString(R.string.reset_all_settings_message));
                            alertDialogBuilder.setPositiveButton(
                                    getResources().getString(R.string.okay),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            userSettings.repoArray = new ArrayList<>();
                                            userSettings.repoArray.add("http://subdiox.com/repo");
                                            userSettings.currentRepo = 0;
                                            userSettings.buttonArray = new ArrayList<>();
                                            userSettings.installedArray = new ArrayList<>();
                                            userSettings.blitzPath = "";
                                            userSettings.internal = true;
                                            userSettings.treeUriString = "";
                                            userSettings.locale = "";
                                            userSettings.repoNameArray = new ArrayList<>();
                                            userSettings.repoNameArray.add("BlitzModder");
                                            userSettings.repoVersionArray = new ArrayList<>();
                                            userSettings.repoVersionArray.add("0.0.0");
                                            userSettings.saveInstance(getApplicationContext());
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
            } else if (section == 4) {
                if (position == 0) {
                    itemHolder.settingsText.setText(getString(R.string.bug_report));
                    itemHolder.settingsText.setTextColor(Color.BLUE);
                    itemHolder.settingsDetailText.setText(getString(R.string.bug_report_detail));
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("message/rfc822");
                            intent.putExtra(Intent.EXTRA_EMAIL  , new String[]{"subdiox@gmail.com"});
                            intent.putExtra(Intent.EXTRA_SUBJECT, "[BlitzModder for Android] Feedback");
                            String osVersion = android.os.Build.VERSION.RELEASE;
                            String model = android.os.Build.MODEL;
                            String appVersion = "unknown";
                            try {
                                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                appVersion = pInfo.versionName;
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            UserSettings userSettings = UserSettings.getInstance(getApplicationContext());
                            String deviceInfo = "[Device Information]\nAndroid OS version: " + osVersion + "\nDevice Model Name: " + model + "\nApplication Version: " + appVersion + "\n\n";
                            String storageName;
                            if (userSettings.internal) {
                                storageName = "Internal";
                            } else {
                                storageName = "External";
                            }
                            String appSettings = "[Application Settings]\nInstalled Mods: " + userSettings.installedArray + "\nStorage: " + storageName + "\nBlitz Path: " + userSettings.blitzPath + "\nRepo Array: " + userSettings.repoArray;
                            String feedback = "\n\n[Feedback Here]";
                            String bodyText = deviceInfo + appSettings + feedback;
                            intent.putExtra(Intent.EXTRA_TEXT, bodyText);
                            try {
                                startActivity(Intent.createChooser(intent, "Choose application to send email"));
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(getApplicationContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else if (position == 1) {
                    itemHolder.settingsText.setText(getString(R.string.donate));
                    itemHolder.settingsDetailText.setText(getString(R.string.donate_detail));
                    itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://subdiox.com/blitzmodder/contact.html"));
                            startActivity(intent);
                        }
                    });
                }
            }
        }
    }
}

class SettingsHeaderViewHolder extends RecyclerView.ViewHolder {
    public final TextView settingsCategory;
    public SettingsHeaderViewHolder(View view) {
        super(view);
        settingsCategory = (TextView)itemView.findViewById(R.id.settingsCategory);
    }
}

class SettingsItemViewHolder extends RecyclerView.ViewHolder {
    public final TextView settingsText;
    public final TextView settingsDetailText;
    public final ImageView checkMark;
    public final Spinner spinner;

    public SettingsItemViewHolder(View view) {
        super(view);
        settingsText = (TextView) itemView.findViewById(R.id.settingsText);
        settingsDetailText = (TextView) itemView.findViewById(R.id.settingsDetailText);
        checkMark = (ImageView) itemView.findViewById(R.id.check);
        spinner = (Spinner) itemView.findViewById(R.id.locale_spinner);
        spinner.setVisibility(View.GONE);
    }
}