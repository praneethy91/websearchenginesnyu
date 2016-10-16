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

      for(Query query : queries) {
          Vector<ScoredDocument> all = new Vector<ScoredDocument>();
          for (int i = 0; i < _indexer.numDocs(); ++i) {
              all.add(scoreDocument(query, i));
          }
          Collections.sort(all, Collections.reverseOrder());
          for (int i = 0; i < all.size() && i < numResults; ++i) {
              results.add(all.get(i));
          }
      }

      return results;

  }
    private ScoredDocument scoreDocument(Query query, int did) {
        query.processQuery();
        DocumentFull docFull = (DocumentFull) _indexer.getDoc(did);
        HashMap<String, Double> docTokenCountMap = docFull.getTokenCountMap();

        int docTokenCount = docFull.getConvertedBodyTokens().size();

        Vector<String> docbodytokens = docFull.getConvertedBodyTokens();

        Vector<String> querytokens = query._tokens;

        double score = 0.0;


            if (querytokens.size() == 1)

                score = docTokenCountMap.get(querytokens.firstElement());

            else {

                for (int docbodyindex = 0; docbodyindex < docTokenCount - 1; docbodyindex++) {

                    for (int queryindex = 0; queryindex != querytokens.size() - 1 ; queryindex++) {

                        if (querytokens.get(queryindex).equals(docbodytokens.get(docbodyindex)) && querytokens.get(queryindex + 1).equals(docbodytokens.get(docbodyindex + 1))) {

                                score++;

                        }
                    }

                }
            }


        return new ScoredDocument(query._query, docFull, score);
    }

}
