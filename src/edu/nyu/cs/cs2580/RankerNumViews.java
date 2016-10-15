package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Use this template to implement the numviews ranker for HW1.
 * 
 * @author congyu
 * @author fdiaz
 */
public class RankerNumViews extends Ranker {

  public RankerNumViews(Options options,
                        CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Vector<Query> queries, int numResults) {
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

  private ScoredDocument scoreDocument(Query query, int did) {
    DocumentFull docFull = (DocumentFull) _indexer.getDoc(did);

    double score = docFull.getNumViews();

    return new ScoredDocument(query._query, docFull, score);
  }
}
