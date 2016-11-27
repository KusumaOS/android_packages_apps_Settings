/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.hardware;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settings.CustomDialogPreference;
import com.android.settings.R;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

public class VibratorIntensity extends CustomDialogPreference
        implements SeekBar.OnSeekBarChangeListener {
    private static final String PREF_NAME = "vibrator_intensity";

    private SeekBar mSeekBar;
    private TextView mValue;
    private TextView mWarning;
    private int mOriginalValue;
    private int mMinValue;
    private int mMaxValue;
    private int mDefaultValue;
    private int mWarningValue;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    private final CMHardwareManager mHardware;
    private final Vibrator mVibrator;

    public VibratorIntensity(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHardware = CMHardwareManager.getInstance(context);
        if (!mHardware.isSupported(CMHardwareManager.FEATURE_VIBRATOR)) {
            return;
        }

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        setDialogLayoutResource(R.layout.vibrator_intensity);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setNeutralButton(R.string.settings_reset_button, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mValue = (TextView) view.findViewById(R.id.value);
        mWarning = (TextView) view.findViewById(R.id.warning_text);

        // Read the current value in case user wants to dismiss his changes
        mOriginalValue = mHardware.getVibratorIntensity();
        mWarningValue = mHardware.getVibratorWarningIntensity();
        mMinValue = mHardware.getVibratorMinIntensity();
        mMaxValue = mHardware.getVibratorMaxIntensity();
        mDefaultValue = mHardware.getVibratorDefaultIntensity();
        if (mWarningValue > 0) {
            String message = getContext().getResources().getString(
                    R.string.vibrator_warning, intensityToPercent(mMinValue, mMaxValue,
                            mWarningValue));
            mWarning.setText(message);
        } else if (mWarning != null) {
            mWarning.setVisibility(View.GONE);
        }

        Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mOriginalValue - mMinValue);
    }

    @Override
    protected boolean onDismissDialog(DialogInterface dialog, int which) {
        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            mSeekBar.setProgress(mDefaultValue - mMinValue);
            mHardware.setVibratorIntensity(mDefaultValue);
            testVibration();
            return false;
        }
        return true;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Store percent value in SharedPreferences object
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getContext());
            final int intensity = mSeekBar.getProgress() + mMinValue;
            final int percent = intensityToPercent(mMinValue, mMaxValue, intensity);
            prefs.edit().putInt(PREF_NAME, percent).commit();
            CMSettings.Secure.putInt(getContext().getContentResolver(),
                    CMSettings.Secure.VIBRATOR_INTENSITY, intensity);
        } else {
            mHardware.setVibratorIntensity(mOriginalValue);
            CMSettings.Secure.putInt(getContext().getContentResolver(),
                    CMSettings.Secure.VIBRATOR_INTENSITY, mOriginalValue);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        final int intensity = progress + mMinValue;
        final boolean shouldWarn = mWarningValue > 0 && intensity >= mWarningValue;

        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }

        mValue.setText(String.format("%d%%", intensityToPercent(mMinValue, mMaxValue, intensity)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHardware.setVibratorIntensity(seekBar.getProgress() + mMinValue);
        testVibration();
    }

    private void testVibration() {
        final Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    private static int intensityToPercent(final int min, final int max, final int value) {
        final int percent = Math.round((value - min) * (100.f / (max - min)));

        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 0;
        }

        return percent;
    }

    private static int percentToIntensity(final int min, final int max, final int percent) {
        final int value = Math.round((((max - min) * percent) / 100.f) + min);

        if (value > max) {
            value = max;
        } else if (value < min) {
            value = min;
        }

        return value;
    }
}
