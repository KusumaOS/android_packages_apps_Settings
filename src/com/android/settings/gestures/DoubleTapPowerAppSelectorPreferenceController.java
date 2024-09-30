/*
 * Copyright (C) 2019 The Dirty Unicorns Project
 *               2024 Kusuma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.os.UserHandle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DoubleTapPowerAppSelectorPreferenceController extends AbstractPreferenceController 
        implements SelectorWithWidgetPreference.OnClickListener, LifecycleObserver,
        OnResume, OnPause, PreferenceControllerMixin {

    private static final String TAG = "DoubleTapPowerAppSelectorPreferenceController";

    private final Context mContext;
    private final String KEY = "power_double_tap_app_screen";

    private PackageManager packageManager;
    private List<ApplicationInfo> appList;
    private String selectedPackageName;

    private PreferenceScreen mScreen;
    private SettingObserver mSettingObserver;

    public DoubleTapPowerAppSelectorPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        packageManager = mContext.getPackageManager();
        appList = getLaunchableApps();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        loadAppList();
        mSettingObserver = new SettingObserver(screen);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        selectedPackageName = preference.getKey();
        String friendlyAppString = preference.getTitle().toString();

        Settings.System.putStringForUser(mContext.getContentResolver(),
                Settings.System.POWER_DOUBLE_TAP_APP_ACTION, selectedPackageName, UserHandle.USER_CURRENT);

        Settings.System.putStringForUser(mContext.getContentResolver(),
                Settings.System.POWER_DOUBLE_TAP_APP_FR_ACTION, friendlyAppString, UserHandle.USER_CURRENT);

        if (mScreen != null) {
            for (int i = 0; i < mScreen.getPreferenceCount(); i++) {
                Preference pref = mScreen.getPreference(i);
                updateState(pref);
            }
        }
    }

    @Override
    public void updateState(Preference preference) {
        String selectedKey = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.POWER_DOUBLE_TAP_APP_ACTION);

        if (preference instanceof SelectorWithWidgetPreference) {
            SelectorWithWidgetPreference pref = (SelectorWithWidgetPreference) preference;
            pref.setChecked(pref.getKey().equals(selectedKey));
        }
    }

    private List<ApplicationInfo> getLaunchableApps() {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(launchIntent, 0);
        return resolveInfos.parallelStream()
                .map(resolveInfo -> resolveInfo.activityInfo.applicationInfo)
                .distinct()
                .sorted(Comparator.comparing(app -> app.loadLabel(packageManager).toString().toLowerCase()))
                .collect(Collectors.toList());
    }

    private void loadAppList() {
        if (appList != null && !appList.isEmpty() && mScreen != null) {
            mScreen.removeAll();
            for (ApplicationInfo app : appList) {
                SelectorWithWidgetPreference appPreference = new SelectorWithWidgetPreference(mContext);
                appPreference.setTitle(app.loadLabel(packageManager));
                appPreference.setIcon(app.loadIcon(packageManager));
                appPreference.setKey(app.packageName);
                appPreference.setSummary(app.packageName);
                appPreference.setChecked(app.packageName.equals(selectedPackageName));
                appPreference.setOnClickListener(this);
                updateState(appPreference);
                mScreen.addPreference(appPreference);
            }
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Uri URI_POWER_DOUBLE_TAP_APP_ACTION = 
                Settings.System.getUriFor(Settings.System.POWER_DOUBLE_TAP_APP_ACTION);
        private final Uri URI_POWER_DOUBLE_TAP_APP_FR_ACTION = 
                Settings.System.getUriFor(Settings.System.POWER_DOUBLE_TAP_APP_FR_ACTION);

        private final PreferenceScreen mScreen;

        SettingObserver(PreferenceScreen screen) {
            super(Handler.getMain());
            this.mScreen = screen;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(URI_POWER_DOUBLE_TAP_APP_ACTION, false, this);
            cr.registerContentObserver(URI_POWER_DOUBLE_TAP_APP_FR_ACTION, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || URI_POWER_DOUBLE_TAP_APP_ACTION.equals(uri) || URI_POWER_DOUBLE_TAP_APP_FR_ACTION.equals(uri)) {
                if (mScreen != null) {
                    for (int i = 0; i < mScreen.getPreferenceCount(); i++) {
                        Preference pref = mScreen.getPreference(i);
                        updateState(pref);
                    }
                }
            }
        }
    }
}
