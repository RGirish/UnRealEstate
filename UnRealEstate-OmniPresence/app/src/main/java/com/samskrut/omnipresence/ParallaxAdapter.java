/**
 * This file is used for displaying the Instructions screens with a parallax effect.
 * It is from a Parallax Library 'ParallaxPagerTransformer'. It doesn't really have anything that I have modified.
 * It's just predefined code.
 */

package com.samskrut.omnipresence;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import java.util.ArrayList;

public class ParallaxAdapter extends FragmentStatePagerAdapter {

    private ArrayList<ParallaxFragment> mFragments;
    private ViewPager mPager;

    public ParallaxAdapter(FragmentManager fm) {
        super(fm);

        mFragments = new ArrayList<>();
    }

    @Override
    public Fragment getItem(int i) {
        return mFragments.get(i);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    public void add(ParallaxFragment parallaxFragment) {
        parallaxFragment.setAdapter(this);
        mFragments.add(parallaxFragment);
        notifyDataSetChanged();
        mPager.setCurrentItem(getCount() - 1, true);

    }

    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public void setPager(ViewPager pager) {
        mPager = pager;
    }
}