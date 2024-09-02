/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.settings.gestures;

import android.content.Context;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import lineageos.providers.LineageSettings;

public class QuickPulldownMainSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnMainSwitchChangeListener {

    private static final String KEY = "quick_pulldown_main_switch";
    private final Context mContext;

    MainSwitchPreference mSwitch;

    public QuickPulldownMainSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            Preference pref = screen.findPreference(getPreferenceKey());
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    int quickPulldown = LineageSettings.System.getInt(mContext.getContentResolver(),
                            LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 0);
                    boolean isChecked = quickPulldown != 0;
                    LineageSettings.System.putInt(mContext.getContentResolver(),
                            LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, isChecked ? 0 : 1);
                    return true;
                });
                mSwitch = (MainSwitchPreference) pref;
                mSwitch.addOnSwitchChangeListener(this);
                updateState(mSwitch);
            }
        }
    }

    public void setChecked(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.updateStatus(isChecked);
        }
    }

    @Override
    public void updateState(Preference preference) {
        int quickPulldownSetting = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 0);
        setChecked(quickPulldownSetting != 0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        int newQuickPulldownSetting = isChecked ? 1 : 0;
        LineageSettings.System.putInt(mContext.getContentResolver(),
                LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, newQuickPulldownSetting);
    }
}
