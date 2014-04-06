package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.flurry.android.FlurryAgent;

import java.util.Map;

public class BaseFragment extends Fragment {

    protected void registerEvent(String event) {
        FlurryAgent.onEvent(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
