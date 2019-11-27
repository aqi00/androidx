package com.example.mixture.adapter;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.mixture.fragment.HtmlFragment;

public class EpubPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<String> mHtmlArray = new ArrayList<String>();

    public EpubPagerAdapter(FragmentManager fm, ArrayList<String> htmlArray) {
        super(fm);
        mHtmlArray = htmlArray;
    }

    public int getCount() {
        return mHtmlArray.size();
    }

    public Fragment getItem(int position) {
        return HtmlFragment.newInstance(mHtmlArray.get(position));
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return "封面";
        } else {
            return "第" + position + "页";
        }
    }

}
