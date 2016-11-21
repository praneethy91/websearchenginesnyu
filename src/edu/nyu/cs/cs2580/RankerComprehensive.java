package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.*;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 * from HW2. The new Ranker should now combine both term features and the
 * document-level features including the PageRank and the NumViews. 
 */
public class RankerComprehensive extends Ranker {

  public RankerComprehensive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  private HashMap<String, Double> tokenFrequencyCache = null;

  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
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
      ScoredDocument scoredDocument = scoreDocument(query, doc);
      rankQueue.add(scoredDocument);
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
      docid = doc._docid;
    }

    double minScore = Integer.MAX_VALUE;
    double maxScore = Integer.MIN_VALUE;

    double minPageRank = Integer.MAX_VALUE;
    double maxPageRank = Integer.MIN_VALUE;

    double minNumViews = Integer.MAX_VALUE;
    double maxNumViews = Integer.MIN_VALUE;

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDocument;
    while((scoredDocument = rankQueue.poll()) != null) {
      if(minScore > scoredDocument.getScore()) {
        minScore = scoredDocument.getScore();
      }
      if(maxScore < scoredDocument.getScore()) {
        maxScore = scoredDocument.getScore();
      }
      if(minPageRank > _indexer._pageRanks.get(scoredDocument.getID())) {
        minPageRank = _indexer._pageRanks.get(scoredDocument.getID());
      }
      if(maxPageRank < _indexer._pageRanks.get(scoredDocument.getID())) {
        maxPageRank = _indexer._pageRanks.get(scoredDocument.getID());
      }
      if(minNumViews > _indexer._numViews.get(scoredDocument.getID())) {
        minNumViews = _indexer._numViews.get(scoredDocument.getID());
      }
      if(maxNumViews < _indexer._numViews.get(scoredDocument.getID())) {
        maxNumViews = _indexer._numViews.get(scoredDocument.getID());
      }
      results.add(scoredDocument);
    }

    double scoreNm = maxScore - minScore;
    double pageRankNm = maxPageRank - minPageRank;
    double numviewsNm = maxNumViews - minNumViews;

    for (ScoredDocument scoredDoc : results) {
      double score = scoredDoc.getScore();
      double numViews = _indexer._numViews.get(scoredDoc.getID());
      scoredDoc.setNumViews(numViews);
      double pageRank = _indexer._pageRanks.get(scoredDoc.getID());
      scoredDoc.setPageRank(pageRank);
      double finalScore = 0.33*(score - minScore)/scoreNm + 0.33*(numViews - minNumViews)/numviewsNm + 0.33*(pageRank - minPageRank)/pageRankNm;
      scoredDoc.setScore(finalScore);
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
  public Vector<TermProbability> querySimilarity(Query query, int numDocs, int numTerms) {
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
