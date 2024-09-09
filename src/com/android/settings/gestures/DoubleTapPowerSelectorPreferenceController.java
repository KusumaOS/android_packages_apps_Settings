/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.controls.ControlsProviderService;
import android.service.quickaccesswallet.GetWalletCardsError;
import android.service.quickaccesswallet.GetWalletCardsRequest;
import android.service.quickaccesswallet.GetWalletCardsResponse;
import android.service.quickaccesswallet.QuickAccessWalletClient;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.applications.ServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class DoubleTapPowerSelectorPreferenceController extends AbstractPreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, LifecycleObserver,
        OnResume, OnPause, PreferenceControllerMixin {

    private static final String KEY = "double_tap_power_category";
    private static final String[] GESTURE_ACTION_KEYS = {
            "double_tap_power_camera", "double_tap_power_assistant",
            "double_tap_power_media", "double_tap_power_qr",
            "double_tap_power_wallet", "double_tap_power_device_control", 
            "double_tap_power_app"
    };

    private static final String[] GESTURE_ACTIONS = {
            "camera", "assistant", "togglemedia", "qr", "wallet", "devicecontrol", "customapp"
    };

    private final Context mContext;
    private final QuickAccessWalletClient mWalletClient;

    private PreferenceCategory mPreferenceCategory;
    private SelectorWithWidgetPreference[] mPreferences;

    private SettingObserver mSettingObserver;

    public DoubleTapPowerSelectorPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        mWalletClient = initWalletClient();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mPreferences = new SelectorWithWidgetPreference[GESTURE_ACTION_KEYS.length];
        for (int i = 0; i < GESTURE_ACTION_KEYS.length; i++) {
            mPreferences[i] = makeRadioPreference(GESTURE_ACTION_KEYS[i], getTitleResource(i));
        }

        if (mPreferenceCategory != null) {
            mSettingObserver = new SettingObserver(mPreferenceCategory);
        }
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
        for (int i = 0; i < mPreferences.length; i++) {
            if (preference == mPreferences[i]) {
                Settings.Secure.putStringForUser(mContext.getContentResolver(),
                        Settings.Secure.POWER_DOUBLE_TAP_ACTION, GESTURE_ACTIONS[i], 
                        UserHandle.USER_CURRENT);
                if (GESTURE_ACTIONS[i].equals("customapp")) {
                    String customAppAction = Settings.System.getStringForUser(
                            mContext.getContentResolver(),
                            Settings.System.POWER_DOUBLE_TAP_APP_ACTION,
                            UserHandle.USER_CURRENT);
                    if (customAppAction == null) {
                        launchAppSettings();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void updateState(Preference preference) {
        String currentAction = Settings.Secure.getStringForUser(
                mContext.getContentResolver(), Settings.Secure.POWER_DOUBLE_TAP_ACTION,
                UserHandle.USER_CURRENT);
        if (currentAction == null) {
            currentAction = "camera";
        }

        if ("none".equals(currentAction)) {
            for (SelectorWithWidgetPreference pref : mPreferences) {
                pref.setEnabled(false);
                pref.setChecked(false);
            }
        } else {
            for (SelectorWithWidgetPreference pref : mPreferences) {
                pref.setEnabled(true);
            }
            for (int i = 0; i < GESTURE_ACTIONS.length; i++) {
                boolean isSelected = GESTURE_ACTIONS[i].equals(currentAction);
                mPreferences[i].setChecked(isSelected);
                if ("double_tap_power_wallet".equals(GESTURE_ACTION_KEYS[i])) {
                    getWalletAvailability();
                } else if ("double_tap_power_device_control".equals(GESTURE_ACTION_KEYS[i])) {
                    getDeviceControlAvailability();
                } else if ("double_tap_power_app".equals(GESTURE_ACTION_KEYS[i])) {
                    getAppSummary();
                }
            }
        }
    }

    private void getAppSummary() {
        SelectorWithWidgetPreference pref = mPreferenceCategory.findPreference(
                "double_tap_power_app");
        if (pref != null) {
            String appSummary = Settings.System.getStringForUser(
                    mContext.getContentResolver(), Settings.System.POWER_DOUBLE_TAP_APP_FR_ACTION,
                    UserHandle.USER_CURRENT);
            if (appSummary != null) {
                pref.setSummary(appSummary);
            } else {
                pref.setSummary(R.string.custom_gesture_action_name_app_summary);
            }
        }
    }

    private void getDeviceControlAvailability() {
        SelectorWithWidgetPreference pref = mPreferenceCategory.findPreference(
                "double_tap_power_device_control");
        if (pref != null) {
            ServiceListing serviceListing = new ServiceListing.Builder(mContext)
                    .setIntentAction(ControlsProviderService.SERVICE_CONTROLS)
                    .setPermission(Manifest.permission.BIND_CONTROLS)
                    .setNoun("Controls Provider")
                    .setSetting("controls_providers")
                    .setTag("controls_providers")
                    .build();
            serviceListing.addCallback(services -> {
                boolean hasServices = !services.isEmpty();
                if (hasServices) {
                    pref.setEnabled(true);
                } else {
                    pref.setEnabled(false);
                    pref.setSummary(R.string.custom_gesture_action_name_device_control_disabled);
                }
            });
            serviceListing.reload();
        }
    }

    private void getWalletAvailability() {
        SelectorWithWidgetPreference pref = mPreferenceCategory.findPreference(
                "double_tap_power_wallet");
        if (pref != null) {
            mWalletClient.getWalletCards(new GetWalletCardsRequest(0, 0, 0, 1), 
                    new QuickAccessWalletClient.OnWalletCardsRetrievedCallback() {
                @Override
                public void onWalletCardsRetrieved(@NonNull GetWalletCardsResponse response) {
                    if (response.getWalletCards().isEmpty()) {
                        pref.setEnabled(false);
                        pref.setSummary(R.string.custom_gesture_action_name_wallet_disabled);
                    } else {
                        pref.setEnabled(true);
                    }
                }
                @Override
                public void onWalletCardRetrievalError(@NonNull GetWalletCardsError error) {
                    pref.setEnabled(false);
                }
            });
        }
    }

    private int getTitleResource(int index) {
        switch (index) {
            case 0: return R.string.custom_gesture_action_name_camera;
            case 1: return R.string.custom_gesture_action_name_assistant;
            case 2: return R.string.custom_gesture_action_name_toggle_media;
            case 3: return R.string.custom_gesture_action_name_qr;
            case 4: return R.string.custom_gesture_action_name_wallet;
            case 5: return R.string.custom_gesture_action_name_device_control;
            case 6: return R.string.custom_gesture_action_name_app;
            default: throw new IllegalArgumentException("Invalid index");
        }
    }

    private void launchAppSettings() {
        new SubSettingLauncher(mContext)
            .setDestination(DoubleTapPowerAppSettings.class.getName())
            .setSourceMetricsCategory(MetricsEvent.EXTRA_METRICS)
            .launch();
    }

    private SelectorWithWidgetPreference makeRadioPreference(String key, int titleId) {
        SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(
                mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        if ("double_tap_power_wallet".equals(key)) {
            pref.setVisible(mWalletClient.isWalletServiceAvailable());
            getWalletAvailability();
        } else if ("double_tap_power_device_control".equals(key)) {
            getDeviceControlAvailability();
        } else if ("double_tap_power_app".equals(key)) {
            getAppSummary();
            pref.setExtraWidgetOnClickListener(v -> {
                launchAppSettings();
            });
        }
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    private class SettingObserver extends ContentObserver {
        private final Uri URI_POWER_DOUBLE_TAP_ACTION = Settings.Secure.getUriFor(
                Settings.Secure.POWER_DOUBLE_TAP_ACTION);
        private final Uri URI_POWER_DOUBLE_TAP_APP_FR_ACTION = Settings.System.getUriFor(
                Settings.System.POWER_DOUBLE_TAP_APP_FR_ACTION);

        private final Preference mPreference;
        public SettingObserver(Preference preference) {
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
                updateState(mPreferenceCategory);
            }
        }
    }

    QuickAccessWalletClient initWalletClient() {
        return QuickAccessWalletClient.create(mContext);
    }
}
