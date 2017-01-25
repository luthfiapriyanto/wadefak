package co.kartoo.app.tabfragment;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import co.kartoo.app.InterestActivity_;
import co.kartoo.app.nearby.GPSTracker;
import co.kartoo.app.nearby.NearbyEvent;
import co.kartoo.app.nearby.PromoFromNearbyAdapter;
import co.kartoo.app.rest.model.Card;
import de.greenrobot.event.EventBus;
import co.kartoo.app.R;
import co.kartoo.app.models.LoginPref_;
import co.kartoo.app.nearby.ActivityNearbyResult_;
import co.kartoo.app.rest.PromoService;
import co.kartoo.app.rest.model.newest.Nearby;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

@EFragment(R.layout.fragment_nearby)
public class FragmentNearby extends Fragment implements CompoundButton.OnCheckedChangeListener{

    @ViewById
    Switch mTBmyCards;

    @ViewById
    FloatingActionButton mFABlist;

    @ViewById
    RelativeLayout relativeLayout3;
    @ViewById
    LinearLayout GPSoff;
    @ViewById
    TextView Location;
    @ViewById
    RecyclerView mRVpromo;
    @ViewById
    ProgressBar progressBarScroll;
    @ViewById
    RelativeLayout oops;
    @ViewById
    Spinner sortbySpinner;
    @ViewById
    RelativeLayout timeOut;
    @ViewById
    ImageView reload;


    ArrayList<Nearby> allNearbyOutlet;
    ArrayList<Nearby> showedNearbyOutlet;

    PromoFromNearbyAdapter adapter;
    ArrayList<Nearby> listCategoryOutlet;
    ArrayList<Nearby> listCategoryOutletToShow;
    Map<String,Card> myCardsMap;

    Retrofit retrofit;
    PromoService service;
    SupportMapFragment mapFragment;
    LocationManager location;
    boolean myCards;

    String[] sortBy = {
            "Distance",
            "Popular",
            "Ending Soon",
            "A to Z",
    };

    @Pref
    LoginPref_ loginPref;
    final int RQS_GooglePlayServices = 1;
    LatLng myLocation;
    ClusterManager<Nearby> mClusterManager;
    GPSTracker gps;

    String sortValue;
    int a;


    @AfterViews
    public void init() {
        retrofit = new Retrofit.Builder().baseUrl(getString(R.string.base_url)).addConverterFactory(GsonConverterFactory.create()).build();
        service = retrofit.create(PromoService.class);

        progressBarScroll.setVisibility(View.VISIBLE);
        mRVpromo.setVisibility(View.GONE);
        mFABlist.setVisibility(View.GONE);

        getCurrentLocation();
        mixpanel();
        myLocation = getCurrentLocation();

        listCategoryOutletToShow =  new ArrayList<>();
        adapter = new PromoFromNearbyAdapter(getContext(),listCategoryOutletToShow);
        mRVpromo.setLayoutManager(new LinearLayoutManager(getContext()));
        mRVpromo.setAdapter(adapter);

        //spinner SortBy
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_dropdown_item, sortBy);
        sortbySpinner.setAdapter(adapter);

        if (myLocation.longitude==0){
            a=2;
            sortbySpinner.setSelection(a);
            sortValue = "expiring";
        }
        else {
            sortbySpinner.setSelection(0);
            sortValue = "distance";
        }

        sortbySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                if (pos==0){
                    if (myLocation.longitude==0){
                        Toast.makeText(getActivity(), "Are you lost? Please turn on your location", Toast.LENGTH_LONG).show();
                        sortbySpinner.setSelection(a);
                    }
                    else {
                        sortValue = "distance";
                        mRVpromo.setVisibility(View.GONE);

                    }
                } else if (pos==1){
                    sortValue = "popular";
                    mRVpromo.setVisibility(View.GONE);

                } else if (pos==2){
                    sortValue = "expiring";
                    mRVpromo.setVisibility(View.GONE);

                } else if (pos==3){
                    sortValue = "alphabet";
                    mRVpromo.setVisibility(View.GONE);

                }
                Log.e("TAG", "onItemSelected: "+pos);
                Log.e("TAG", "onItemSelected: "+sortbySpinner.getSelectedItem().toString().toLowerCase());
                a = sortbySpinner.getSelectedItemPosition();
                if (myLocation.longitude!=0){
                    checkPromo();
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mTBmyCards.setOnCheckedChangeListener(FragmentNearby.this);
    }

