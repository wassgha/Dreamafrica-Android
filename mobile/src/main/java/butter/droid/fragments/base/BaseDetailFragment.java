package dream.africa.fragments.base;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;

import dream.africa.activities.MediaDetailActivity;
import dream.africa.base.torrent.StreamInfo;

public abstract class BaseDetailFragment extends Fragment {

    protected FragmentListener mCallback;
    protected MediaDetailActivity mActivity;
    protected View mRoot;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof MediaDetailActivity)
            mActivity = (MediaDetailActivity) activity;
    }

    public interface FragmentListener {
        public void playStream(StreamInfo streamInfo);
    }

}
