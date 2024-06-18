/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static lineageos.providers.LineageSettings.TORCH_LONG_PRESS_POWER_GESTURE;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

public class SwipeToScreenshotPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    public SwipeToScreenshotPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "torch_long_press_power_gesture");
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return LineageSettings.System.putInt(mContext.getContentResolver(), TORCH_LONG_PRESS_POWER_TIMEOUT,
                isChecked ? ON : OFF);
    }

    @Override
    public boolean isChecked() {
        return LineageSettings.System.getInt(mContext.getContentResolver(), TORCH_LONG_PRESS_POWER_TIMEOUT, 0) != 0;
    }
}
