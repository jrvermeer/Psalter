package com.jrvermeer.psalter.Core.Models;

import android.provider.MediaStore;

/**
 * Created by Jonathan on 3/27/2017.
 */

public class Psalter {

    private int id;
    private int number;
    private int psalm;
    private String title;
    private String lyrics;
    private int numverses;
    private int NumVersesInsideStaff;
    private String heading;
    private String AudioFileName;
    private String ScoreFileName;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeading() {
        return heading;
    }
    public void setHeading(String heading) {
        this.heading = heading;
    }

    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }

    public Integer getPsalm() { return psalm; }
    public void setPsalm(Integer psalm) {
        this.psalm = psalm;
    }

    public String getLyrics() {
        return lyrics;
    }
    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public int getNumverses() {
        return numverses;
    }
    public void setNumverses(int numverses) {
        this.numverses = numverses;
    }

    public String getAudioFileName(){ return AudioFileName; }
    public void setAudioFileName(String name) { this.AudioFileName = name; }

    public String getScoreFileName(){ return ScoreFileName; }
    public void setScoreFileName(String name) { this.ScoreFileName = name; }

    public int getNumVersesInsideStaff(){ return NumVersesInsideStaff; }
    public void setNumVersesInsideStaff(int num) { this.NumVersesInsideStaff = num; }

    public String getSubtitleText(){
        if(psalm == 0){
            return "Lords Prayer";
        } else return "Psalm " + psalm;
    }

    public String getSubtitleLink(){
        String passage;
        if(psalm == 0){ //Lords prayer
            passage = "Matthew+6:9-13";
        }
        else passage = "Psalm+" + psalm;
        return String.format("<a href=https://www.biblegateway.com/passage?search=%s>%s</a>", passage, getSubtitleText());
    }
}
