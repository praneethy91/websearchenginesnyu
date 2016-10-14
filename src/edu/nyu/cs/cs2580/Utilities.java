package edu.nyu.cs.cs2580;

import java.util.Vector;

/**
 * Created by Praneeth on 10/13/2016.
 */
public class Utilities {
    public static int termFrequency(String term, Vector<String> searchTokens){
        int frequencyofTermInDoc = 0;
        for(String token: searchTokens) {
            if(token == term) {
                frequencyofTermInDoc++;
            }
        }

        return frequencyofTermInDoc;
    }
}
