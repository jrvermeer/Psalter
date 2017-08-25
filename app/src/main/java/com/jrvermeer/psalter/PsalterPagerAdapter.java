package com.jrvermeer.psalter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Created by Jonathan on 3/27/2017.
 */

public class PsalterPagerAdapter extends PagerAdapter {
    private Context _context;
    private PsalterDb db;
    private HashMap<Integer, Psalter> items;

    public PsalterPagerAdapter(Context context, PsalterDb psalterDb){
        _context = context;
        db = psalterDb;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position){
        try{
            Psalter psalter = getPsalter(position);

            LayoutInflater inflater = LayoutInflater.from(_context);
            ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.psalter_layout, collection, false);

            ((TextView)layout.findViewById(R.id.tvPagerLyrics)).setText(psalter.getLyrics());
            String subtitle;
            if(psalter.getPsalm() != 0){
                subtitle = "Psalm " + psalter.getPsalm();
            }
            else subtitle = "Lords Prayer";
            ((TextView)layout.findViewById(R.id.tvPagerPsalm)).setText(subtitle);
            collection.addView(layout);
            return layout;
        } catch (Exception ex){
            return null;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if(object != null){
            container.removeView((View)object);
        }
    }

    @Override
    public int getCount() {
        return db.getCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
            try{
                return "#" + getPsalter(position).getNumber();
            } catch (Exception ex){
                return "?";
            }

        }


    public Psalter getPsalter(int pagerIndex){
        return db.getPsalter(pagerIndex + 1);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
