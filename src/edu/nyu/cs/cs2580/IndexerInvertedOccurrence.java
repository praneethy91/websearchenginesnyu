package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable{

  // This is where we will store the index file
  private final String _indexFile = _options._indexPrefix + "/invertedIndex.idx";
  //The wiki corpus directory from where we will load files for constructing index
  private final String _wikiCorpusDir = _options._corpusPrefix;
  private Map<String, LinkedHashMap<Integer,DocumentWordOccurrence>> _index = new HashMap<>();
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
        for (int pos = 0 ; pos < tokens.size() ; pos++) {
          String token = tokens.elementAt(pos);

          insertToken(token, docID, pos,true);

        }

        docID++;
        count++;
        if(count >= 20000){
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

  private void insertToken(String token, int docID, int position, boolean isAbosolutePosition) {

    if (!_index.containsKey(token)) {
      _index.put(token, new LinkedHashMap<>());
      _index.get(token).put(docID, new DocumentWordOccurrence(docID, position));
    } else {
        if(!_index.get(token).containsKey(docID)){
             _index.get(token).put(docID, new DocumentWordOccurrence(docID, position));
        }else {
          if(isAbosolutePosition)
            _index.get(token).get(docID).occurrence.add(position);
          else
            _index.get(token).get(docID).occurrence.add(position);
        }
    }

  }

  @Override
  public void loadIndex() throws IOException {

    _index.clear();
      // Open the file
      FileInputStream fstream = new FileInputStream(_indexFile + "1");
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));


      String strLine;

    while ((strLine = br.readLine()) != null)   {
      String[] indexTokens = strLine.split(":");
      for(int i  = 1 ; i < indexTokens.length ; i++){
        String[] occurences = indexTokens[i].split(",");
        for(int j = 1; j < occurences.length ; j++)
        insertToken(indexTokens[0],Integer.parseInt( occurences[0]),Integer.parseInt(occurences[j]),false);
      }
    }

  //Close the input stream
    fstream.close();
    br.close();
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

    Map<QueryToken, Integer> queryTokenCount = new HashMap<>();

    List<QueryTokenIndexData> indexData  = new Vector<>();

    for(QueryToken token : query._tokens ){
      QueryTokenIndexData data;
        int nextDocId = -1 ;
        if(token.isPhrase()){
           data = nextDocForPhrase(token,docid);
        }else {
           data  = nextDocForWord(token,docid);
        }

      if(data.docId == -1){
        return null;
      }else {
        indexData.add(data);
      }
    }


    boolean flag = true ;

    for(int i = 0 ; i < indexData.size() - 1; i++){
      if(indexData.get(i).docId != indexData.get(i+1).docId){
        flag = false;
      }
    }

    if(flag){
      DocumentIndexed documentIndexed = new DocumentIndexed(indexData.get(0).docId);
      for(int i = 0; i < indexData.size() ; i++) {
        documentIndexed.quertTokenCount.put(indexData.get(i).queryToken,indexData.get(i).count);
      }
      return documentIndexed;
    }

    int maxDocId = -1;
    for(int i = 0 ; i < indexData.size() ; i++){
      if(indexData.get(i).docId > maxDocId)
        maxDocId = indexData.get(i).docId;
    }


    return nextDoc(query,maxDocId -1);

  }

  private QueryTokenIndexData nextDocForWord(QueryToken word, int docId){

    LinkedHashMap<Integer,DocumentWordOccurrence> wordMap = _index.get(word.getToken());

    Set<Integer> keys = wordMap.keySet();

    QueryTokenIndexData data = new QueryTokenIndexData();

    for(Integer key: keys){
      if(key > docId){
        data.queryToken = word;
        data.count = wordMap.get(key).occurrence.size();
        data.docId = key;
        return data;
      }
    }

    return null;
  }

  private QueryTokenIndexData nextDocForPhrase(QueryToken phrase, Integer docId){

    String[] tokens = phrase.getToken().split(" ");

    List<QueryTokenIndexData> indexData  = new Vector<>();


    for(String token : tokens){
      QueryTokenIndexData data   = nextDocForWord(new QueryToken(false,token),docId);
      //return null if any one of the token in the phrase is not in the doc
      if(data.docId == -1)
        return null;
      indexData.add(data);
    }

    boolean flag = true ;

    for(int i = 0 ; i < indexData.size() - 1; i++){
      if(indexData.get(i).docId != indexData.get(i+1).docId){
        flag = false;
      }
    }

    if(flag) {
      int currentPosition = nextPhraseInSameDoc(phrase,indexData.get(0).docId, -1);
      if( currentPosition == -1){
        return  nextDocForPhrase(phrase, indexData.get(0).docId);
      }
      int count = 1;

      while(currentPosition > -1 ){
        currentPosition = nextPhraseInSameDoc(phrase, indexData.get(0).docId, currentPosition);
        if(currentPosition > -1)
            count ++;
      }

      QueryTokenIndexData returnData = new QueryTokenIndexData();
      returnData.docId = indexData.get(0).docId;
      returnData.queryToken = phrase;
      returnData.count = count;

      return returnData;
    }

    int maxDocId = -1;
    for(int i = 0 ; i < indexData.size() ; i++){
      if(indexData.get(i).docId > maxDocId)
        maxDocId = indexData.get(i).docId;
    }


    return nextDocForPhrase(phrase,maxDocId -1);

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

    return nextPhraseInSameDoc(phrase,docId,Collections.max(positions) - positions.size());


  }

  private int nextWordPostionInSameDoc(String word, Integer docId, Integer pos){

    List<Integer> occurrence = _index.get(word).get(docId).occurrence;
    Integer currentPos = 0;

    for(int i = 0 ; i < occurrence.size() ; i++){
      //currentPos = currentPos + occurrence.get(i);
      if(occurrence.get(i) > pos){
        return occurrence.get(i);
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
      fw.close();;
      bw.close();
      out.close();

    } catch (IOException e) {
    }


  }

  public class QueryTokenIndexData{
    QueryToken queryToken;
    Integer count;
    Integer docId;
  }

  public class DocumentWordOccurrence  implements Serializable {
    Integer docId;
    ArrayList<Integer> occurrence;

    DocumentWordOccurrence(Integer docId, int pos){
      this.docId = docId;
      this.occurrence = new ArrayList<>();
      this.occurrence.add(pos);
    }

//    private void addAbsolutePosition(Integer pos){
//        Integer currentPostition = 0;
////      for(int i = 0 ; i < occurrence.size() ; i++){
////        currentPostition =currentPostition + occurrence.get(i);
////      }
//      this.occurrence.add(pos - currentPostition);
//    }
//
//    private void addRelativePosition(Integer pos){
//      this.occurrence.add(pos);
//    }
  }
}
