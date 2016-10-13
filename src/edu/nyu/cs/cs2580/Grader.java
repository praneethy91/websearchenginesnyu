package edu.nyu.cs.cs2580;

/**
 * Grading criteria.
 * 
 * Grading will be done via the public APIs for the four main classes:
 *   Indexer, Ranker, Document, Query
 * Do NOT change the public APIs for those classes.
 * 
 * In HW1, we will examine your index implementation through the following
 * tasks.
 *
 *  1) Implement RankerCosine, RankerQl, RankerNumviews, RankerPhrase
 *     (40 points)
 *
 *  2) Implement RankerLinear (20 points)
 *
 *  3) Implement Evaluator (40 points)
 *
 * @author congyu
 */
public class Grader {
  Indexer _indexer;
  Ranker _ranker;

  public Grader() { }

  public void setIndexer(Indexer indexer) {
    _indexer = indexer;
  }

  public void setRanker(Ranker ranker) {
    _ranker = ranker;
  }

  public static void main(String[] args) { }
}
