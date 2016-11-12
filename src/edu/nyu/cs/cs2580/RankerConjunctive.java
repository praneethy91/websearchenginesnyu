package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.*;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Instructors' code for illustration purpose. Non-tested code.
 *
 * @author congyu
 */
public class RankerConjunctive extends Ranker {

  public RankerConjunctive(Options options,
                           CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    Queue<ScoredDocument> rankQueue = new PriorityQueue<>();
    Document doc;
    int docid = -1;

    try {
      _indexer.loadIndex(query);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    while ((doc = _indexer.nextDoc(query, docid)) != null) {
      rankQueue.add(new ScoredDocument(doc, 1.0));
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
      docid = doc._docid;
    }

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDoc = null;
    while ((scoredDoc = rankQueue.poll()) != null) {
      results.add(scoredDoc);
    }
    Collections.sort(results, Collections.reverseOrder());
    return results;
  }

  @Override
  public Vector<TermProbability> querySimilarity(Query query, int numDocs, int numTerms) {
    throw new UnsupportedOperationException("This ranker does not support the Query Similarity model");
  }
}