package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Use this template to implement the cosine ranker for HW1.
 *
 * @author congyu
 * @author fdiaz
 */
public class RankerCosine extends Ranker {

  public RankerCosine(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    for (int i = 0; i < _indexer.numDocs(); ++i) {
      all.add(scoreDocument(query, i));
    }
    Collections.sort(all, Collections.reverseOrder());
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    for (int i = 0; i < all.size() && i < numResults; ++i) {
      results.add(all.get(i));
    }
    return results;
  }

  private ScoredDocument scoreDocument(Query query, int did) {

    // Get the document tokens.
    DocumentFull doc = (DocumentFull) _indexer.getDoc(did);
    HashMap<String, Double> docTokenCountMap = doc.getTfIdfRepresentation();

    // Score the document. Here we have provided a very simple ranking model,
    // where a document is scored 1.0 if it gets hit by at least one query term.
    double score = 0.0;
    int totalQueryTerms = query._tokens.size();
    for (Map.Entry<String, Integer> queryTermWithCount : query._tokenCountMap.entrySet()) {
      String queryTerm = queryTermWithCount.getKey();
      int queryTermCount = queryTermWithCount.getValue();
      if(!docTokenCountMap.containsKey(queryTerm))
      {
        continue;
      }

      // We are not normalizing the queryTf. Just dividing by the summation
      // of all frequencies of the terms in the query vector (total token count)
      double queryTf = (queryTermCount*1.0/totalQueryTerms);

      double documentNormalizedTfIdf = docTokenCountMap.get(queryTerm);
      score += queryTf*documentNormalizedTfIdf;
    }

    return new ScoredDocument(query._query, doc, score);
  }
}
