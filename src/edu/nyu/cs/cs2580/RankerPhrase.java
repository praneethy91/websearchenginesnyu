package edu.nyu.cs.cs2580;


import java.util.Collections;
import java.util.HashMap;
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
  public Vector<ScoredDocument> runQuery(Vector<Query> queries, int numResults) {
      Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    // @CS2580: fill in your code here.

      int numDocs = _indexer.numDocs();

      for(Query query : queries) {
          Vector<ScoredDocument> all = new Vector<ScoredDocument>();
          for (int i = 0; i < _indexer.numDocs(); ++i) {
              all.add(scoreDocument(query, i, numDocs));
          }
          Collections.sort(all, Collections.reverseOrder());
          for (int i = 0; i < all.size() && i < numResults; ++i) {
              results.add(all.get(i));
          }
      }

      return results;

  }
    private ScoredDocument scoreDocument(Query query, int did, int numDocs) {
        DocumentFull docFull = (DocumentFull) _indexer.getDoc(did);
        HashMap<String, Double> docTokenCountMap = docFull.getTokenCountMap();

        int docTokenCount = docFull.getBodyTokens().size();

        double score = 0.0;
        int i = 0;

        for (int k=0;k<numDocs;++k) {

            if (query._tokens.size() == 1)

                score = docTokenCountMap.get(query);

            else {

                for (int j = 0; j < docTokenCount - 1; j++) {


                    while (i != query._tokens.size() - 1) {

                        if (query._tokens.get(i).equals(docFull.getConvertedBodyTokens().get(j))) {

                            if (query._tokens.get(i + 1).equals(docFull.getConvertedBodyTokens().get(j + 1))) {

                                score++;
                                i++;
                            } else
                                i++;
                        } else
                            i++;
                    }
                    i = 0;
                }
            }
        }
        return new ScoredDocument(query._query, docFull, score);
    }

}
