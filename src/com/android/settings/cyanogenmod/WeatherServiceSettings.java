/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.weatherservice.WeatherProviderService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static org.cyanogenmod.internal.logging.CMMetricsLogger.WEATHER_SETTINGS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeatherServiceSettings extends SettingsPreferenceFragment {

    private Context mContext;
    private Handler mHandler;
    private static final String TAG = WeatherServiceSettings.class.getSimpleName();

    private static final String PREFERENCE_GENERAL = "weather_general_settings";
    private static final String PREFERENCE_PROVIDERS = "weather_service_providers";

    private PreferenceCategory mGeneralSettingsCategory;
    private PreferenceCategory mProvidersCategory;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.weather_settings);
        setHasOptionsMenu(true);
        mGeneralSettingsCategory = (PreferenceCategory) getPreferenceScreen()
                .findPreference(PREFERENCE_GENERAL);
        mProvidersCategory = (PreferenceCategory) getPreferenceScreen()
                .findPreference(PREFERENCE_PROVIDERS);
    }

    @Override
    protected int getMetricsCategory() {
        return WEATHER_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
        registerPackageMonitor();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterPackageMonitor();
    }

    private void registerPackageMonitor() {
        mPackageMonitor.register(mContext, BackgroundThread.getHandler().getLooper(),
                UserHandle.ALL, true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.weather_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.get_more_weather_providers) {
            launchGetWeatherProviders();
        }
        return false;
    }

    private void launchGetWeatherProviders() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.weather_settings_play_store_market_url))));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.weather_settings_play_store_http_url))));
        }
    }

    private void unregisterPackageMonitor() {
        mPackageMonitor.unregister();
    }

    private PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }
    };

    private void updateAdapter() {
        final PackageManager pm = getContext().getPackageManager();
        final Intent intent = new Intent(WeatherProviderService.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfoList = pm.queryIntentServices(intent,
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        List<WeatherProviderServiceInfo> weatherProviderServiceInfos
                = new ArrayList<>(resolveInfoList.size());
        ComponentName activeService = getEnabledWeatherServiceProvider();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo.serviceInfo == null) continue;

            if (resolveInfo.serviceInfo.packageName == null
                    || resolveInfo.serviceInfo.name == null) {
                //Really?
                continue;
            }

            if (!resolveInfo.serviceInfo.permission.equals(
                    cyanogenmod.platform.Manifest.permission.BIND_WEATHER_PROVIDER_SERVICE)) {
                continue;
            }
            WeatherProviderServiceInfo serviceInfo = new WeatherProviderServiceInfo();
            serviceInfo.componentName = new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            serviceInfo.isActive = serviceInfo.componentName.equals(activeService);
            serviceInfo.caption = resolveInfo.loadLabel(pm);
            serviceInfo.icon = resolveInfo.loadIcon(pm);
            serviceInfo.settingsComponentName = getSettingsComponent(pm, resolveInfo);

            weatherProviderServiceInfos.add(serviceInfo);
        }

        if (weatherProviderServiceInfos.size() > 0) {
            if (getPreferenceScreen().findPreference(PREFERENCE_GENERAL) == null);
                getPreferenceScreen().addPreference(mGeneralSettingsCategory);
            if (getPreferenceScreen().findPreference(PREFERENCE_PROVIDERS) == null);
                getPreferenceScreen().addPreference(mProvidersCategory);

            mProvidersCategory.removeAll();
            for (WeatherProviderServiceInfo info : weatherProviderServiceInfos) {
                Preference preference = new WeatherProviderPreference(mContext, info);
                preference.setLayoutResource(R.layout.weather_service_provider_info_row);
                mProvidersCategory.addPreference(preference);
            }

        } else {
            getPreferenceScreen().removePreference(mGeneralSettingsCategory);
            getPreferenceScreen().removePreference(mProvidersCategory);
        }

    }

    private class WeatherProviderPreference extends Preference {

        private WeatherProviderServiceInfo mInfo;
        private View mView;

        public WeatherProviderPreference(Context context, WeatherProviderServiceInfo info) {
            super(context);
            mInfo = info;
        }

        @Override
        protected void onBindView(final View view) {
            ((ImageView) view.findViewById(android.R.id.icon))
                    .setImageDrawable(mInfo.icon);
            view.setTag(mInfo);
            mView = view;

            ((TextView) view.findViewById(android.R.id.title)).setText(mInfo.caption);

            RadioButton radioButton = (RadioButton) view.findViewById(android.R.id.button1);
            radioButton.setChecked(mInfo.isActive);
            radioButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    view.onTouchEvent(event);
                    return false;
                }
            });

            boolean showSettings = mInfo.settingsComponentName != null;
            View settingsDivider = view.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            ImageView settingsButton = (ImageView) view.findViewById(android.R.id.button2);
            settingsButton.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            settingsButton.setAlpha(mInfo.isActive ? 1f : Utils.DISABLED_ALPHA);
            settingsButton.setEnabled(mInfo.isActive);
            settingsButton.setFocusable(mInfo.isActive);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchSettingsActivity(mInfo);
                }
            });
            final View header = mView.findViewById(R.id.provider);
            header.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    setActiveWeatherProviderService();
                }
            });

            notifyChanged();
        }

        private boolean isActiveProvider() {
            return mInfo.isActive;
        }

        public void setActiveState(boolean active) {
            mInfo.isActive = active;
            RadioButton radioButton = (RadioButton) mView.findViewById(android.R.id.button1);
            radioButton.setChecked(active);

            boolean hasSettings = mInfo.settingsComponentName != null;
            if (hasSettings) {
                ImageView settingsButton = (ImageView) mView.findViewById(android.R.id.button2);
                settingsButton.setAlpha(mInfo.isActive ? 1f : Utils.DISABLED_ALPHA);
                settingsButton.setEnabled(mInfo.isActive);
                settingsButton.setFocusable(mInfo.isActive);
            }
        }

        private void launchSettingsActivity(WeatherProviderServiceInfo info) {
            if (info != null && info.settingsComponentName != null) {
                try {
                    mContext.startActivity(new Intent().setComponent(info.settingsComponentName));
                } catch (ActivityNotFoundException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast t = Toast.makeText(mContext,
                                    R.string.weather_settings_activity_not_found,
                                    Toast.LENGTH_LONG);
                            TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                            if (v != null) v.setGravity(Gravity.CENTER);
                            t.show();
                        }
                    });
                    Log.w(TAG, info.settingsComponentName + " not found");
                }
            }
        }

        private void setActiveWeatherProviderService() {
            if (!mInfo.isActive) {
                markAsActiveProvider();
                CMSettings.Secure.putString(mContext.getContentResolver(),
                        CMSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        mInfo.componentName.flattenToString());
            }
            launchSettingsActivity(mInfo);
        }

        private void markAsActiveProvider() {
            // Check for current active provider
            for (int indx = 0; indx < mProvidersCategory.getPreferenceCount(); indx++) {
                Preference p = mProvidersCategory.getPreference(indx);
                if (p instanceof WeatherProviderPreference) {
                    WeatherProviderPreference preference = (WeatherProviderPreference) p;
                    if (preference.isActiveProvider()) {
                        preference.setActiveState(false);
                        break;
                    }
                }
            }
            // Marks this provider as active
            setActiveState(true);
        }
    }

    private ComponentName getSettingsComponent(PackageManager pm, ResolveInfo resolveInfo) {
        if (resolveInfo == null
                || resolveInfo.serviceInfo == null
                || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        XmlResourceParser parser = null;
        Exception caughtException = null;

        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm,
                    WeatherProviderService.SERVICE_META_DATA);
            if (parser == null) {
                Log.w(TAG, "Can't find " + WeatherProviderService.SERVICE_META_DATA + " meta-data");
                return null;
            }
            Resources res =
                    pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"weather-provider-service".equals(nodeName)) {
                Log.w(TAG, "Meta-data does not start with weather-provider-service tag");
                return null;
            }
            //Will use Dream styleable for now, it has the attribute we need
            TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.Dream);
            cn = sa.getString(com.android.internal.R.styleable.Dream_settingsActivity);
            sa.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
        } catch (IOException e) {
            caughtException = e;
        } catch (XmlPullParserException e) {
            caughtException = e;
        } finally {
            if (parser != null) parser.close();
        }
        if (caughtException != null) {
            Log.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName,
                    caughtException);
            return null;
        }
        if (cn != null && cn.indexOf('/') < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        return cn == null ? null : ComponentName.unflattenFromString(cn);
    }

    private ComponentName getEnabledWeatherServiceProvider() {
        String activeWeatherServiceProvider = CMSettings.Secure.getString(
                mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
        if (activeWeatherServiceProvider == null) return null;
        return ComponentName.unflattenFromString(activeWeatherServiceProvider);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.empty_weather_state, contentRoot, false);
        TextView emptyTextView = (TextView) emptyView.findViewById(R.id.message);
        emptyTextView.setText(R.string.weather_settings_no_services_prompt);

        contentRoot.addView(emptyView);

        ListView listView = getListView();
        listView.setEmptyView(emptyView);
    }

    private class WeatherProviderServiceInfo {
        CharSequence caption;
        Drawable icon;
        boolean isActive;
        ComponentName componentName;
        public ComponentName settingsComponentName;
    }
}
