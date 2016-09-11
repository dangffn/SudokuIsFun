package com.danbuntu.sudokuisfun.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ActionMode;

import java.lang.ref.WeakReference;

/**
 * Created by Dan on 6/20/2016. Have a great day!
 */
public class SwipeAdapter extends FragmentStatePagerAdapter {

    private ActionMode actionMode;
    private WeakReference[] ids;
    private String[] tabs;

    public SwipeAdapter(FragmentManager fm, String[] tabs) {
        super(fm);
        this.tabs = tabs;
        ids = new WeakReference[tabs.length];
    }

    @Override
    public Fragment getItem(int position) {

        PuzzleSelectionFragment sf = new PuzzleSelectionFragment();
        ids[position] = new WeakReference<>(sf);

        sf.setCategoryFilter(tabs[position]);

        sf.setOnFragmentChangeListener(new PuzzleSelectionFragment.OnFragmentChangeListener() {
            @Override
            public void actionModeCreated(ActionMode actionMode) {
                SwipeAdapter.this.actionMode = actionMode;
            }

            @Override
            public void actionModeDestroyed() {
                SwipeAdapter.this.actionMode = null;
            }

            @Override
            public void requeryAllFragments() {
                requeryFragments();
            }
        });

        return sf;
    }

    public void requeryFragments() {
        PuzzleSelectionFragment fragment;
        for(int i=0; i<tabs.length; i++) {
            fragment = getRealFragmentAtPosition(i);
            if(fragment != null) {
                fragment.requeryCursor();
                Log.i("Fragment", "Reference to fragment " + i + " was re-queried");
            }
        }
    }

    public PuzzleSelectionFragment getRealFragmentAtPosition(int position) {
        WeakReference ref = ids[position];
        if(ref != null) {
            return (PuzzleSelectionFragment) ids[position].get();
        } else {
            return null;
        }
    }

    @Override
    public int getCount() {
        return tabs.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position < tabs.length && position >= 0) {
            return tabs[position];
        } else {
            return null;
        }
    }

    public void killActionMode() {
        if(actionMode != null) actionMode.finish();
    }

    @Override
    public int getItemPosition(Object object) {
        for(int i=0; i<tabs.length; i++) if(tabs[i].equals(String.valueOf(object))) return i;
        return -1;
    }
}

