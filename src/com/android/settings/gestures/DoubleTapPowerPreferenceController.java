/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class DoubleTapPowerPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";

    private PrimarySwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    public DoubleTapPowerPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return !isGestureAvailable(context)
                || prefs.getBoolean(DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    private static boolean isGestureAvailable(Context context) {
        return context.getResources()
                .getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_double_tap_power");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean isChecked() {
        String currentAction = Settings.Secure.getStringForUser(
                mContext.getContentResolver(), Settings.Secure.POWER_DOUBLE_TAP_ACTION,
                UserHandle.USER_CURRENT);
        if (currentAction == null) {
            currentAction = "camera";
        }
        return !TextUtils.equals(currentAction, "none");
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        String newAction = isChecked ? "camera" : "none";
        return Settings.Secure.putStringForUser(mContext.getContentResolver(), 
                Settings.Secure.POWER_DOUBLE_TAP_ACTION, newAction, 
                UserHandle.USER_CURRENT);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        String currentAction = Settings.Secure.getStringForUser(
                mContext.getContentResolver(), Settings.Secure.POWER_DOUBLE_TAP_ACTION,
                UserHandle.USER_CURRENT);
        if (currentAction == null) {
            currentAction = "camera";
        }
        String summary = "";
        switch (currentAction) {
            case "none":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_none);
                break;
            case "camera":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_camera);
                break;
            case "assistant":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_assistant);
                break;
            case "togglemedia":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_toggle_media);
                break;
            case "qr":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_qr);
                break;
            case "wallet":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_wallet);
                break;
            case "devicecontrol":
                summary = mContext.getResources().getString(
                        R.string.custom_gesture_action_name_device_control);
                break;
            case "customapp":
                String appSummary = Settings.System.getStringForUser(mContext.getContentResolver(), 
                        Settings.System.POWER_DOUBLE_TAP_APP_FR_ACTION, UserHandle.USER_CURRENT);
                if (appSummary != null) {
                    summary = mContext.getResources().getString(
                            R.string.custom_gesture_action_name_app_open) + " " + appSummary;
                } else {
                    summary = mContext.getResources().getString(
                            R.string.custom_gesture_action_name_app_unavailable);
                }
                break;
        }
        preference.setSummary(summary);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Uri URI_POWER_DOUBLE_TAP_ACTION = Settings.Secure.getUriFor(
                Settings.Secure.POWER_DOUBLE_TAP_ACTION);
        private final Uri URI_POWER_DOUBLE_TAP_APP_FR_ACTION = Settings.System.getUriFor(
                Settings.System.POWER_DOUBLE_TAP_APP_FR_ACTION);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(URI_POWER_DOUBLE_TAP_ACTION, false, this);
            cr.registerContentObserver(URI_POWER_DOUBLE_TAP_APP_FR_ACTION, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || URI_POWER_DOUBLE_TAP_ACTION.equals(uri) || 
                    URI_POWER_DOUBLE_TAP_APP_FR_ACTION.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
