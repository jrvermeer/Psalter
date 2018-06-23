package com.jrvermeer.psalter.UI.Adaptors;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jrvermeer.psalter.Core.Contracts.IPagerCallbacks;
import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository;
import com.jrvermeer.psalter.Core.Models.Psalter;
import com.jrvermeer.psalter.Core.Util;
import com.jrvermeer.psalter.Infrastructure.PsalterDb;
import com.jrvermeer.psalter.R;

import java.security.spec.InvalidParameterSpecException;

/**
 * Created by Jonathan on 3/27/2017.
 */

public class PsalterPagerAdapter extends PagerAdapter {
    private Context _context;
    private IPsalterRepository psalterRepository;
    private IPagerCallbacks callbacks;
    private boolean nightMode;
    private boolean showScore = false;

    public PsalterPagerAdapter(Context context, IPsalterRepository repo, IPagerCallbacks callbacks, boolean showScore, boolean nightMode){
        _context = context;
        psalterRepository = repo;
        this.showScore = showScore;
        this.nightMode = nightMode;
        this.callbacks = callbacks;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position){
        try{
             Psalter psalter = psalterRepository.getIndex(position);

            LayoutInflater inflater = LayoutInflater.from(_context);
            ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.psalter_layout, collection, false);

            ((TextView)layout.findViewById(R.id.tvPagerHeading)).setText(psalter.getHeading());
            String lyrics = psalter.getLyrics();
            TextView tvLyrics = ((TextView)layout.findViewById(R.id.tvPagerLyrics));
            if(showScore){
                Drawable score = psalterRepository.getScore(psalter);
                if(score != null){
                    if(nightMode) Util.invertColors(score);
                    ((ImageView)layout.findViewById(R.id.imgScore)).setImageDrawable(score);
                    int lyricStartIndex = lyrics.indexOf((psalter.getNumVersesInsideStaff() + 1) + ". ");
                    if(lyricStartIndex > 0) tvLyrics.setText(lyrics.substring(lyricStartIndex));
                }
                else tvLyrics.setText(lyrics);
            }
            else tvLyrics.setText(lyrics);

            TextView tvPagerPsalm = layout.findViewById(R.id.tvPagerPsalm);
            tvPagerPsalm.setText(Html.fromHtml(psalter.getSubtitleLink()));
            tvPagerPsalm.setMovementMethod(LinkMovementMethod.getInstance());
            collection.addView(layout);

            callbacks.pageCreated(layout, position);
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
        return psalterRepository.getCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        try{
            return psalterRepository.getIndex(position).getTitle();
        } catch (Exception ex){
            return "?";
        }
    }


    public void showScore(){
        if(!showScore){
            showScore = true;
            notifyDataSetChanged();
        }
    }
    public void hideScore(){
        if(showScore){
            showScore = false;
            notifyDataSetChanged();
        }
    }


    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
