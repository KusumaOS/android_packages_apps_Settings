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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.DeviceUtils;
import com.android.settingslib.search.SearchIndexable;

/** Accessibility settings for system controls. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class SystemControlsFragment extends DashboardFragment {

    private static final String TAG = "SystemControlsFragment";

    private static final String KEY_VOLUME_ANSWER_CALL = "volume_answer_call";

    private static final String KEY_HOME_WAKE_SCREEN = "home_wake_screen";
    private static final String KEY_BACK_WAKE_SCREEN = "back_wake_screen";
    private static final String KEY_MENU_WAKE_SCREEN = "menu_wake_screen";
    private static final String KEY_ASSIST_WAKE_SCREEN = "assist_wake_screen";
    private static final String KEY_APP_SWITCH_WAKE_SCREEN = "app_switch_wake_screen";
    private static final String KEY_VOLUME_WAKE_SCREEN = "volume_wake_screen";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DeviceUtils.hasHomeKey(mContext.getActivity())) {
            if (!DeviceUtils.canWakeUsingHomeKey(mContext.getActivity())) {
                removePreference(findPreference(KEY_HOME_WAKE_SCREEN));
            }
        }
        if (DeviceUtils.hasBackKey(mContext.getActivity())) {
            if (!DeviceUtils.canWakeUsingBackKey(mContext.getActivity())) {
                removePreference(findPreference(KEY_BACK_WAKE_SCREEN));
            }
        }
        if (DeviceUtils.hasMenuKey(mContext.getActivity())) {
            if (!DeviceUtils.canWakeUsingMenuKey(mContext.getActivity())) {
                removePreference(findPreference(KEY_MENU_WAKE_SCREEN));
            }
        }
        if (DeviceUtils.hasAssistKey(mContext.getActivity())) {
            if (!DeviceUtils.canWakeUsingAssistKey(mContext.getActivity())) {
                removePreference(findPreference(KEY_ASSIST_WAKE_SCREEN));
            }
        }
        if (DeviceUtils.hasAppSwitchKey(mContext.getActivity())) {
            if (!DeviceUtils.canWakeUsingAppSwitchKey(mContext.getActivity())) {
                removePreference(findPreference(KEY_APP_SWITCH_WAKE_SCREEN));
            }
        }
        if (DeviceUtils.hasVolumeKeys(mContext.getActivity())) {
            if (!Utils.isVoiceCapable(mContext)) {
                removePreference(findPreference(KEY_VOLUME_ANSWER_CALL));
            }
            if (!DeviceUtils.canWakeUsingVolumeKey(mContext.getActivity())) {
                removePreference(findPreference(KEY_VOLUME_WAKE_SCREEN));
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_SYSTEM_CONTROLS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_system_controls;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_system_controls) {

                @Override
                public Set<String> getNonIndexableKeys(Context context) {
                    final Set<String> result = new ArraySet<>();

                    if (!DeviceUtils.canWakeUsingHomeKey(context)) {
                        result.add(KEY_HOME_WAKE_SCREEN);
                    }
                    if (!DeviceUtils.canWakeUsingBackKey(context)) {
                        result.add(KEY_BACK_WAKE_SCREEN);
                    }
                    if (!DeviceUtils.canWakeUsingMenuKey(context)) {
                        result.add(KEY_MENU_WAKE_SCREEN);
                    }
                    if (!DeviceUtils.canWakeUsingAssistKey(context)) {
                        result.add(KEY_ASSIST_WAKE_SCREEN);
                    }
                    if (!DeviceUtils.canWakeUsingAppSwitchKey(context)) {
                        result.add(KEY_APP_SWITCH_WAKE_SCREEN);
                    }
                    if (!Utils.isVoiceCapable(context)) {
                        result.add(KEY_VOLUME_ANSWER_CALL);
                    }
                    if (!DeviceUtils.canWakeUsingVolumeKey(context)) {
                        result.add(KEY_VOLUME_WAKE_SCREEN);
                    }
                return result;
                }
            }
}
