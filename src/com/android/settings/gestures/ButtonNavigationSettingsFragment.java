/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.DeviceUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import static com.android.systemui.shared.recents.utilities.Utilities.isLargeScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lineageos.internal.util.DeviceKeysConstants.*;

import lineageos.providers.LineageSettings;

/**
 * A fragment that includes settings for 3-button navigation modes.
 */
@SearchIndexable(forTarget = SearchIndexable.MOBILE)
public class ButtonNavigationSettingsFragment extends DashboardFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "ButtonNavigationSettingsFragment";

    public static final String BUTTON_NAVIGATION_SETTINGS =
            "com.android.settings.BUTTON_NAVIGATION_SETTINGS";

    private static final String KEY_NAVIGATION_BACK_LONG_PRESS = "navigation_back_long_press";
    private static final String KEY_NAVIGATION_HOME_LONG_PRESS = "navigation_home_long_press";
    private static final String KEY_NAVIGATION_HOME_DOUBLE_TAP = "navigation_home_double_tap";
    private static final String KEY_NAVIGATION_APP_SWITCH_LONG_PRESS =
            "navigation_app_switch_long_press";

    private static final String KEY_NAV_BAR_INVERSE = "sysui_nav_bar_inverse";
    private static final String KEY_NAV_BAR_LAYOUT = "navbar_layout_views";
    private static final String KEY_ENABLE_TASKBAR = "enable_taskbar";

    private ListPreference mNavigationBackLongPressAction;
    private ListPreference mNavigationHomeLongPressAction;
    private ListPreference mNavigationHomeDoubleTapAction;
    private ListPreference mNavigationAppSwitchLongPressAction;
    private ListPreference mNavBarLayout;

    private SwitchPreference mNavBarInverse;
    private SwitchPreference mEnableTaskbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Resources res = getResources();
        final ContentResolver resolver = requireActivity().getContentResolver();

        Action defaultBackLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnBackBehavior));
        Action defaultHomeLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnHomeBehavior));
        Action defaultHomeDoubleTapAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_doubleTapOnHomeBehavior));
        Action defaultAppSwitchLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnAppSwitchBehavior));
        Action backLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_BACK_LONG_PRESS_ACTION,
                defaultBackLongPressAction);
        Action homeLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                defaultHomeLongPressAction);
        Action homeDoubleTapAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                defaultHomeDoubleTapAction);
        Action appSwitchLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                defaultAppSwitchLongPressAction);

        // Navigation bar back long press
        mNavigationBackLongPressAction = initList(KEY_NAVIGATION_BACK_LONG_PRESS,
                backLongPressAction);

        // Navigation bar home long press
        mNavigationHomeLongPressAction = initList(KEY_NAVIGATION_HOME_LONG_PRESS,
                homeLongPressAction);

        // Navigation bar home double tap
        mNavigationHomeDoubleTapAction = initList(KEY_NAVIGATION_HOME_DOUBLE_TAP,
                homeDoubleTapAction);

        // Navigation bar app switch long press
        mNavigationAppSwitchLongPressAction = initList(KEY_NAVIGATION_APP_SWITCH_LONG_PRESS,
                appSwitchLongPressAction);

        mNavBarInverse = findPreference(KEY_NAV_BAR_INVERSE);
        mNavBarLayout = findPreference(KEY_NAV_BAR_LAYOUT);
        if (mNavBarLayout != null) {
            String[] navBarEntries = res.getStringArray(R.array.navbar_layout_entries);
            if (res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                String temp = navBarEntries[2];
                navBarEntries[2] = navBarEntries[3];
                navBarEntries[3] = temp;
            }
            mNavBarLayout.setEntries(navBarEntries);
            mNavBarLayout.setEntryValues(res.getStringArray(R.array.navbar_layout_values));
        }

        mEnableTaskbar = findPreference(KEY_ENABLE_TASKBAR);
        if (mEnableTaskbar != null) {
            if (!isLargeScreen(requireContext()) || !DeviceUtils.hasNavigationBar()) {
                getPreferenceScreen().removePreference(mEnableTaskbar);
            } else if (DeviceUtils.isSwipeUpEnabled(requireContext())) {
                mEnableTaskbar.setEnabled(false);
                mEnableTaskbar.setSummary(
                        R.string.navigation_bar_enable_taskbar_disabled_button);
            } else if ((Settings.System.getInt(resolver, 
                    Settings.System.NAVIGATION_BAR_HINT_KEYBOARD, 1) == 0) &&
                    DeviceUtils.isEdgeToEdgeEnabled(requireContext())) {
                mEnableTaskbar.setEnabled(false);
            } else {
                mEnableTaskbar.setOnPreferenceChangeListener(this);
                mEnableTaskbar.setChecked(LineageSettings.System.getInt(resolver,
                        LineageSettings.System.ENABLE_TASKBAR,
                        isLargeScreen(requireContext()) ? 1 : 0) == 1);
                toggleTaskBarDependencies(mEnableTaskbar.isChecked());
            }
        }

        List<Integer> unsupportedValues = new ArrayList<>();
        List<String> entries = new ArrayList<>(
                Arrays.asList(res.getStringArray(R.array.hardware_keys_action_entries)));
        List<String> values = new ArrayList<>(
                Arrays.asList(res.getStringArray(R.array.hardware_keys_action_values)));

        // hide split screen option unconditionally - it doesn't work at the moment
        // once someone gets it working again: hide it only for low-ram devices
        // (check ActivityManager.isLowRamDeviceStatic())
        unsupportedValues.add(Action.SPLIT_SCREEN.ordinal());

        for (int unsupportedValue: unsupportedValues) {
            entries.remove(unsupportedValue);
            values.remove(unsupportedValue);
        }

        String[] actionEntries = entries.toArray(new String[0]);
        String[] actionValues = values.toArray(new String[0]);

        mNavigationBackLongPressAction.setEntries(actionEntries);
        mNavigationBackLongPressAction.setEntryValues(actionValues);

        mNavigationHomeLongPressAction.setEntries(actionEntries);
        mNavigationHomeLongPressAction.setEntryValues(actionValues);

        mNavigationHomeDoubleTapAction.setEntries(actionEntries);
        mNavigationHomeDoubleTapAction.setEntryValues(actionValues);

        mNavigationAppSwitchLongPressAction.setEntries(actionEntries);
        mNavigationAppSwitchLongPressAction.setEntryValues(actionValues);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavigationBackLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_BACK_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mNavigationHomeLongPressAction) {
            handleHomeLongPressChange(mNavigationHomeLongPressAction, newValue);
            return true;
        } else if (preference == mNavigationHomeDoubleTapAction) {
            handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            return true;
        } else if (preference == mNavigationAppSwitchLongPressAction) {
            handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mEnableTaskbar) {
            toggleTaskBarDependencies((Boolean) newValue);
            LineageSettings.System.putInt(getContentResolver(),
                    LineageSettings.System.ENABLE_TASKBAR, ((Boolean) newValue) ? 1 : 0);
            return true;
        }
        return false;
    }

    private ListPreference initList(String key, Action value) {
        return initList(key, value.ordinal());
    }

    private ListPreference initList(String key, int value) {
        ListPreference list = getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        LineageSettings.System.putInt(getContentResolver(), setting, Integer.parseInt(value));
    }

    private void handleHomeLongPressChange(ListPreference pref, Object newValue) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        LineageSettings.System.putInt(getContentResolver(), 
                LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION, Integer.parseInt(value));
        int homeLongPressValue = Integer.parseInt(value);
        if (homeLongPressValue == 0) {
            Settings.Secure.putInt(getContentResolver(), 
                    Settings.Secure.ASSIST_LONG_PRESS_HOME_ENABLED, 0);
        } else {
            Settings.Secure.putInt(getContentResolver(), 
                    Settings.Secure.ASSIST_LONG_PRESS_HOME_ENABLED, 1);
        }
    }

    private void toggleTaskBarDependencies(boolean enabled) {
        enablePreference(mNavBarInverse, !enabled);
        enablePreference(mNavBarLayout, !enabled);
        enablePreference(mNavigationBackLongPressAction, !enabled);
        enablePreference(mNavigationHomeLongPressAction, !enabled);
        enablePreference(mNavigationHomeDoubleTapAction, !enabled);
        enablePreference(mNavigationAppSwitchLongPressAction, !enabled);
    }

    private void enablePreference(Preference pref, boolean enabled) {
        if (pref != null) {
            pref.setEnabled(enabled);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_BUTTON_NAV_DLG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.button_navigation_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.button_navigation_settings) {

        @Override
        protected boolean isPageSearchEnabled(Context context) {
        return (DeviceUtils.hasNavigationBar() || 
                DeviceUtils.isKeyDisablerSupported(context)) &&
                SystemNavigationPreferenceController.isOverlayPackageAvailable(context,
                NAV_BAR_MODE_3BUTTON_OVERLAY);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = super.getNonIndexableKeys(context);
            if (!isLargeScreen(context) || !DeviceUtils.hasNavigationBar()) {
                keys.add(KEY_ENABLE_TASKBAR);
            }
        return keys;
        }
    };
}
