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

package dream.africa.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.facebook.FacebookSdk;

import com.facebook.appevents.AppEventsLogger;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import dream.africa.activities.PaymentActivity;
import dream.africa.base.providers.media.DreamAfricaProvider;
import butterknife.Bind;
import android.support.annotation.Nullable;
import dream.africa.BuildConfig;
import dream.africa.R;
import dream.africa.activities.base.ButterBaseActivity;
import dream.africa.base.Constants;
import dream.africa.base.beaming.BeamManager;
import dream.africa.base.beaming.BeamPlayerNotificationService;
import dream.africa.base.beaming.server.BeamServerService;
import dream.africa.base.content.preferences.Prefs;
import dream.africa.base.providers.media.models.Movie;
import dream.africa.base.providers.subs.SubsProvider;
import dream.africa.base.providers.subs.YSubsProvider;
import dream.africa.base.torrent.StreamInfo;
import dream.africa.base.utils.PrefUtils;
import dream.africa.base.utils.SignUtils;
import dream.africa.base.youtube.YouTubeData;
import dream.africa.fragments.dialog.MessageDialogFragment;
import dream.africa.fragments.MediaContainerFragment;
import dream.africa.fragments.NavigationDrawerFragment;
import dream.africa.utils.ToolbarUtils;
import dream.africa.widget.ScrimInsetsFrameLayout;
import timber.log.Timber;

/**
 * The main activity that houses the navigation drawer, and controls navigation between fragments
 */
public class MainActivity extends ButterBaseActivity implements NavigationDrawerFragment.Callbacks {

    private static final int PERMISSIONS_REQUEST = 123;
    private Fragment mCurrentFragment;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.navigation_drawer_container)
    ScrimInsetsFrameLayout mNavigationDrawerContainer;
    @Nullable
    @Bind(R.id.tabs)
    TabLayout mTabs;
    NavigationDrawerFragment mNavigationDrawerFragment;

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_main);

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        if (!PrefUtils.contains(this, TermsActivity.TERMS_ACCEPTED)) {
            startActivity(new Intent(this, TermsActivity.class));
        }

        if (!PrefUtils.contains(this, FacebookLogin.LOGGED_IN)) {
            startActivity(new Intent(this, FacebookLogin.class));
        }