    public void sortby(){

    }

    @Click(R.id.mFABlist)
    public void mFABlistClick() {
        ArrayList<Nearby> selectedOutlet = allNearbyOutlet;
        Bundle bundle = new Bundle();
        bundle.putSerializable("result", selectedOutlet);
        Intent intent = new Intent(getActivity(), ActivityNearbyResult_.class);
        intent.putExtras(bundle);
        getActivity().startActivity(intent);
    }

    public LatLng getCurrentLocation() {
        gps = new GPSTracker(getContext());
        double latitude;
        double longitude;
        // check if GPS enabled
        if(gps.canGetLocation()){
            GPSoff.setVisibility(View.GONE);
            mRVpromo.setVisibility(View.VISIBLE);
            timeOut.setVisibility(View.GONE);
            mFABlist.setVisibility(View.VISIBLE);
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            Log.e("TAG", "getCurrentLocation: "+latitude+longitude );
            return new LatLng(latitude, longitude);
        }
        else{
            GPSoff.setVisibility(View.VISIBLE);
            mRVpromo.setVisibility(View.GONE);
            mFABlist.setVisibility(View.GONE);
            oops.setVisibility(View.GONE);
            timeOut.setVisibility(View.GONE);
            progressBarScroll.setVisibility(View.GONE);
            return new LatLng(0, 0);
        }
    }

