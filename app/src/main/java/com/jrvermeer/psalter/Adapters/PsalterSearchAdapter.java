package com.jrvermeer.psalter.Adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jrvermeer.psalter.Models.Psalter;
import com.jrvermeer.psalter.PsalterDb;
import com.jrvermeer.psalter.R;

/**
 * Created by Jonathan on 4/4/2017.
 */

//uses data from PsalterDb class to fill search "screen" with items
public class PsalterSearchAdapter extends ArrayAdapter<Psalter> {
    private String query;
    private PsalterDb db;
    private boolean psalmSearch;
    public static char[] searchIgnoreChars = new char[] { '\'', ',', ';', ':', '\"'};
    public PsalterSearchAdapter(@NonNull Context context, PsalterDb psalterDb){
        super(context, R.layout.search_results_layout);
        db = psalterDb;
    }

    public void queryPsalter(String searchQuery){
        query = getFilteredQuery(searchQuery.toLowerCase());
        psalmSearch = false;
        String[] arrQuery = query.split(" ");
        // if it's a psalm search
        if(arrQuery.length == 2 && arrQuery[0].toLowerCase().equals("psalm")){
            try{
                int psalm = Integer.parseInt(arrQuery[1]);
                showResults(db.getPsalm(psalm));
                psalmSearch = true;
            }
            catch (NumberFormatException ex){
                showResults(db.searchPsalter(query));
            }
        }
        else {
            showResults(db.searchPsalter(query));
        }
    }
    private void showResults(Psalter[] results){
        clear();
        addAll(results);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            Psalter psalter = getItem(position);
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.search_results_layout, parent, false);
            }
            TextView tvLyrics = (TextView) convertView.findViewById(R.id.tvSearchLyrics);
            try{
                if(psalmSearch){
                    String displayVerse = psalter.getLyrics().substring(0, psalter.getLyrics().indexOf("\n\n"));
                    tvLyrics.setText(displayVerse);
                }
                else{
                    String filterLyrics = psalter.getLyrics().toLowerCase();
                    int i = filterLyrics.indexOf(query);
                    int iStart = getStartIndex(filterLyrics, i);
                    int iEnd = getEndIndex(filterLyrics, i);

                    String displayVerse = psalter.getLyrics().substring(iStart, iEnd);
                    String filterVerse = displayVerse.toLowerCase();
                    SpannableStringBuilder str = new SpannableStringBuilder(displayVerse);
                    int iHighlightStart = filterVerse.indexOf(query);
                    int iHighlightEnd = iHighlightStart + query.length();
                    str.setSpan(new StyleSpan(Typeface.BOLD), iHighlightStart, iHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    str.setSpan(new AbsoluteSizeSpan((int)(tvLyrics.getTextSize() * 1.5)), iHighlightStart, iHighlightEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tvLyrics.setText(str);
                }

            }catch (Exception ex){
                Log.e("PsalterSearchAdapter", ex.getMessage());
            }
            TextView tvNumber = (TextView) convertView.findViewById(R.id.tvSearchNumber);
            tvNumber.setText(String.valueOf(psalter.getNumber()));
            return convertView;

        } catch (Exception ex){
            return  convertView;
        }
    }
    //filter out characters
    private String getFilteredQuery(String rawQuery){
        String filteredQuery = rawQuery;
        for (char c : searchIgnoreChars) {
            filteredQuery = filteredQuery.replace(String.valueOf(c), "");
        }
        return filteredQuery;
    }
    //given random index in lyrics string, return index of the beginning of that verse
    private int getStartIndex(String lyrics, int queryStartIndex){
        int startIndex = lyrics.lastIndexOf("\n\n", queryStartIndex);
        if(startIndex < 0) return 0;
        else return startIndex + 2; // don't need to display the 2 newline chars
    }
    //given random index in lyrics string, return index of the end of that verse
    private int getEndIndex(String lyrics, int queryStartIndex){
        int i = lyrics.indexOf("\n\n", queryStartIndex);
        if(i > 0) return i;
        else return  lyrics.length() - 1;
    }
}
