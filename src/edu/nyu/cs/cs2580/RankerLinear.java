package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @CS2580: Use this template to implement the linear ranker for HW1. You must
 * use the provided _betaXYZ for combining the signals.
 * 
 * @author congyu
 * @author fdiaz
 */
public class RankerLinear extends Ranker {
  private float _betaCosine = 1.0f;
  private float _betaQl = 1.0f;
  private float _betaPhrase = 1.0f;
  private float _betaNumviews = 1.0f;

  public RankerLinear(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    _betaCosine = options._betaValues.get("beta_cosine");
    _betaQl = options._betaValues.get("beta_ql");
    _betaPhrase = options._betaValues.get("beta_phrase");
    _betaNumviews = options._betaValues.get("beta_numviews");
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    System.out.println("  with beta values" +
        ": cosine=" + Float.toString(_betaCosine) +
        ", ql=" + Float.toString(_betaQl) +
        ", phrase=" + Float.toString(_betaPhrase) +
        ", numviews=" + Float.toString(_betaNumviews));
    Vector<ScoredDocument> results = new Vector<>();

    /*Implemented a new run query method which returns all ranked
     *documents in the order in which they appear in the indexer
     * This way we apply the coefficients to each ranking model
     * and sort the entire documents again based on linear combination
     * of scores
     */
    _arguments._rankerType = CgiArguments.RankerType.COSINE;
    Ranker cosineRanker = new RankerCosine(_options, _arguments, _indexer);
    Vector<ScoredDocument> cosineRankedDocs = cosineRanker.runQueryOriginalOrder(query);

    _arguments._rankerType = CgiArguments.RankerType.QL;
    Ranker qlRanker = new RankerQl(_options, _arguments, _indexer);
    Vector<ScoredDocument> qlRankedDocs = qlRanker.runQueryOriginalOrder(query);

    _arguments._rankerType = CgiArguments.RankerType.PHRASE;
    Ranker phraseRanker = new RankerPhrase(_options, _arguments, _indexer);
    Vector<ScoredDocument> phraseRankedDocs = phraseRanker.runQueryOriginalOrder(query);

    _arguments._rankerType = CgiArguments.RankerType.NUMVIEWS;
    Ranker numViewsRanker = new RankerNumViews(_options, _arguments, _indexer);
    Vector<ScoredDocument> numViewsRankedDocs = numViewsRanker.runQueryOriginalOrder(query);

    for(int i = 0; i < _indexer.numDocs(); i++) {
      double score =
              _betaCosine*cosineRankedDocs.get(i).getScore()
              + _betaQl*qlRankedDocs.get(i).getScore()
              + _betaPhrase*phraseRankedDocs.get(i).getScore()
              + _betaNumviews*numViewsRankedDocs.get(i).getScore();

      // We are using cosine ranked docs as an intermediate storage for sorting to save space
      cosineRankedDocs.get(i).setScore(score);
    }

    Collections.sort(cosineRankedDocs, Collections.reverseOrder());
    for (int i = 0; i < cosineRankedDocs.size() && i < numResults; ++i) {
      results.add(cosineRankedDocs.get(i));
    }

    return results;
  }
  public ScoredDocument scoreDocument(Query query, int did) {
    throw new NotImplementedException();
  }
}
