package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by mansivirani on 30/10/16.
 */
public class DocumentWordOccurrence  implements Serializable {
    Integer docId;
    ArrayList<Integer> occurrence;

    DocumentWordOccurrence(Integer docId, int pos) {
        this.docId = docId;
        this.occurrence = new ArrayList<>();
        this.occurrence.add(pos);
    }
}