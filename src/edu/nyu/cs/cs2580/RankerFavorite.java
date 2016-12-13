package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.*;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

  public RankerFavorite(Options options,
                        CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  private HashMap<String, Double> tokenFrequencyCache = null;

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) throws IOException {
    tokenFrequencyCache = new HashMap<>();
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    Document doc = null;
    int docid = -1;

    try {
      _indexer.loadIndex(query);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    while ((doc = _indexer.nextDoc(query, docid)) != null) {
      rankQueue.add(scoreDocument(query, doc));
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
      docid = doc._docid;
    }

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDoc = null;
    while ((scoredDoc = rankQueue.poll()) != null) {
      Collection<String> categories = _indexer.getCategories(scoredDoc.getUrl());
      scoredDoc.setCategories(categories);
      scoredDoc.setInternetUrl(_indexer.getURL(scoredDoc.getUrl()));
      results.add(scoredDoc);
    }
    Collections.sort(results, Collections.reverseOrder());
    return results;
  }

  public ScoredDocument scoreDocument(Query query, Document document) {

    //TODO: Need to implement total term frequency (free words, not phrases)
    double totalTermFrequencyInCorpus = _indexer.totalTermFrequency();
    DocumentIndexed docIndexed = (DocumentIndexed) document;

    double score = 1.0;
    double lambda = 0.5;

    // TODO: Need to get document token count from _indexer or DocumentIndexed class
    double docTokenCount = _indexer.getTokensPerDoc(docIndexed._docid);

    for (Map.Entry<QueryToken, Integer> queryTokenEntry: docIndexed.quertTokenCount.entrySet()) {
      double queryTokenFrequency = queryTokenEntry.getValue();

      // TODO: Need to get the frequency of the token (should support free words and phrases both) in the corpus
      double frequencyOfTokenInCorpus = getQueryTokenCountInCorpus(queryTokenEntry.getKey());
      score = score * ((1 - lambda)*queryTokenFrequency/docTokenCount) + (lambda * frequencyOfTokenInCorpus / totalTermFrequencyInCorpus);
    }

    return new ScoredDocument(docIndexed, score);
  }

  @Override
  public Vector<TermProbability> querySimilarity(Query query, int numDocs, int numTerms) throws IOException {
    Vector<ScoredDocument> documents = runQuery(query, numDocs);
    Vector<Integer> docIds = new Vector<>();
    for(ScoredDocument document: documents) {
      docIds.add(document.getID());
    }
    Collections.sort(docIds);
    return _indexer.getHighestTermProbabilitiesForDocs(docIds, numTerms);
  }

  private double getQueryTokenCountInCorpus(QueryToken queryToken) {
    if(!tokenFrequencyCache.containsKey(queryToken.getToken())) {
      tokenFrequencyCache.put(queryToken.getToken(), (double)_indexer.getQueryTokenCountInCorpus(queryToken));
      return tokenFrequencyCache.get(queryToken.getToken());
    }
    else {
      return tokenFrequencyCache.get(queryToken.getToken());
    }
  }
}