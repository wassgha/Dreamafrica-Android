/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package dream.africa.base;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.multidex.MultiDex;
import android.support.v4.app.NotificationCompat;

import com.sjl.foreground.Foreground;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.concurrent.TimeUnit;

import dream.africa.base.beaming.BeamManager;
import dream.africa.base.content.preferences.Prefs;
import dream.africa.base.torrent.TorrentService;
import dream.africa.base.updater.ButterUpdater;
import dream.africa.base.utils.FileUtils;
import dream.africa.base.utils.LocaleUtils;
import dream.africa.base.utils.PrefUtils;
import dream.africa.base.utils.StorageUtils;
import dream.africa.base.utils.VersionUtils;
import timber.log.Timber;

public class ButterApplication extends Application implements ButterUpdater.Listener {

    private static OkHttpClient sHttpClient;
    private static String sDefSystemLanguage;
    private static Application sThis;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sThis = this;

        sDefSystemLanguage = LocaleUtils.getCurrentAsString();

        LeakCanary.install(this);
        Foreground.init(this);

        Constants.DEBUG_ENABLED = false;
        int versionCode = 0;
        try {
            String packageName = getPackageName();
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            int flags = packageInfo.applicationInfo.flags;
            versionCode = packageInfo.versionCode;
            Constants.DEBUG_ENABLED = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //initialise logging
        if (Constants.DEBUG_ENABLED) {
            Timber.plant(new Timber.DebugTree());
        }

        ButterUpdater.getInstance(this, this).checkUpdates(false);

        if(VersionUtils.isUsingCorrectBuild()) {
            TorrentService.start(this);
        }

        File path = new File(PrefUtils.get(this, Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(this).toString()));
        File directory = new File(path, "/torrents/");
        if (PrefUtils.get(this, Prefs.REMOVE_CACHE, true)) {
            FileUtils.recursiveDelete(directory);
            FileUtils.recursiveDelete(new File(path + "/subs"));
        } else {
            File statusFile = new File(directory, "status.json");
            statusFile.delete();
        }

        Timber.d("StorageLocations: " + StorageUtils.getAllStorageLocations());
        Timber.i("Chosen cache location: " + directory);

        Picasso.Builder builder = new Picasso.Builder(getAppContext());
        OkHttpDownloader downloader = new OkHttpDownloader(getHttpClient());
        builder.downloader(downloader);
        Picasso.setSingletonInstance(builder.build());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sDefSystemLanguage = LocaleUtils.getCurrentAsString();
    }

    @Override
    public void onTerminate() {
        // Just, so that it exists. Cause it is not executed in production, the whole application is closed anyways on OS level.
        BeamManager.getInstance(getAppContext()).onDestroy();
        super.onTerminate();
    }

    public static String getSystemLanguage() {
        return sDefSystemLanguage;
    }

    public static OkHttpClient getHttpClient() {
        if (sHttpClient == null) {
            sHttpClient = new OkHttpClient();
            sHttpClient.setConnectTimeout(30, TimeUnit.SECONDS);
            sHttpClient.setReadTimeout(60, TimeUnit.SECONDS);
            sHttpClient.setRetryOnConnectionFailure(true);

            int cacheSize = 10 * 1024 * 1024;
            File cacheLocation = new File(PrefUtils.get(ButterApplication.getAppContext(), Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(ButterApplication.getAppContext()).toString()));
            cacheLocation.mkdirs();
            com.squareup.okhttp.Cache cache = null;
            try {
                cache = new com.squareup.okhttp.Cache(cacheLocation, cacheSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sHttpClient.setCache(cache);
        }
        return sHttpClient;
    }

    public static String getStreamDir() {
        File path = new File(PrefUtils.get(getAppContext(), Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(getAppContext()).toString()));
        File directory = new File(path, "/torrents/");
        return directory.toString();
    }

    @Override
    public void updateAvailable(String updateFile) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (updateFile.length() > 0) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notif_logo)
                    .setContentTitle(getString(R.string.update_available))
                    .setContentText(getString(R.string.press_install))
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
            notificationIntent.setDataAndType(Uri.parse("file://" + updateFile), ButterUpdater.ANDROID_PACKAGE);

            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));

            nm.notify(ButterUpdater.NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    public static Context getAppContext() {
        return sThis;
    }
}
