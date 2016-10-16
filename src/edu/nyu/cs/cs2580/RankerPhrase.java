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
  public Vector<ScoredDocument> runQuery(Vector<Query> queries  , int numResults) {
    Vector<ScoredDocument> results = new Vector<>();
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

  private ScoredDocument scoreDocument(Query query, int did) {DocumentFull docFull = (DocumentFull) _indexer.getDoc(did);

    HashMap<String, Double> docTokenCountMap = docFull.getTokenCountMap();
    Vector<String> docbodytokens = docFull.getConvertedBodyTokens();
    int docTokenCount = docbodytokens.size();
    Vector<String> queryTokens = query._tokens;

    double score = 0.0;
    if (queryTokens.size() == 1) {
      if(docTokenCountMap.containsKey(queryTokens.get(0))) {
        score = docTokenCountMap.get(queryTokens.get(0));
      }
      return new ScoredDocument(query._query, docFull, score);
    }

    for (int docBodyIndex = 0; docBodyIndex < docTokenCount - 1; docBodyIndex++) {
      for (int queryIndex = 0; queryIndex < queryTokens.size() - 1 ; queryIndex++) {
        String queryTokenFirst = queryTokens.get(queryIndex);
        String queryTokenSecond = queryTokens.get(queryIndex + 1);
        String docBodyFirst = docbodytokens.get(docBodyIndex);
        String docBodySecond = docbodytokens.get(docBodyIndex + 1);
        if (queryTokenFirst.equals(docBodyFirst)
                && queryTokenSecond.equals(docBodySecond)) {
          score++;
        }
      }
    }

    return new ScoredDocument(query._query, docFull, score);
  }
}
