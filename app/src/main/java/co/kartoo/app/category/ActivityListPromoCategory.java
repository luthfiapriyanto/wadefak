package co.kartoo.app.category;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
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

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.Plus;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import co.kartoo.app.landing.LandingActivity_;
import co.kartoo.app.nearby.GPSTracker;
import de.greenrobot.event.EventBus;
import co.kartoo.app.R;
import co.kartoo.app.events.TrendingFinishedEventCategory;
import co.kartoo.app.models.LoginPref_;
import co.kartoo.app.promo.ActivityFilter_;
import co.kartoo.app.rest.PromoService;
import co.kartoo.app.rest.model.CategoryOutlet;
import co.kartoo.app.rest.model.newest.DiscoverPromotionCategory;
import co.kartoo.app.views.SpaceItemDecoration;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;



@EActivity(R.layout.activity_list_promo_category)
public class ActivityListPromoCategory extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener {

    @ViewById
    Switch mTBmyCards;
    @ViewById
    ImageView mIVfilter;
    @ViewById
    RecyclerView mRVpromo;
    @ViewById
    Toolbar mToolbar;
    @ViewById
    TextView mTVtitle;
    @ViewById
    RelativeLayout relativeLayout3;
    @ViewById
    RelativeLayout oops;

    @ViewById
    Spinner sortbySpinner;

    @ViewById
    RelativeLayout timeOut;
    @ViewById
    ImageView reload;

    @ViewById
    LinearLayout mainLayout;

    @ViewById
    LinearLayout LLloadMore;


    @ViewById
    ProgressBar progressBar;


    private boolean loading = false;
    ArrayList<DiscoverPromotionCategory> listCategoryOutlet;
    ArrayList<DiscoverPromotionCategory> listTrendingPromo;
    PromoFromCategoryAdapter adapterTrendingPromo;

    String TAG = "ActivityListPromoCategory";

    int lastPage=  1;
    int maxPage = -1;
    int diff;

    @Pref
    LoginPref_ loginPref;

    boolean myCards;
    String idPromo, name;

    String[] sortBy = {
            "Distance",
            "Popular",
            "Ending Soon",
            "A to Z",
    };

    String sortValue;
    LatLng myLocation;
    GPSTracker gps;
    int a;
    Boolean hasPromo;

    GoogleApiClient mGoogleApiClient;

    private void setUpNavDrawer() {
        if (mToolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationIcon(R.drawable.ic_back_orange);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
    @AfterViews
    public void init() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addApi(LocationServices.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        mGoogleApiClient.connect();

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(100); // half second between each showcase view
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, "52");
        sequence.setConfig(config);
        sequence.addSequenceItem(new MaterialShowcaseView.Builder(this)
                .setTarget(mTBmyCards)
                .setDismissOnTouch(true)
                .setTitleText("My Cards")
                .setMaskColour(getResources().getColor(R.color.transparent))

                .setContentText("Filter the list of promotions\nbased on your cards")
                .build()
        );
        sequence.addSequenceItem(new MaterialShowcaseView.Builder(this)
                .setTarget(sortbySpinner)
                .setDismissOnTouch(true)
                .setTitleText("Sort By")
                .setMaskColour(getResources().getColor(R.color.transparent))

                .setContentText("Arrange the promotions \nbased on different filters")
                .build()
        );
        sequence.start();

        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
        mRVpromo.setVisibility(View.GONE);
        oops.setVisibility(View.GONE);
        setUpNavDrawer();
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getCurrentLocation();
        myLocation = getCurrentLocation();

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.base_url)).addConverterFactory(GsonConverterFactory.create()).build();
        final PromoService promoService = retrofit.create(PromoService.class);
        final String token = loginPref.token().get();

        listCategoryOutlet = new ArrayList<>();

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        idPromo = intent.getStringExtra("id");
        //hasPromo = intent.getBooleanExtra("hasPromo", false);

        mTVtitle.setText(name);

        mTBmyCards.setChecked(true);


        if (loginPref.type().get().equals("guest")){
            mTBmyCards.setChecked(false);
        }