//        if (PrefUtils.contains(this, FacebookLogin.LOGGED_IN) && !PrefUtils.contains(this, PaymentActivity.BYPASS_PAYMENT)) {
//            startActivity(new Intent(this, PaymentActivity.class));
//        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        }

        String action = getIntent().getAction();
        Uri data = getIntent().getData();
        if (action != null && action.equals(Intent.ACTION_VIEW) && data != null) {
            String streamUrl = data.toString();
            try {
                streamUrl = URLDecoder.decode(streamUrl, "utf-8");
                StreamLoadingActivity.startActivity(this, new StreamInfo(streamUrl));
                finish();
                return;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        FragmentManager.enableDebugLogging(BuildConfig.DEBUG);


        setSupportActionBar(mToolbar);
        setShowCasting(true);

        ToolbarUtils.updateToolbarHeight(this, mToolbar);

        // Set up the drawer.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        mNavigationDrawerFragment =
                (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer_fragment);

        mNavigationDrawerFragment.initialise(mNavigationDrawerContainer, drawerLayout);

        if (null != savedInstanceState) return;
        int providerId = PrefUtils.get(this, Prefs.DEFAULT_VIEW, 0);
        mNavigationDrawerFragment.selectItem(providerId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String title = mNavigationDrawerFragment.getCurrentItem().getTitle();
        setTitle(null != title ? title : getString(R.string.app_name));
        supportInvalidateOptionsMenu();
        if (mNavigationDrawerFragment.getCurrentItem() != null && mNavigationDrawerFragment.getCurrentItem().getTitle() != null) {
            setTitle(mNavigationDrawerFragment.getCurrentItem().getTitle());
        }

        mNavigationDrawerFragment.initItems();

        if(BeamServerService.getServer() != null)
            BeamServerService.getServer().stop();
        BeamPlayerNotificationService.cancelNotification();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_overview, menu);

        MenuItem playerTestMenuItem = menu.findItem(R.id.action_playertests);
        playerTestMenuItem.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                /* Override default {@link pct.droid.activities.BaseActivity } behaviour */
                return false;
            case R.id.action_playertests:
                openPlayerTestDialog();
                break;
            case R.id.action_search:
                //start the search activity
                SearchActivity.startActivity(this, mNavigationDrawerFragment.getCurrentItem().getMediaProvider());
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onNavigationDrawerItemSelected(NavigationDrawerFragment.NavDrawerItem item, String title) {
        setTitle(null != title ? title : getString(R.string.app_name));
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        String tag = title + "_tag";
        // Fragment fragment = mFragmentCache.get(position);
        mCurrentFragment = fragmentManager.findFragmentByTag(tag);
        if (null == mCurrentFragment && item.hasProvider()) {
            mCurrentFragment = MediaContainerFragment.newInstance(item.getMediaProvider());
        }

        if(mTabs.getTabCount() > 0)
            mTabs.getTabAt(0).select();

        fragmentManager.beginTransaction().replace(R.id.container, mCurrentFragment, tag).commit();

        if(mCurrentFragment instanceof MediaContainerFragment) {
            updateTabs((MediaContainerFragment) mCurrentFragment, ((MediaContainerFragment) mCurrentFragment).getCurrentSelection());
        }
    }

    public void updateTabs(MediaContainerFragment containerFragment, final int position) {
        if(mTabs == null)
            return;

        if(containerFragment != null) {
            ViewPager viewPager = containerFragment.getViewPager();
            if(viewPager == null)
                return;

            mTabs.setupWithViewPager(viewPager);
            mTabs.setTabGravity(TabLayout.GRAVITY_CENTER);
            mTabs.setTabMode(TabLayout.MODE_SCROLLABLE);
            mTabs.setVisibility(View.VISIBLE);

            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));
            mTabs.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

            if(mTabs.getTabCount() > 0) {
                mTabs.getTabAt(0).select();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mTabs.getTabCount() > position)
                            mTabs.getTabAt(position).select();
                    }
                }, 10);
            }

        } else {
            mTabs.setVisibility(View.GONE);
        }
    }

    private void openPlayerTestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final String[] file_types = getResources().getStringArray(R.array.file_types);
        final String[] files = getResources().getStringArray(R.array.files);

        builder.setTitle("Player Tests")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setSingleChoiceItems(file_types, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                dialogInterface.dismiss();
                final String location = files[index];
                if (location.equals("dialog")) {
                    final EditText dialogInput = new EditText(MainActivity.this);
                    dialogInput.setText("http://download.wavetlan.com/SVV/Media/HTTP/MP4/ConvertedFiles/QuickTime/QuickTime_test13_5m19s_AVC_VBR_324kbps_640x480_25fps_AAC-LCv4_CBR_93.4kbps_Stereo_44100Hz.mp4");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setView(dialogInput)
                            .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Movie media = new Movie(new DreamAfricaProvider(), new YSubsProvider());

                                    media.videoId = "dialogtestvideo";
                                    media.title = "User input test video";

                                    String location = dialogInput.getText().toString();

                                    BeamManager bm = BeamManager.getInstance(MainActivity.this);
                                    if (bm.isConnected()) {
                                        BeamPlayerActivity.startActivity(MainActivity.this, new StreamInfo(media, null, null, null, null, location), 0);
                                    } else {
                                        VideoPlayerActivity.startActivity(MainActivity.this, new StreamInfo(media, null, null, null, null, location), 0);
                                    }
                                }
                            });
                    builder.show();
                } else if (YouTubeData.isYouTubeUrl(location)) {
                    Intent i = new Intent(MainActivity.this, TrailerPlayerActivity.class);
                    Movie media = new Movie(new DreamAfricaProvider(), new YSubsProvider());
                    media.title = file_types[index];
                    i.putExtra(TrailerPlayerActivity.DATA, media);
                    i.putExtra(TrailerPlayerActivity.LOCATION, location);
                    startActivity(i);
                } else {
                    final Movie media = new Movie(new DreamAfricaProvider(), new YSubsProvider());
                    media.videoId = "bigbucksbunny";
                    media.title = file_types[index];
                    media.subtitles = new HashMap<>();
                    media.subtitles.put("en", "http://sv244.cf/bbb-subs.srt");

                    SubsProvider.download(MainActivity.this, media, "en", new Callback() {
                        @Override
                        public void onFailure(Request request, IOException e) {
                            BeamManager bm = BeamManager.getInstance(MainActivity.this);

                            if (bm.isConnected()) {
                                BeamPlayerActivity.startActivity(MainActivity.this, new StreamInfo(media, null, null, null, null, location), 0);
                            } else {
                                VideoPlayerActivity.startActivity(MainActivity.this, new StreamInfo(media, null, null, null, null, location), 0);
                            }
                        }

                        @Override
                        public void onResponse(Response response) throws IOException {
                            BeamManager bm = BeamManager.getInstance(MainActivity.this);
                            if (bm.isConnected()) {
                                BeamPlayerActivity.startActivity(MainActivity.this, new StreamInfo(media, null, null, "en", null, location), 0);
                            } else {
                                VideoPlayerActivity.startActivity(MainActivity.this, new StreamInfo(media, null, null, "en", null, location), 0);
                            }
                        }
                    });
                }
            }
        });

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length < 1 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    finish();
                }
            }
        }
    }

}