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
      int phrasefreq = 0;

      if (Query.length == 1)
          phrasefreq = getfreq(docID);

      else {

          for (int j = 0; j < docwords.size(); j++) {

              if (docwords.get(j).equals(Query[i])) {
                  i++;
                  if (i == Query.length) {
                      phrasefreq++;
                      i = 0;
                  } else
                      i = 0;
              }
          }

      }
      return phrasefreq;
  }

int getfreq(int docID){
    return 20;
}
}
