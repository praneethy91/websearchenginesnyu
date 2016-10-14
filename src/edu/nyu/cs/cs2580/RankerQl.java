package edu.nyu.cs.cs2580;

import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Use this template to implement the query likelihood ranker for HW1.
 * 
 * @author congyu
 * @author fdiaz
 */
public class RankerQl extends Ranker {

  public RankerQl(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();

    return all;
    // @CS2580: fill in your code here.
  }

  public double Rank(int docID, String[] Query) {
    double lambda = (float) 0.5;
    double p = (float) 0.0;
    double c = 100;
    for (int i = 0; i < Query.length; i++) {
      p = p + ((1 - lambda) * getfreq(docID, Query[i]) / docwordcount(docID)) + (lambda * getfreqcoll(Query[i]) / c);
    }

    return p;
  }

  String[] Query = {"big", "brown", "box"};

  double getfreq(int docID, String word){

    return 6;

  }

  double docwordcount(int docID){
    return 30.0;

  }

  double getfreqcoll(String word){

    return 20;
  }


}











