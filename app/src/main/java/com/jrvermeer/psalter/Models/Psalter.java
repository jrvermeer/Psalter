package com.jrvermeer.psalter.Models;

/**
 * Created by Jonathan on 3/27/2017.
 */

public class Psalter {

    private int number;
    private int psalm;
    private String lyrics;
    private int numverses;

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

    public String getDisplayTitle(){
        return "#" + number;
    }

    public String getDisplaySubtitle(){
        if(psalm == 0){
            return "Lords Prayer";
        } else return "Psalm " + psalm;
    }
}