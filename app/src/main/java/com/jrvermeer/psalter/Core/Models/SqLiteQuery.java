package com.jrvermeer.psalter.Core.Models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan on 9/4/2017.
 */

public class SqLiteQuery {
    public SqLiteQuery()
    {
        queryParameters = new ArrayList<String>();
    }

    public String getQueryText() {
        return queryText;
    }
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }
    private String queryText;

    public void addParameter(String value){
        queryParameters.add(value);
    }

    public String[] getParameters(){
        return queryParameters.toArray(new String[0]);
    }

    private List<String> queryParameters;
}
