/*
 * Copyright (C) 2024 Kusuma
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import lineageos.providers.LineageSettings;

public class QuickPulldownPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private PrimarySwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    public QuickPulldownPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public boolean isChecked() {
        int quickPulldown = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 0);
        return quickPulldown != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int newQuickPulldownSetting = isChecked ? 1 : 0;
        return LineageSettings.System.putInt(mContext.getContentResolver(),
                LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, newQuickPulldownSetting);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        int value = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 0);
        String summary=	"";
        switch (value) {
            case 0:
                summary = mContext.getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;
            case 1:
            case 2:
                summary = mContext.getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary,
                    mContext.getResources().getString(
                        (value == 2) ^
                        (mContext.getResources().getConfiguration().getLayoutDirection()
                            == View.LAYOUT_DIRECTION_RTL)
                        ? R.string.status_bar_quick_qs_pulldown_summary_left
                        : R.string.status_bar_quick_qs_pulldown_summary_right));
                break;
        }
        preference.setSummary(summary);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
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
        private final Uri URI_STATUS_BAR_QUICK_QS_PULLDOWN  = LineageSettings.System.getUriFor(
                LineageSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(URI_STATUS_BAR_QUICK_QS_PULLDOWN, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || URI_STATUS_BAR_QUICK_QS_PULLDOWN.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