        listTrendingPromo = new ArrayList<>();
        adapterTrendingPromo = new PromoFromCategoryAdapter(this, listTrendingPromo, promoService, token, mRVpromo);
        mRVpromo.addItemDecoration(new SpaceItemDecoration(3));
        mRVpromo.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        mRVpromo.setAdapter(adapterTrendingPromo);


/*
        //setMyCard
        if (hasPromo.equals(true)){
            mTBmyCards.setChecked(true);
        }
        else if (hasPromo.equals(false)){
            mTBmyCards.setChecked(false);
        }
*/
        //spinner SortBy
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item, sortBy);
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
        checkPromos();

        sortbySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos==0){
                    if (myLocation.longitude==0){
                        Toast.makeText(ActivityListPromoCategory.this, "Are you lost? Please turn on your location", Toast.LENGTH_LONG).show();
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
                loadData();
                //checkPromos();
            }
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mTBmyCards.setOnCheckedChangeListener(ActivityListPromoCategory.this);
    }

    public LatLng getCurrentLocation() {
        gps = new GPSTracker(this);
        double latitude;
        double longitude;
        // check if GPS enabled
        if(gps.canGetLocation()){
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            Log.e("TAG", "getCurrentLocation: "+latitude+longitude );
            return new LatLng(latitude, longitude);
        }
        else{
            return new LatLng(0,0);
        }
    }

    public void checkPromos(){
        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.base_url)).addConverterFactory(GsonConverterFactory.create()).build();
        final PromoService promoService = retrofit.create(PromoService.class);
        final String token = loginPref.token().get();

        Call<CategoryOutlet> favoriteOutletCall = promoService.getPromoMyCard(token, idPromo, 1, sortValue, String.valueOf(myLocation.longitude), String.valueOf(myLocation.latitude));
        favoriteOutletCall.enqueue(new Callback<CategoryOutlet>() {
            @Override
            public void onResponse(Response<CategoryOutlet> response, Retrofit retrofit) {
                if (response.isSuccess()) {
                    if (response.code() == 200) {
                        Log.e("TAG", "HEADER: "+response.headers().get("X-Promotion-Available"));

                        Log.e("TAG", "PERTAMA: "+response.body().getPromotions().size());
                        if (response.body().getPromotions().size()==0){
                            mTBmyCards.setChecked(false);
                            oops.setVisibility(View.GONE);
                            progressBar.setVisibility(View.VISIBLE);
                            loadData();
                        }
                        else {
                            mTBmyCards.setChecked(true);
                            loadData();

                        }
                    }else if (response.code() == 401){

                        Toast.makeText(ActivityListPromoCategory.this, "Your session has expired, please login again", Toast.LENGTH_LONG).show();
                        loginPref.name().remove();
                        loginPref.email().remove();
                        loginPref.token().remove();
                        loginPref.urlPhoto().remove();
                        loginPref.level().remove();
                        loginPref.uid().remove();
                        loginPref.type().remove();
                        loginPref.fromBitmap().remove();
                        loginPref.addcard().remove();
                        loginPref.regid().remove();
                        loginPref.notifID().remove();
                        //LoginManager.getInstance().logOut();
                        FacebookSdk.sdkInitialize(getApplicationContext());
                        LoginManager.getInstance().logOut();
                        if (mGoogleApiClient.isConnected()) {
                            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                            mGoogleApiClient.disconnect();
                            Log.e("asdf", "logged out");
                        }

                        Log.e("sesudah","sesudah"+loginPref.name().get());
                        Intent intent2 = new Intent(ActivityListPromoCategory.this, LandingActivity_.class);
                        startActivity(intent2);
                        finish();

                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                progressBar.setVisibility(View.GONE);
                oops.setVisibility(View.GONE);
                mRVpromo.setVisibility(View.GONE);
                timeOut.setVisibility(View.VISIBLE);
            }
        });
    }

    public void loadData() {
        getCurrentLocation();
        listTrendingPromo.clear();


        progressBar.setVisibility(View.VISIBLE);
        oops.setVisibility(View.GONE);
        mRVpromo.setVisibility(View.GONE);
        timeOut.setVisibility(View.GONE);

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.base_url)).addConverterFactory(GsonConverterFactory.create()).build();
        final PromoService promoService = retrofit.create(PromoService.class);
        final String token = loginPref.token().get();

        Intent intent = getIntent();
        final String id = intent.getStringExtra("id");

        if (mTBmyCards.isChecked()){
            myCards = true;
        }
        else {
            myCards = false;
        }
        int sortBy = 0;

        Log.e("TAG", "masukScrollAwal: "+myCards);

        if (myCards){
            Call<CategoryOutlet> promoByCategoryCall = promoService.getPromoMyCard(token, id, 1, sortValue, String.valueOf(myLocation.longitude), String.valueOf(myLocation.latitude));
            promoByCategoryCall.enqueue(new Callback<CategoryOutlet>() {
                @Override
                public void onResponse(Response<CategoryOutlet> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        Log.e("TAG", "KEDUA"+ response.code());
                        if (response.code() == 200) {
                            if (response.body().getPromotions().size()==0){
                                progressBar.setVisibility(View.GONE);
                                mRVpromo.setVisibility(View.GONE);
                                oops.setVisibility(View.VISIBLE);
                                timeOut.setVisibility(View.GONE);

                            }
                            else {
                                Log.e("TAG", "KEDUAmyCards"+ response.code());

                                progressBar.setVisibility(View.GONE);
                                mRVpromo.setVisibility(View.VISIBLE);
                                oops.setVisibility(View.GONE);
                                timeOut.setVisibility(View.GONE);

                            }
                            EventBus.getDefault().postSticky(new TrendingFinishedEventCategory(response.body()));
                            mTBmyCards.setOnCheckedChangeListener(ActivityListPromoCategory.this);
                            mixpanel();
                        }
                    }
                }
                @Override
                public void onFailure(Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    oops.setVisibility(View.GONE);
                    mRVpromo.setVisibility(View.GONE);
                    timeOut.setVisibility(View.VISIBLE);
                }
            });
        }
        else {
            Call<CategoryOutlet> promoByCategoryCall = promoService.getPromo(token, id, 1, sortValue, String.valueOf(myLocation.longitude), String.valueOf(myLocation.latitude));
            promoByCategoryCall.enqueue(new Callback<CategoryOutlet>() {
                @Override
                public void onResponse(Response<CategoryOutlet> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        Log.e("TAG", "Category: NotmyCard"+ response.code());
                        if (response.code() == 200) {
                            if (response.body().getPromotions().size()==0){
                                Log.e("TAG", "KEDUA"+ response.code());

                                progressBar.setVisibility(View.GONE);
                                mRVpromo.setVisibility(View.GONE);
                                oops.setVisibility(View.VISIBLE);
                                timeOut.setVisibility(View.GONE);

                            }
                            else {
                                Log.e("TAG", "KEDUA"+ response.code());

                                progressBar.setVisibility(View.GONE);
                                mRVpromo.setVisibility(View.VISIBLE);
                                oops.setVisibility(View.GONE);
                                timeOut.setVisibility(View.GONE);

                            }
                            EventBus.getDefault().postSticky(new TrendingFinishedEventCategory(response.body()));
                            mTBmyCards.setOnCheckedChangeListener(ActivityListPromoCategory.this);

                        }
                    }
                }
                @Override
                public void onFailure(Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    oops.setVisibility(View.GONE);
                    mRVpromo.setVisibility(View.GONE);
                    timeOut.setVisibility(View.VISIBLE);
                }
            });
        }
        Log.e("TAG", "loadData: "+myCards);

        mRVpromo.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                View view = mRVpromo.getChildAt(mRVpromo.getChildCount() - 1);
                if (listTrendingPromo.size()!=0){
                    diff = (view.getBottom() - (mRVpromo.getHeight() + mRVpromo.getScrollY()));
                }
                Log.e("diff", "diff: "+diff );
                if (diff < 0) {
                    if (!loading && lastPage < maxPage) {
                        loading = true;
                        Log.e("TAG", "masukScroll: "+myCards);


                        if (myCards){
                            Log.e("TAG", "onScrollChanged: "+lastPage+1);
                            Call<CategoryOutlet> promoByCategoryCall = promoService.getPromoMyCard(token, id, lastPage+1, sortValue, String.valueOf(myLocation.longitude), String.valueOf(myLocation.latitude));
                            promoByCategoryCall.enqueue(new Callback<CategoryOutlet>() {
                                @Override
                                public void onResponse(Response<CategoryOutlet> response, Retrofit retrofit) {
                                    loading = false;
                                    if (response.isSuccess()) {
                                        if (response.code() == 200) {
                                            listTrendingPromo.addAll(response.body().getPromotions());
                                            adapterTrendingPromo.notifyDataSetChanged();
                                            maxPage = response.body().getMaxPage();
                                            lastPage++;
                                        }
                                    }
                                }
                                @Override
                                public void onFailure(Throwable t) {
                                    //progressBar.setVisibility(View.GONE);
                                    loading = false;
                                }
                            });
                        }
                        else {
                            Log.e("TAG", "onScrollChanged: "+"masuk+NotMyCard");
                            Call<CategoryOutlet> promoByCategoryCall = promoService.getPromo(token, id, lastPage+1, sortValue, String.valueOf(myLocation.longitude), String.valueOf(myLocation.latitude));
                            promoByCategoryCall.enqueue(new Callback<CategoryOutlet>() {
                                @Override
                                public void onResponse(Response<CategoryOutlet> response, Retrofit retrofit) {
                                    loading = false;
                                    if (response.isSuccess()) {
                                        if (response.code() == 200) {
                                            listTrendingPromo.addAll(response.body().getPromotions());
                                            adapterTrendingPromo.notifyDataSetChanged();
                                            maxPage = response.body().getMaxPage();
                                            lastPage++;
                                        }
                                    }
                                }
                                @Override
                                public void onFailure(Throwable t) {
                                    //progressBar.setVisibility(View.GONE);
                                    loading = false;
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    public void mixpanel(){
        String projectToken = getString(R.string.mixpanel_token);
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, projectToken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTimeStamp = dateFormat.format(new Date());
        try {
            JSONObject props = new JSONObject();
            props.put("TimeStamp", currentTimeStamp);
            props.put("email", loginPref.email().get());
            props.put("category", name);
            mixpanel.track("View Promos by category", props);
        } catch (JSONException e) {
            Log.e("MYAPP", "Unable to add properties to JSONObject", e);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        listTrendingPromo.clear();
        oops.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        loadData();
    }


    @Click(R.id.mIVfilter)
    public void mIVfilterClick() {
        Intent intent = new Intent(this, ActivityFilter_.class);
        startActivityForResult(intent,1);
    }

    @Click(R.id.reload)
    public void reloadClick() {
        loadData();
    }

    @Override
    public void onStart() {
        super.onStart();
        listTrendingPromo.clear();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRVpromo.scrollToPosition(preferences.getInt("position", 0));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRVpromo.scrollBy(0, - preferences.getInt("offset", 0));
            }
        }, 500);
        */
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(TrendingFinishedEventCategory event) {
        Log.e("eventTrending", "caught");
        listTrendingPromo.clear();
        maxPage = event.getListPromo().getMaxPage();
        listTrendingPromo.addAll(event.getListPromo().getPromotions());
        adapterTrendingPromo.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        /*
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        View firstChild = mRVpromo.getChildAt(0);
        int firstVisiblePosition = mRVpromo.getChildAdapterPosition(firstChild);
        int offset = firstChild.getTop();

        Log.d(TAG, "Postition: " + firstVisiblePosition);
        Log.d(TAG, "Offset: " + offset);

        preferences.edit()
                .putInt("position", firstVisiblePosition)
                .putInt("offset", offset)
                .apply();

               */
    }
}
