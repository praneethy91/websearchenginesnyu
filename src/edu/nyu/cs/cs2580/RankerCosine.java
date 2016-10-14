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
    int numDocs = _indexer.numDocs();
    for (int i = 0; i < _indexer.numDocs(); ++i) {
      all.add(scoreDocument(query, i, numDocs));
    }
    Collections.sort(all, Collections.reverseOrder());
    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    for (int i = 0; i < all.size() && i < numResults; ++i) {
      results.add(all.get(i));
    }
    return results;
  }

  /**
    *The frequency of terms in a document is within the document itself in a frequency map
    * so lookups are fast. Also each document has its tfidf normalization factors
    * so we don't need to compute, we can load it of the document from the index
    * @param query
    * @param did
    * @param numdocs
    */
  private ScoredDocument scoreDocument(Query query, int did, int numdocs) {

    // Get the document tokens.
    DocumentFull docFull = (DocumentFull) _indexer.getDoc(did);
    HashMap<String, Double> docTokenCountMap = docFull.getTokenCountMap();

    double score = 0.0;
    double queryTfNormalizationFactor = 0.0;
    for (Map.Entry<String, Integer> queryTermWithCount : query._tokenCountMap.entrySet()) {
      String queryTerm = queryTermWithCount.getKey();
      int queryTermCount = queryTermWithCount.getValue();
      if(!docTokenCountMap.containsKey(queryTerm))
      {
        continue;
      }

      double queryTermFrequency = docTokenCountMap.get(queryTerm);
      double docFrequencyOfTerm = _indexer.corpusDocFrequencyByTerm(queryTerm)*1.0;
      double tfIdfTermWithoutNormalization = (Math.log(queryTermFrequency) + 1.0)*Math.log((numdocs*1.0)/docFrequencyOfTerm);
      score += queryTermCount*tfIdfTermWithoutNormalization;
      queryTfNormalizationFactor += Math.pow(queryTermCount, 2);
    }

    if(score != 0) {
      queryTfNormalizationFactor = Math.sqrt(queryTfNormalizationFactor);
      score /= docFull.getNormalizationFactorTfIdf();
      score /= queryTfNormalizationFactor;
    }

    return new ScoredDocument(query._query, docFull, score);
  }
}
