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

package dream.africa.tv.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import dream.africa.base.providers.media.models.Media;
import dream.africa.base.providers.media.models.Movie;
import dream.africa.base.utils.VersionUtils;
import dream.africa.tv.R;
import dream.africa.tv.activities.base.TVBaseActivity;
import dream.africa.tv.fragments.TVMovieDetailsFragment;
import dream.africa.tv.fragments.TVShowDetailsFragment;
import dream.africa.tv.utils.BackgroundUpdater;

public class TVMediaDetailActivity extends TVBaseActivity implements TVMovieDetailsFragment.Callback {

    public static final String EXTRA_ITEM = "item";
    public static final String SHARED_ELEMENT_NAME = "hero";

    private BackgroundUpdater mBackgroundUpdater = new BackgroundUpdater();

    public static Intent startActivity(Activity activity, Media item) {
        return startActivity(activity, null, item);
    }

    public static Intent startActivity(Activity activity, Bundle options, Media item) {
        Intent intent = buildIntent(activity, item);
        activity.startActivity(intent, options);
        return intent;
    }

    public static Intent buildIntent(Context context, Media item){
        Intent intent = new Intent(context, TVMediaDetailActivity.class);
        intent.putExtra(EXTRA_ITEM, item);
        return intent;
    }

    /**
     * Called when the activity is first created.
     */

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_media_details);

        mBackgroundUpdater.initialise(this, R.color.black);
        Media media = getIntent().getParcelableExtra(EXTRA_ITEM);

        updateBackground(media.headerImage);

        if (VersionUtils.isLollipop()) {
            postponeEnterTransition();
        }

        if (media instanceof Movie) {
            getFragmentManager().beginTransaction().replace(R.id.fragment, TVMovieDetailsFragment.newInstance(media)).commit();
        } else {
            getFragmentManager().beginTransaction().replace(R.id.fragment, TVShowDetailsFragment.newInstance(media)).commit();
        }
        getFragmentManager().executePendingTransactions();

        if (VersionUtils.isLollipop()) {
            startPostponedEnterTransition();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundUpdater) mBackgroundUpdater.destroy();
    }

    protected void updateBackground(String backgroundImage) {
        mBackgroundUpdater.updateBackground(backgroundImage);
    }
}