    public void checkPromo(){
        Call<ArrayList<Nearby>> searchOutletCall = service.searchCoordinateMyCard(loginPref.token().get(), myLocation.latitude + "", myLocation.longitude + "", "distance");
        searchOutletCall.enqueue(new Callback<ArrayList<Nearby>>() {
            @Override
            public void onResponse(Response<ArrayList<Nearby>> response, Retrofit retrofit) {
                if (response.isSuccess()) {
                    if (response.body().size()==0){
                        mTBmyCards.setChecked(false);
                        loadData();
                    }
                    else {
                        mTBmyCards.setChecked(true);
                        loadData();
                    }
                }
            }
            @Override
            public void onFailure(Throwable t) {
                progressBarScroll.setVisibility(View.GONE);
                mRVpromo.setVisibility(View.GONE);
                mFABlist.setVisibility(View.GONE);
                oops.setVisibility(View.GONE);
                timeOut.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        loadData();
    }

    public void loadData() {
        progressBarScroll.setVisibility(View.VISIBLE);
        mRVpromo.setVisibility(View.GONE);
        mFABlist.setVisibility(View.GONE);
        oops.setVisibility(View.GONE);
        timeOut.setVisibility(View.GONE);

        if (mTBmyCards.isChecked()){
            myCards = true;
        }
        else {
            myCards = false;
        }
        Log.e("TAG", "loadData: "+myCards);

        if (myCards) {
            Call<ArrayList<Nearby>> searchOutletCall = service.searchCoordinateMyCard(loginPref.token().get(), myLocation.latitude + "", myLocation.longitude + "", sortValue);
            searchOutletCall.enqueue(new Callback<ArrayList<Nearby>>() {
                @Override
                public void onResponse(Response<ArrayList<Nearby>> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        Log.e("TAG", "onResponseNearby: "+response.body().size());
                        if (response.body().size()==0){
                            progressBarScroll.setVisibility(View.GONE);
                            mRVpromo.setVisibility(View.GONE);
                            mFABlist.setVisibility(View.GONE);
                            oops.setVisibility(View.VISIBLE);
                            timeOut.setVisibility(View.GONE);

                        }
                        else {
                            progressBarScroll.setVisibility(View.GONE);
                            mRVpromo.setVisibility(View.VISIBLE);
                            mFABlist.setVisibility(View.VISIBLE);
                            oops.setVisibility(View.GONE);
                            timeOut.setVisibility(View.GONE);

                            EventBus.getDefault().postSticky(new NearbyEvent(response.body()));

                        }

                        mTBmyCards.setOnCheckedChangeListener(FragmentNearby.this);

                    } else if (response.code() == 401) {

                    } else if (response.code() == 404) {

                    } else if (response.code() == 500) {

                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    progressBarScroll.setVisibility(View.GONE);
                    mRVpromo.setVisibility(View.GONE);
                    mFABlist.setVisibility(View.GONE);
                    oops.setVisibility(View.GONE);
                    timeOut.setVisibility(View.VISIBLE);
                }
            });
        } else {
            Call<ArrayList<Nearby>> searchOutletCall = service.searchCoordinate(loginPref.token().get(), myLocation.latitude + "", myLocation.longitude + "", sortValue);
            searchOutletCall.enqueue(new Callback<ArrayList<Nearby>>() {
                @Override
                public void onResponse(Response<ArrayList<Nearby>> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        if (response.body().size()==0){
                            progressBarScroll.setVisibility(View.GONE);
                            mRVpromo.setVisibility(View.GONE);
                            mFABlist.setVisibility(View.GONE);
                            oops.setVisibility(View.VISIBLE);
                            timeOut.setVisibility(View.GONE);

                        }
                        else {
                            progressBarScroll.setVisibility(View.GONE);
                            mRVpromo.setVisibility(View.VISIBLE);
                            mFABlist.setVisibility(View.VISIBLE);
                            oops.setVisibility(View.GONE);
                            timeOut.setVisibility(View.GONE);

                            EventBus.getDefault().postSticky(new NearbyEvent(response.body()));

                        }
                        mTBmyCards.setOnCheckedChangeListener(FragmentNearby.this);
                    } else if (response.code() == 401) {

                    } else if (response.code() == 404) {

                    } else if (response.code() == 500) {

                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    progressBarScroll.setVisibility(View.GONE);
                    mRVpromo.setVisibility(View.GONE);
                    mFABlist.setVisibility(View.GONE);
                    oops.setVisibility(View.GONE);
                    timeOut.setVisibility(View.VISIBLE);
                }
            });

            }
    }

    public void mixpanel(){
        String projectToken = getString(R.string.mixpanel_token);
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(getContext(), projectToken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTimeStamp = dateFormat.format(new Date());
        try {
            JSONObject props = new JSONObject();
            props.put("TimeStamp", currentTimeStamp);
            props.put("email", loginPref.email().get());
            if (myLocation!=null){
                props.put("LatLong", myLocation.latitude+", "+myLocation.longitude);
            }
            else {
                props.put("LatLong", "0"+", "+"0");
            }
            mixpanel.track("Open Nearby tab", props);
        } catch (JSONException e) {
            Log.e("MYAPP", "Unable to add properties to JSONObject", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("TAG", "onResume: "+ "resume");
        getCurrentLocation();

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity().getApplicationContext());
        if (resultCode == ConnectionResult.SUCCESS) {

        } else {
            Toast.makeText(getActivity(),"Google Play Service unavailable. Please install it first", Toast.LENGTH_LONG).show();
        }

    }

    public void onEvent(NearbyEvent event) {
        Log.d("eventNearby", "caught");
        listCategoryOutletToShow.clear();
        listCategoryOutletToShow.addAll(event.getListPromo());
        adapter.notifyDataSetChanged();
    }

    @Click(R.id.Location)
    public void LocationClick(){
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    @Click(R.id.reload)
    public void reloadClick(){
        loadData();
    }
}
