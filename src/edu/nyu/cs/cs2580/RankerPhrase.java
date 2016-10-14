package edu.nyu.cs.cs2580;


import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Use this template to implement the phrase ranker for HW1.
 * 
 * @author congyu
 * @author fdiaz
 */
public class RankerPhrase extends Ranker {

  public RankerPhrase(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    // @CS2580: fill in your code here.
    return all;
  }

  int phrasecompare(String[] Query, int docID) {

      Document doc = _indexer.getDoc(docID);
      Vector<String> docwords = ((DocumentFull) doc).getConvertedBodyTokens();

      double score = 0.0;
      int i = 0;
      int bigramfreq = 0;
      int ctr=0;

      if (Query.length == 1)
          bigramfreq = getfreq(docID);

      else {

          for (int j = 0; j < docwords.size(); j++) {

              
              while(i!=Query.length-1){

                  if(Query[i]==docwords.get(j)){

                      if(Query[i+1]==docwords.get(j+1)){

                          bigramfreq++;
                          i++;
                          j++;
                      }
                      else
                          i++;
                  }
                  else
                      i++;
              }

              i=0;

          }

      }
      return bigramfreq;
  }

int getfreq(int docID){
    return 20;
}
}
