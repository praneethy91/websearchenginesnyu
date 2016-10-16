package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * This is the abstract Ranker class for all concrete Ranker implementations.
 *
 * Use {@link Ranker.Factory} to create your concrete Ranker implementation. Do
 * NOT change the interface in this class!
 *
 * In HW1: {@link RankerFullScan} is the instructor's simple ranker and students
 * implement four additional concrete Rankers. {@link RankerLinear} is the 
 * template for the Linear ranker and students must use the four beta fields
 * to combine the four signals.
 *
 * @author congyu
 * @author fdiaz
 */
public abstract class Ranker {
  // Options to configure each concrete Ranker.
  protected Options _options;
  // CGI arguments user provide through the URL.
  protected CgiArguments _arguments;

  // The Indexer via which documents are retrieved, see {@code IndexerFullScan}
  // for a concrete implementation. N.B. Be careful about thread safety here.
  protected Indexer _indexer;

  /**
   * Constructor: the construction of the Ranker requires an Indexer.
   */
  protected Ranker(Options options, CgiArguments arguments, Indexer indexer) {
    _options = options;
    _arguments = arguments;
    _indexer = indexer;
  }

  /**
   * Processes one query.
   * @param queries the parsed user query vector of queries
   * @param numResults number of results to return for each query
   * @return Up to {@code numResults} scored documents in ranked order for each query
   *         The ranked/scored documents are appended for each query.
   */

  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Vector<ScoredDocument> results = new Vector<>();
    Vector<ScoredDocument> all = new Vector<>();
    for (int i = 0; i < _indexer.numDocs(); ++i) {
      all.add(scoreDocument(query, i));
    }
    Collections.sort(all, Collections.reverseOrder());
    for (int i = 0; i < all.size() && i < numResults; ++i) {
      results.add(all.get(i));
    }
    return results;
  }

  public abstract ScoredDocument scoreDocument(Query query, int did);

  /**
   * All Rankers must be created through this factory class based on the
   * provided {@code arguments}.
   */
  public static class Factory {
    public static Ranker getRankerByArguments(CgiArguments arguments,
        Options options, Indexer indexer) {
      switch (arguments._rankerType) {
      case FULLSCAN:
        return new RankerFullScan(options, arguments, indexer);
      case COSINE:
        return new RankerCosine(options, arguments, indexer);
      case QL:
        return new RankerQl(options, arguments, indexer);
      case PHRASE:
        return new RankerPhrase(options, arguments, indexer);
      case NUMVIEWS:
        return new RankerNumViews(options, arguments, indexer);
      case LINEAR:
        return new RankerLinear(options, arguments, indexer);
      case NONE:
        // Fall through intended
      default:
        // Do nothing.
      }
      return null;
    }
  }
}
