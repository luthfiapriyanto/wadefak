package co.kartoo.app.cards;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import co.kartoo.app.cards.suggestioncard.FragmentSuggestionCards_;
import co.kartoo.app.category.FragmentCategory_;
import co.kartoo.app.tabfragment.FragmentHome_;
import co.kartoo.app.tabfragment.FragmentMalls_;
import co.kartoo.app.tabfragment.FragmentNearby_;

public class TabAdapterCards extends FragmentStatePagerAdapter {
    final int PAGE_COUNT = 2;
    //public String tabTitles[] = new String[] { "Home", "Categories", "Nearby", "Social"};
    public String tabTitles[];
    final int POSITION_PERSONAL = 0;
    final int POSITION_SUGGESTION = 1;

    int mNumOfTabs;


    public TabAdapterCards(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case POSITION_PERSONAL : return new FragmentCards_();
            case POSITION_SUGGESTION : return new FragmentSuggestionCards_();
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    /*
    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
    */
}
