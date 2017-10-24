package com.jrvermeer.psalter.Models;

/**
 * Created by Jonathan on 3/27/2017.
 */

public class Psalter {

    private int number;
    private int psalm;
    private String lyrics;
    private int numverses;
    private String heading;

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

    public String getDisplaySubtitle(){
        if(psalm == 0){
            return "Lords Prayer";
        } else return "Psalm " + psalm;
    }

    public String getClickableLink(){
        String passage;
        if(psalm == 0){ //Lords prayer
            passage = "Matthew+6:9-13";
        }
        else passage = "Psalm+" + psalm;
        return String.format("<a href=https://www.biblegateway.com/passage?search=%s>%s</a>", passage, getDisplaySubtitle());
    }

    public String getAudioFileName(){
        return "_" + getNumber();
    }
}
