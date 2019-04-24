package com.jrvermeer.psalter.UI.Adaptors;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository;
import com.jrvermeer.psalter.Core.Models.Psalter;
import com.jrvermeer.psalter.Infrastructure.Helpers;
import com.jrvermeer.psalter.Infrastructure.PsalterDb;
import com.jrvermeer.psalter.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Jonathan on 4/4/2017.
 */

//uses data from PsalterDb class to fill search "screen" with items
public class PsalterSearchAdapter extends ArrayAdapter<Psalter> {
    private String query;
    private IPsalterRepository psalterRepository;
    private LayoutInflater inflater;
    private boolean psalmSearch;
    private Context context;

    public PsalterSearchAdapter(@NonNull Context context, IPsalterRepository psalterRepository){
        super(context, R.layout.search_results_layout);
        this.psalterRepository = psalterRepository;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void queryPsalter(String searchQuery){
        query = getFilteredQuery(searchQuery.toLowerCase());
        showResults(psalterRepository.searchPsalter(query));
        psalmSearch = false;
    }
    public void getAllFromPsalm(int psalm){
        showResults(psalterRepository.getPsalm(psalm));
        psalmSearch = true;
    }
    private void showResults(Psalter[] results){
        clear();
        addAll(results);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.search_results_layout, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }
            else holder = (ViewHolder)convertView.getTag();

            Psalter psalter = getItem(position);
            holder.tvNumber.setText(String.valueOf(psalter.getNumber()));
            if(psalmSearch){
                holder.tvLyrics.setText(psalter.getLyrics().substring(0, getVerseEndIndex(psalter.getLyrics(), 0)));
            }
            else{ //lyric search
                String filterLyrics = psalter.getLyrics().toLowerCase();

                // new


                // build SpannableStringBuilder of only the verses that contain query
                SpannableStringBuilder viewText = new SpannableStringBuilder();
                List<Integer> verseHitIndices = new ArrayList<>(); // could be verse number, could be chorus, so build a list of indexes
                boolean first = true;
                for(int i : Helpers.allIndexesOf(filterLyrics, query)){
                    int iStart = getVerseStartIndex(filterLyrics, i);

                    if(!verseHitIndices.contains(iStart)){ // if 1st occurance of query in this verse, add verse to display
                        verseHitIndices.add(iStart);
                        int iEnd = getVerseEndIndex(filterLyrics, i);
                        if(!first) viewText.append("\n\n");
                        first = false;
                        viewText.append(psalter.getLyrics().substring(iStart, iEnd));
                    }
                }
                // highlight all instances of query
                String filterText = viewText.toString().toLowerCase();
                int iHighlightStart = filterText.indexOf(query);
                while(iHighlightStart >= 0) {
                    int iHighlightEnd = iHighlightStart + query.length();
                    viewText.setSpan(new StyleSpan(Typeface.BOLD), iHighlightStart, iHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    viewText.setSpan(new AbsoluteSizeSpan((int)(holder.tvLyrics.getTextSize() * 1.5)), iHighlightStart, iHighlightEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    iHighlightStart = filterText.indexOf(query, iHighlightStart + 1);
                }

                holder.tvLyrics.setText(viewText);
            }
            return convertView;

        } catch (Exception ex){
            return  convertView;
        }
    }
    //filter out characters
    private String getFilteredQuery(String rawQuery){
        String filteredQuery = rawQuery;
        for(String ignore : context.getResources().getStringArray(R.array.search_ignore_strings)){
            filteredQuery = filteredQuery.replace(ignore, "");
        }
        return filteredQuery;
    }


    //given random index in lyrics string, return index of the beginning of that verse
    private int getVerseStartIndex(String lyrics, int queryStartIndex){
        int startIndex = lyrics.lastIndexOf("\n\n", queryStartIndex);
        if(startIndex < 0) return 0;
        else return startIndex + 2; // don't need to display the 2 newline chars
    }

    //given random index in lyrics string, return index of the end of that verse
    private int getVerseEndIndex(String lyrics, int queryStartIndex){
        int i = lyrics.indexOf("\n\n", queryStartIndex);
        if(i > 0) return i;
        else return  lyrics.length();
    }

    public static class ViewHolder{
        @BindView(R.id.tvSearchNumber) public TextView tvNumber;
        @BindView(R.id.tvSearchLyrics) public TextView tvLyrics;
        public ViewHolder(View view){
            ButterKnife.bind(this, view);
        }
    }
}
