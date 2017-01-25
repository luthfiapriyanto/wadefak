package co.kartoo.app.cards;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import co.kartoo.app.MainActivity_;
import co.kartoo.app.R;
import co.kartoo.app.tabfragment.TabAdapter;


@EActivity(R.layout.activity_my_cards)
public class MyCardsActivity extends AppCompatActivity {

    @ViewById
    Toolbar mToolbar;
    @ViewById
    TextView mTVtitle;
    @ViewById(R.id.mVPmain)
    ViewPager mVPmain;
    @ViewById
    TabLayout mTLtab;

    TabAdapterCards tabAdapter;

    @AfterViews
    void init() {
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
        setUpNavDrawer();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.mTLtab);
        tabLayout.addTab(tabLayout.newTab().setText("Personal Cards"));
        tabLayout.addTab(tabLayout.newTab().setText("Suggested Cards"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        Intent intent = getIntent();
        String from = intent.getStringExtra("from");

        /***
         * Belum pasti
         */
        /*
        if (from!=null){
            mVPmain.setCurrentItem(1);
            tabAdapter.notifyDataSetChanged();
        }
        */

        final ViewPager viewPager = (ViewPager) findViewById(R.id.mVPmain);
        final PagerAdapter adapter = new TabAdapterCards(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    public void onBackPressed(){
        finish();
    }


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
}
