package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDocOnly extends Indexer {

  // We will be soring the inverted doc only representation in this data structure
  private Map<String, List<Integer>> _index = new HashMap<>();

  // This is where we will store the index file
  private final String _indexFile = _options._indexPrefix + "//invertedIndexDocOnly";

  private final String _wikiCorpusDir = _options._corpusPrefix;

  //We will also store the Documents in the DocumentIndexed vector for the rankers
  private Vector<DocumentIndexed> _indexedDocs = new Vector<>();

  public IndexerInvertedDocOnly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    File dir = new File(_wikiCorpusDir);
    File[] directoryListing = dir.listFiles();
    WikiParser wikiParser = null;
    for(File wikiFile : directoryListing) {
      wikiParser = new WikiParser(wikiFile);
      Vector<String> tokens = wikiParser.ParseTokens();
    }
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
  }

  @Override
  public Document getDoc(int docid) {
    return null;
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    return null;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    return 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return 0;
  }

  @Override
  public int documentTermFrequency(String term, int docid) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
}
