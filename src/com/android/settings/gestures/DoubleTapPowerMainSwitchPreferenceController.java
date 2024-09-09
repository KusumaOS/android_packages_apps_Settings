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
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

public class DoubleTapPowerMainSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnMainSwitchChangeListener {

    private static final String KEY = "double_tap_power_main_switch";
    private final Context mContext;

    MainSwitchPreference mSwitch;

    public DoubleTapPowerMainSwitchPreferenceController(Context context) {
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
                    String doubleTapPower = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                            Settings.Secure.POWER_DOUBLE_TAP_ACTION, UserHandle.USER_CURRENT);
                    boolean isChecked = !"none".equals(doubleTapPower);
                    Settings.Secure.putStringForUser(mContext.getContentResolver(),
                            Settings.Secure.POWER_DOUBLE_TAP_ACTION, isChecked ? "none" : "camera",
                            UserHandle.USER_CURRENT);
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
        String doubleTapPower = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.POWER_DOUBLE_TAP_ACTION, UserHandle.USER_CURRENT);
        setChecked(!"none".equals(doubleTapPower));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        String newDoubleTapPower = isChecked ? "camera" : "none";
        Settings.Secure.putStringForUser(mContext.getContentResolver(),
                Settings.Secure.POWER_DOUBLE_TAP_ACTION, newDoubleTapPower,
                UserHandle.USER_CURRENT);
    }
}
