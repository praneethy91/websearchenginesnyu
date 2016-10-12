package edu.nyu.cs.cs2580;

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
   * @param query the parsed user query
   * @param numResults number of results to return
   * @return Up to {@code numResults} scored documents in ranked order
   */
  public abstract Vector<ScoredDocument> runQuery(Query query, int numResults);

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
        return new RankerNumviews(options, arguments, indexer);
      case NUMVIEWS:
        return new RankerPhrase(options, arguments, indexer);
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
