/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.markupartist.sthlmtraveling.provider.PlacesProvider;
import com.markupartist.sthlmtraveling.provider.PlacesProvider.Place.Places;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departure;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departures;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;


public class DeparturesActivity extends BaseListActivity {
    static String EXTRA_SITE_NAME = "com.markupartist.sthlmtraveling.siteName";

    private static final String STATE_GET_SITES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getsites.inprogress";
    private static final String STATE_GET_DEPARTURES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getdepartures.inprogress";
    private static final String STATE_SITE =
        "com.markupartist.sthlmtraveling.site";

    static String TAG = "DeparturesActivity";
    
    private static final int DIALOG_SITE_ALTERNATIVES = 0;
    private static final int DIALOG_GET_SITES_NETWORK_PROBLEM = 1;
    private static final int DIALOG_GET_DEPARTURES_NETWORK_PROBLEM = 3;

    private Site mSite;
    private static ArrayList<Site> mSiteAlternatives;

    private ProgressDialog mProgress;
    private GetSitesTask mGetSitesTask;
    private GetDeparturesTask mGetDeparturesTask;
    private String mSiteName;
    private Departures mDepartureResult;
    private Bundle mSavedState;

    private DepartureAdapter mSectionedAdapter;

    // TODO: Hacks for now...
    private int mPreferredTrafficMode = 2;
    private int mPlaceId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.departures_list);

        registerEvent("Departures");

        Bundle extras = getIntent().getExtras();
        mSiteName = extras.getString(EXTRA_SITE_NAME);

        mSectionedAdapter = new DepartureAdapter(this);
        
        //setupFilterButtons();
        //loadDepartures();
    }

    /**
     * We need to call loadDeapartures after restoreLocalState that's
     * why we need to override this method. Only needed for 1.5 devices though.
     * @see android.app.Activity#onPostCreate(android.os.Bundle)
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        loadDepartures();

        super.onPostCreate(savedInstanceState);
    }

    OnCheckedChangeListener mOnTransportTypeChange = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                handleCheckedTransportType(buttonView.getId());
            }
            /*
            switch (buttonView.getId()) {
            case R.id.radio_metros:
                if (isChecked) {
                    mSectionedAdapter.fillDepartures(mDepartureResult,
                            DepartureAdapter.TRANSPORT_TYPE_METRO);
                }
                break;
            case R.id.radio_buses:
                if (isChecked) {
                    mSectionedAdapter.fillDepartures(mDepartureResult,
                            DepartureAdapter.TRANSPORT_TYPE_BUS);
                }
                break;
            case R.id.radio_trains:
                if (isChecked) {
                    mSectionedAdapter.fillDepartures(mDepartureResult,
                            DepartureAdapter.TRANSPORT_TYPE_TRAIN);
                }
                break;
            case R.id.radio_trams:
                if (isChecked) {
                    mSectionedAdapter.fillDepartures(mDepartureResult,
                            DepartureAdapter.TRANSPORT_TYPE_TRAM);
                }
                break;
            }
            setListAdapter(mSectionedAdapter);
            */
        }
    };

    private void handleCheckedTransportType(int id) {
        switch (id) {
        case R.id.radio_metros:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    DepartureAdapter.TRANSPORT_TYPE_METRO);
            break;
        case R.id.radio_buses:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    DepartureAdapter.TRANSPORT_TYPE_BUS);
            break;
        case R.id.radio_trains:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    DepartureAdapter.TRANSPORT_TYPE_TRAIN);
            break;
        case R.id.radio_trams:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    DepartureAdapter.TRANSPORT_TYPE_TRAM);
            break;
        }
        setListAdapter(mSectionedAdapter);
    }

    public void setupFilterButtons() {
        // TODO: Fix hard coded values for preferred traffic mode.
        RadioButton radioMetros = (RadioButton) findViewById(R.id.radio_metros);
        radioMetros.setOnCheckedChangeListener(mOnTransportTypeChange);
        radioMetros.setEnabled(true);
        radioMetros.setChecked(mPreferredTrafficMode == 2 ? true : false);
        RadioButton radioBuses = (RadioButton) findViewById(R.id.radio_buses);
        radioBuses.setOnCheckedChangeListener(mOnTransportTypeChange);
        radioBuses.setEnabled(true);
        radioBuses.setChecked(mPreferredTrafficMode == 1 ? true : false);
        RadioButton radioTrains = (RadioButton) findViewById(R.id.radio_trains);
        radioTrains.setOnCheckedChangeListener(mOnTransportTypeChange);
        radioTrains.setEnabled(true);
        radioTrains.setChecked(mPreferredTrafficMode == 3 ? true : false);
        RadioButton radioTrams = (RadioButton) findViewById(R.id.radio_trams);
        radioTrams.setOnCheckedChangeListener(mOnTransportTypeChange);
        radioTrams.setEnabled(true);
        radioTrams.setChecked(mPreferredTrafficMode == 4 ? true : false);
    }

    private void loadDepartures() {
        @SuppressWarnings("unchecked")
        final Departures departureResult =
            (Departures) getLastNonConfigurationInstance();
        if (departureResult != null) {
            fillData(departureResult);
        } else if (mSite != null) {
            mGetDeparturesTask = new GetDeparturesTask();
            mGetDeparturesTask.execute(mSite);
        } else {
            mGetSitesTask = new GetSitesTask();
            mGetSitesTask.execute(mSiteName);
        }
    }

    private void fillData(Departures result) {
        // TODO: Get the selected type...
        mDepartureResult = result;

        RadioGroup transportGroup = (RadioGroup) findViewById(R.id.transport_group);

        setupFilterButtons();

        int checkedId = transportGroup.getCheckedRadioButtonId();
        handleCheckedTransportType(checkedId);
        /*mSectionedAdapter.fillDepartures(result,
                DepartureAdapter.TRANSPORT_TYPE_METRO);
        setListAdapter(mSectionedAdapter);
        */
        setTitle(getString(R.string.departures_for, mSite.getName()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSavedState != null) restoreLocalState(mSavedState);
    }

    @Override
    protected void onPause() {
        super.onPause();

        onCancelGetDeparturesTask();
        onCancelGetSitesTask();

        dismissProgress();

        // TODO: Fix hard coded values for transport type.
        int preferredTransportType = -1;
        RadioGroup transportGroup = (RadioGroup) findViewById(R.id.transport_group);
        int checkedId = transportGroup.getCheckedRadioButtonId();
        switch (checkedId) {
            case R.id.radio_buses:
                preferredTransportType = 1;
                break;
            case R.id.radio_metros:
                preferredTransportType = 2;
                break;
            case R.id.radio_trains:
                preferredTransportType = 3;
                break;
            case R.id.radio_trams:
                preferredTransportType = 4;
                break;
        }
        
        // TODO: Do in background thread.
        if (mPlaceId == -1) {
            ContentValues values = new ContentValues();
            values.put(Places.NAME, mSite.getName());
            values.put(Places.SITE_ID, mSite.getId());
            values.put(Places.PREFERRED_TRANSPORT_MODE, preferredTransportType);
            Uri uri = getContentResolver().insert(Places.CONTENT_URI, values);
        } else {
            ContentValues values = new ContentValues();
            values.put(Places.NAME, mSite.getName());
            values.put(Places.SITE_ID, mSite.getId());
            values.put(Places.PREFERRED_TRANSPORT_MODE, preferredTransportType);
            int updated = getContentResolver().update(Places.CONTENT_URI, values,
                    Places.SITE_ID + "= ?",
                    new String[] {String.valueOf(mSite.getId())});            
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDepartureResult;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSite != null) {
            outState.putParcelable(STATE_SITE, mSite);
        }

        saveGetSitesTask(outState);
        saveGetDeparturesTask(outState);

        mSavedState = outState;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
        mSavedState = null;
    }

    /**
     * Restores any local state, if any.
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(STATE_SITE)) {
            mSite = savedInstanceState.getParcelable(STATE_SITE);
        }

        restoreGetSitesTask(savedInstanceState);
        restoreGetDeparturesTask(savedInstanceState);
    }

    /**
     * Cancels the {@link GetSitesTask} if it is running.
     */
    private void onCancelGetSitesTask() {
        if (mGetSitesTask != null &&
                mGetSitesTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels GetSitesTask.");
            mGetSitesTask.cancel(true);
            mGetSitesTask = null;
        }
    }

    /**
     * Restores the {@link GetSitesTask}.
     * @param savedInstanceState the saved state
     */
    private void restoreGetSitesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_SITES_IN_PROGRESS)) {
            mSiteName = savedInstanceState.getString(EXTRA_SITE_NAME);
            Log.d(TAG, "restoring getSitesTask");
            mGetSitesTask = new GetSitesTask();
            mGetSitesTask.execute(mSiteName);
        }
    }

    /**
     * Saves the state of {@link GetSitesTask}.
     * @param outState
     */
    private void saveGetSitesTask(Bundle outState) {
        final GetSitesTask task = mGetSitesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetSitesTask");
            task.cancel(true);
            mGetSitesTask = null;
            outState.putBoolean(STATE_GET_SITES_IN_PROGRESS, true);
            outState.putString(EXTRA_SITE_NAME, mSiteName);
        }
    }

    /**
     * Cancels the {@link GetDeparturesTask} if it is running.
     */
    private void onCancelGetDeparturesTask() {
        if (mGetDeparturesTask != null/* &&
                mGetDeparturesTask.getStatus() == AsyncTask.Status.RUNNING*/) {
            Log.i(TAG, "Cancels GetDeparturesTask.");
            mGetDeparturesTask.cancel(true);
            mGetDeparturesTask= null;
        }
    }

    /**
     * Restores the {@link GetDeparturesTask}.
     * @param savedInstanceState the saved state
     */
    private void restoreGetDeparturesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_DEPARTURES_IN_PROGRESS)) {
            Log.d(TAG, "restoring getSitesTask");
            mGetDeparturesTask = new GetDeparturesTask();
            mGetDeparturesTask.execute(mSite);
        }
    }

    /**
     * Saves the state of {@link GetDeparturesTask}.
     * @param outState
     */
    private void saveGetDeparturesTask(Bundle outState) {
        final GetDeparturesTask task = mGetDeparturesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetDeparturesState");
            task.cancel(true);
            mGetSitesTask = null;
            outState.putBoolean(STATE_GET_DEPARTURES_IN_PROGRESS, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_departures, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                new GetDeparturesTask().execute(mSite);
                return true;
            case R.id.menu_journey_planner:
                Intent i = new Intent(this, StartActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_SITE_ALTERNATIVES:
            ArrayAdapter<Site> siteAdapter =
                new ArrayAdapter<Site>(this, R.layout.simple_dropdown_item_1line,
                        mSiteAlternatives);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.did_you_mean)
                .setAdapter(siteAdapter, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new GetDeparturesTask().execute(
                                mSiteAlternatives.get(which));
                    }
                })
                .create();
        case DIALOG_GET_SITES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetSitesTask = new GetSitesTask();
                    mGetSitesTask.execute(mSiteName);
                }
            });
        case DIALOG_GET_DEPARTURES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetDeparturesTask = new GetDeparturesTask();
                    mGetDeparturesTask.execute(mSite);
                }
            });
        }
        return null;
    }

    /**
     * Show progress dialog.
     */
    private void showProgress() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage(getText(R.string.loading));
            mProgress.show();   
        }
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        if (mProgress != null) {
            try {
                mProgress.dismiss();
            } catch (Exception e) {
                Log.d(TAG, "Could not dismiss progress; " + e.getMessage());
            }
            mProgress = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        onCancelGetSitesTask();
        onCancelGetDeparturesTask();

        dismissProgress();
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    /**
     * Background job for getting {@link Site}s.
     */
    private class GetSitesTask extends AsyncTask<String, Void, ArrayList<Site>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Site> doInBackground(String... params) {
            try {
                return SitesStore.getInstance().getSite(params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Site> result) {
            dismissProgress();

            if (result != null && !result.isEmpty()) {
                if (result.size() == 1) {
                    new GetDeparturesTask().execute(result.get(0));
                } else {
                    mSiteAlternatives = result;
                    showDialog(DIALOG_SITE_ALTERNATIVES);
                }
            } else if (!mWasSuccess) {
                showDialog(DIALOG_GET_SITES_NETWORK_PROBLEM);
            } else {
            //    onNoRoutesDetailsResult();
            }
        }
    }

    /**
     * Background job for getting {@link Departure}s.
     */
    private class GetDeparturesTask extends AsyncTask<Site, Void, Departures> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Departures doInBackground(Site... params) {
            try {
                mSite = params[0];
                
                String[] projection = new String[] {
                                             Places._ID,
                                             Places.NAME,
                                             Places.PREFERRED_TRANSPORT_MODE,
                                             Places.SITE_ID
                                          };
                Uri sitesUri =  Places.CONTENT_URI;
                Cursor sitesCursor = managedQuery(sitesUri, projection,
                        Places.SITE_ID + "= ?",
                        new String[] {String.valueOf(mSite.getId())},
                        Places.NAME + " asc");
                if (sitesCursor.moveToFirst()) {
                    mPlaceId = sitesCursor.getInt(sitesCursor.getColumnIndex(Places._ID));
                    mPreferredTrafficMode = sitesCursor.getInt(sitesCursor.getColumnIndex(Places.PREFERRED_TRANSPORT_MODE));
                }

                DeparturesStore departures = new DeparturesStore();
                return departures.find(params[0]);
            } catch (IllegalArgumentException e) {
                mWasSuccess = false;
                return null;
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Departures result) {
            dismissProgress();

            if (mWasSuccess) {
                fillData(result);
            } else {
                showDialog(DIALOG_GET_DEPARTURES_NETWORK_PROBLEM);
            }
        }
    }
}
