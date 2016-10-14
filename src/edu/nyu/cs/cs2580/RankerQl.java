package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
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
    for (int i = 0; i < _indexer.numDocs(); ++i) {
      double totalTermFrequencyInCorpus = _indexer.totalTermFrequency();
      all.add(scoreDocument(query, i, totalTermFrequencyInCorpus));
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
   * so lookups are fast. Also the corpus statistics data required for the query likelihood
   * ranker are within the index for fast computation. We are using Language model probabilities
   * with Jelinek-Mercer smoothing for the QL ranker.
   * @param query
   * @param did
   */
  private ScoredDocument scoreDocument(Query query, int did, double totalTermFrequencyInCorpus) {
    DocumentFull docFull = (DocumentFull) _indexer.getDoc(did);
    HashMap<String, Double> docTokenCountMap = docFull.getTokenCountMap();

    double score = 1.0;
    double lambda = 0.5;
    double docTokenCount = docFull.getBodyTokens().size();

    for (String queryToken: query._tokens) {
      double queryTokenFrequency = 0.0;
      if(docTokenCountMap.containsKey(queryToken)) {
        queryTokenFrequency = docTokenCountMap.get(queryToken);
      }

      double frequencyOfTokenInCorpus = _indexer.corpusTermFrequency(queryToken);
      score = score * ((1 - lambda)*queryTokenFrequency/docTokenCount) + (lambda * frequencyOfTokenInCorpus / totalTermFrequencyInCorpus);
    }

    return new ScoredDocument(query._query, docFull, score);
  }
}
