package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable{

  public class DocumentWordOccurrence  implements Serializable {
    Integer docId;
    ArrayList<Integer> occurrence;

    DocumentWordOccurrence(Integer docId, int pos){
      this.docId = docId;
      this.occurrence = new ArrayList<>();
      this.occurrence.add(pos);
    }

    private void addNextPosition(Integer pos){
      Integer currentPostition = 0;
      for(int i = 0 ; i < occurrence.size() ; i++){
        currentPostition =currentPostition + occurrence.get(i);
      }
      this.occurrence.add(pos - currentPostition);
    }
  }

  private Map<String, LinkedHashMap<Integer,DocumentWordOccurrence>> _index = new HashMap<>();

  // This is where we will store the index file
  private final String _indexFile = _options._indexPrefix + "/invertedIndex.idx";

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
    long startTime = System.currentTimeMillis();

    File dir = new File(_wikiCorpusDir);
    File[] directoryListing = dir.listFiles();
    WikiParser wikiParser = null;
    int docID = 0;
    Vector<String> tokens;
    int count = 0;
    int fileNumber = 1;
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
            _index.put(token, new LinkedHashMap<>());
            _index.get(token).put(docID, new DocumentWordOccurrence(docID,i));
          } else {
              if(!_index.get(token).containsKey(docID)){
                   _index.get(token).put(docID, new DocumentWordOccurrence(docID, i));
              }else {
                _index.get(token).get(docID).addNextPosition(i);
              }
          }

        }
        docID++;
        count++;
        if(count >= 2000){
          WriteToIndexFile(fileNumber);
          count = 0;
          fileNumber++;
          _index.clear();
        }

      }
      WriteToIndexFile(fileNumber);
      _index.clear();

      //Finally writes to Index file.
      long endTime = System.currentTimeMillis();
      System.out.println("Seconds taken to run indexing is : " + (endTime - startTime)/1000);

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
  public DocumentIndexed nextDoc(Query query, int docid) {


    List<Integer> docs  = new Vector<>();

    for(QueryToken token : query._tokens ){
        int nextDocId = -1;
        if(token.isPhrase()){
           nextDocId = nextDocForPhrase(token,docid);
        }else {
           nextDocId = nextDocForWord(token.getToken(),docid);
        }

      if(nextDocId == -1){
        return null;
      }else {
        docs.add(nextDocId);
      }
    }


    boolean flag = true ;

    for(int i = 0 ; i < docs.size() - 1; i++){
      if(docs.get(i) != docs.get(i+1)){
        flag = false;
      }
    }

    if(flag)
      return new DocumentIndexed(docs.get(0));

    return nextDoc(query,Collections.max(docs) -1);

  }

  private Integer nextDocForWord(String word, int docId){

    LinkedHashMap<Integer,DocumentWordOccurrence> wordMap = _index.get(word);

    Set<Integer> keys = wordMap.keySet();

    for(Integer key: keys){
      if(key > docId){
        return key;
      }
    }

    return -1;
  }

  private Integer nextDocForPhrase(QueryToken phrase, Integer docId){

    String[] tokens = phrase.getToken().split(" ");
    List<Integer> docs  = new Vector<>();

    for(String token : tokens){
      int nextDoc  = nextDocForWord(token,docId);
      //return -1 if any one of the token in the phrase is not in the doc
      if(nextDoc == -1)
        return -1;

      docs.add(nextDoc);
    }

    boolean flag = true ;

    for(int i = 0 ; i < docs.size() - 1; i++){
      if(docs.get(i) != docs.get(i+1)){
        flag = false;
      }
    }

    if(flag)
      return docs.get(0);

    return nextPhraseInSameDoc(phrase,docId,Collections.max(docs) -1);

  }

  //returns next pos of phrase in docId after pos
  private  Integer nextPhraseInSameDoc(QueryToken phrase, Integer docId, Integer pos){

    String[] tokens = phrase.getToken().split(" ");
    List<Integer> positions  = new Vector<>();

    for(String token : tokens){
      int nextPosition  = nextWordPostionInSameDoc(token,docId,pos);
      //return -1 if any one of the token in the phrase is not in the doc
      if(nextPosition == -1)
          return -1;

      positions.add(nextPosition);
    }
    boolean flag = true ;
    for(int i = 0 ; i < positions.size() - 1; i++){
      if(positions.get(i) + 1 != positions.get(i+1)){
        flag = false;
      }
    }

    if(flag)
      return positions.get(0);

    return nextPhraseInSameDoc(phrase,docId,Collections.max(positions) -1);


  }

  private int nextWordPostionInSameDoc(String word, Integer docId, Integer pos){

    List<Integer> occurrence = _index.get(word).get(docId).occurrence;
    Integer currentPos = 0;

    for(int i = 0 ; i < occurrence.size() ; i++){
      currentPos = currentPos + occurrence.get(i);
      if(currentPos > pos){
        return currentPos;
      }
    }

    return -1;
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

  private void WriteToIndexFile(Integer fileNumber) throws IOException {
    String indexFileName = _indexFile + fileNumber.toString();
    System.out.println("Printing index number " + fileNumber.toString());
    try(FileWriter fw = new FileWriter(indexFileName, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw))
    {
      List<String> sortedKeys=new ArrayList(_index.keySet());
      Collections.sort(sortedKeys);

      for(String word : sortedKeys){
         Map<Integer, DocumentWordOccurrence> entry = _index.get(word);

        String line = "";
        line = word;

        for(Map.Entry<Integer,DocumentWordOccurrence> occurrenceEntry : entry.entrySet()){
          line = line+":"+occurrenceEntry.getKey().toString()+",";
          for(Integer occurrence : occurrenceEntry.getValue().occurrence){
            line = line+occurrence.toString()+",";
          }
          line = line.substring(0, line.length() - 1);
        }

        out.append(line+"\n");

      }
      out.close();
      bw.close();

    } catch (IOException e) {
    }


  }
}
