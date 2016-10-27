package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {

  public class DocumentWordOccurrence {
    Integer docId;
    Vector<Integer> occurrence;

    DocumentWordOccurrence(Integer docId, Integer pos){
      this.docId = docId;
      this.occurrence = new Vector<>();
      this.occurrence.add(pos);
    }
  }

  private Map<String, Map<Integer,DocumentWordOccurrence>> _index = new HashMap<>();

  // This is where we will store the index file
  private final String _indexFile = _options._indexPrefix + "//invertedIndex.idx";

  //The wiki corpus directory from where we will load files for constructing index
  private final String _wikiCorpusDir = _options._corpusPrefix;

  //We will also store the Documents in the DocumentIndexed vector for the rankers
  private Vector<DocumentIndexed> _indexedDocs = new Vector<>();

  public IndexerInvertedOccurrence(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {

    File dir = new File(_wikiCorpusDir);
    File[] directoryListing = dir.listFiles();
    WikiParser wikiParser = null;
    int docID = 0;
    Vector<String> tokens;

    try {
      for (File wikiFile : directoryListing) {
        try {

          //Parsing and extracting token;
          wikiParser = new WikiParser(wikiFile);
          tokens = wikiParser.ParseTokens();

          //TODO: Document Id
          // Populating and adding DocumentIndexed for this document.
          DocumentIndexed docIndexed = new DocumentIndexed(docID);
          docIndexed.setTitle(wikiParser.getTitle());
          docIndexed.setUrl(wikiParser.getUrl());
          _indexedDocs.add(docIndexed);
        }
        catch(IllegalArgumentException e) {
          // A random non-wiki file, just skip this document
          tokens = new Vector<>();
        }

        // Updating postings lists
        for (int i = 0 ; i < tokens.size() ; i++) {
          String token = tokens.elementAt(i);
          if (!_index.containsKey(token)) {
            _index.put(token, new HashMap<>());
            _index.get(token).put(docID, new DocumentWordOccurrence(docID,i));
          } else {
            _index.get(token).get(docID).occurrence.add(docID);
          }
        }
      }

      //Finally writes to Index file.
      WriteToIndexFile();
    }
    catch(Exception e)
    {
      e.printStackTrace();
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
   * In HW2, you should be using {@link DocumentIndexed}.
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

  private void WriteToIndexFile() throws IOException {
    System.out.println("Store index to: " + _indexFile);
    File file = new File(_indexFile);
    File parent = file.getParentFile();
    if(!parent.exists() && !parent.mkdirs()){
      throw new IllegalStateException("Couldn't create dir: " + parent);
    }
    ObjectOutputStream writer =
            new ObjectOutputStream(new FileOutputStream(_indexFile));
    writer.writeObject(this);
    writer.close();
  }
}
