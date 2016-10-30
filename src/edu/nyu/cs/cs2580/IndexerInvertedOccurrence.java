package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable{

  // This is where we will store the index file
  private final String _indexFile = _options._indexPrefix + "/invertedOccurrenceIndex.idx";
  private final String _corpusStatics = _options._indexPrefix + "/corpusStatistics.idx";
  private final String _documentStatistics = _options._indexPrefix + "/documentStatistics.idx";

  //The wiki corpus directory from where we will load files for constructing index
  private final String _wikiCorpusDir = _options._corpusPrefix;
  private Map<String, LinkedHashMap<Integer,DocumentWordOccurrence>> _index = new HashMap<>();

  private Vector<Integer> totalTokensPerDoc =new Vector<>();
  int totalTokensInCorpus = 0;
  int numberOfDocs = 0;

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

    //Clearing all Index files
    File indexDir = new File(_options._indexPrefix);
    File[] foundFiles = indexDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("invertedOccurrenceIndex");
      }
    });

    for (File file : foundFiles) {
      file.delete();
    }

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


          // Updating postings lists
          for (int pos = 0 ; pos < tokens.size() ; pos++) {
            String token = tokens.elementAt(pos);
            insertToken(token, docID, pos,true);
          }

          //Adding later as well formed documents only we should consider
          docID++;
          count++;
          totalTokensPerDoc.add(tokens.size());
          totalTokensInCorpus += tokens.size();
        }
        catch(IllegalArgumentException e) {
          // A random non-wiki file, just skip this document
        }

        if(count >= 200){
          WriteToIndexFile(fileNumber);
          count = 0;
          fileNumber++;
          _index.clear();
        }

      }

      numberOfDocs = docID;
      WriteToIndexFile(fileNumber);
      fileNumber++;
      writeCorpusStatistics();
      writeDocumentData();
      _index.clear();
      MergeFiles(fileNumber - 1);

      //Clearing the old Index files after merging
      indexDir = new File(_options._indexPrefix);
      foundFiles = indexDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.matches(".*x\\d+");
        }
      });

      for (File file : foundFiles) {
        file.delete();
      }

      long endTime = System.currentTimeMillis();
      System.out.println("Done Indexing. Seconds taken to run indexing is : " + (endTime - startTime)/1000);

    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  private void MergeFiles(int fileNumber) throws IOException {

    class PostingListPointer {
      public String _word;
      public int _pointer;
      public PostingListPointer(String word, int pointer) {
        this._word = word;
        this._pointer = pointer;
      }
    }

    System.out.println("Merging the index files");

    DataInputStream[] disArr = new DataInputStream[fileNumber];
    PriorityQueue<PostingListPointer> postingListPQ = new PriorityQueue<>(new Comparator<PostingListPointer>() {
      @Override
      public int compare(PostingListPointer o1, PostingListPointer o2) {
        return o1._word.compareTo(o2._word);
      }
    });

    for(int i = 0; i < fileNumber; i++) {
      disArr[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile + fileNumber)));
      postingListPQ.add(new PostingListPointer(disArr[i].readUTF(), i));
    }

    while(!postingListPQ.isEmpty()) {
      int[] pointersToMerge = new int[fileNumber];
      int i = 0;
      PostingListPointer postingListPointer = postingListPQ.poll();
      pointersToMerge[i] = postingListPointer._pointer;
      while(postingListPQ.peek() != null && postingListPQ.peek()._word.equals(postingListPointer._word)) {
        pointersToMerge[++i] = postingListPQ.poll()._pointer;
      }

      MergePostingsLists(pointersToMerge, disArr, postingListPointer._word);
      for(int j = 0; j < pointersToMerge.length; j++) {
        if(disArr[pointersToMerge[j]].available() > 0) {
          postingListPQ.add(new PostingListPointer(disArr[pointersToMerge[j]].readUTF(), pointersToMerge[j]));
        }
      }
    }

    for(int i = 0; i < fileNumber; i++) {
      disArr[i].close();
    }
  }

  private void MergePostingsLists(int[] pointersToMerge, DataInputStream[] disArr, String word) throws IOException {

    class OccurenceListPointer {
      public int _docID;
      public int _pointer;
      public OccurenceListPointer(int docID, int pointer) {
        this._docID = docID;
        this._pointer = pointer;
      }
    }

    PriorityQueue<OccurenceListPointer> occurenceListPQ = new PriorityQueue<>(new Comparator<OccurenceListPointer>() {
      @Override
      public int compare(OccurenceListPointer o1, OccurenceListPointer o2) {
        if(o1._docID > o2._docID) {
          return 1;
        }
        else if(o2._docID > o1._docID) {
          return -1;
        }
        else {
          return 0;
        }
      }
    });

    //TODO: delete all alphabet merged files before starting indexing

    System.out.println("Merging posting lists for character: " + word.charAt(0));
    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_indexFile + "_" + word.charAt(0), true)));
    dataOutputStream.writeUTF(word);
    int[] numberOfOccurences = new int[disArr.length];
    int totalOccurences = 0;
    for(int i = 0; i < pointersToMerge.length; i++) {
      numberOfOccurences[pointersToMerge[i]] = disArr[pointersToMerge[i]].readInt();
      totalOccurences += numberOfOccurences[pointersToMerge[i]];
      occurenceListPQ.add(new OccurenceListPointer(disArr[pointersToMerge[i]].readInt(), pointersToMerge[i]));
    }

    dataOutputStream.writeInt(totalOccurences);
    while(!occurenceListPQ.isEmpty()) {
      OccurenceListPointer occurenceListPointer = occurenceListPQ.poll();
      DataInputStream dis = disArr[occurenceListPointer._pointer];
      int occurrences = dis.readInt();
      dataOutputStream.writeInt(occurrences);
      for(int i = 0; i < occurrences; i++) {
        dataOutputStream.writeInt(dis.readInt());
      }

      numberOfOccurences[occurenceListPointer._pointer]--;
      if(numberOfOccurences[occurenceListPointer._pointer] != 0) {
        occurenceListPQ.add(new OccurenceListPointer(disArr[occurenceListPointer._pointer].readInt(), occurenceListPointer._pointer));
      }
    }

    dataOutputStream.flush();
    dataOutputStream.close();
  }

  private void writeDocumentData() throws IOException {
    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_documentStatistics, false)));

    for(DocumentIndexed doc : _indexedDocs){
      dataOut.writeUTF(doc.getTitle());
      dataOut.writeUTF(doc.getUrl());
    }

    dataOut.flush();
    dataOut.close();
  }

  private void writeCorpusStatistics() throws  FileNotFoundException, IOException{


    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_corpusStatics, false)));

    dataOut.writeInt(numberOfDocs);
    dataOut.writeInt(totalTokensInCorpus);
    dataOut.writeInt(totalTokensPerDoc.size());

    for(Integer entry : totalTokensPerDoc){
      dataOut.writeInt(entry);
    }

    dataOut.flush();
    dataOut.close();

  }

  private  void loadCorpusStatistics() throws  FileNotFoundException, IOException{
    // Open the file
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_corpusStatics )));

    while (dis.available() > 0) {
      numberOfDocs = dis.readInt();
      totalTokensInCorpus = dis.readInt();
      int size = dis.readInt();

      totalTokensPerDoc.clear();
      for (int i = 0; i < size; i++) {
        totalTokensPerDoc.add(dis.readInt());
      }
    }
    dis.close();

  }

  private  void loadDocumentData() throws IOException{
    // Open the file
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_documentStatistics)));

    for(int i = 0; i < numDocs(); i++) {
      DocumentIndexed doc = new DocumentIndexed(i);
      doc.setTitle(dis.readUTF());
      doc.setUrl(dis.readUTF());
      _indexedDocs.add(doc);
    }

    dis.close();
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

    long startTime = System.currentTimeMillis();

    _index.clear();
      // Open the file
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile + "1")));

    while (dis.available() > 0) {
      String term = dis.readUTF();
      int numberOfPostings = dis.readInt();
      for (int i = 0; i < numberOfPostings; i++) {
        int docID = dis.readInt();
        int numberOfOccurencesInThisDoc = dis.readInt();
        for (int j = 0; j < numberOfOccurencesInThisDoc; j++) {
          int position = dis.readInt();
          insertToken(term, docID, position, false);
        }
      }
    }
    //Close the input stream
    dis.close();
    loadCorpusStatistics();
    loadDocumentData();
    System.out.println("Time taken for loading index is "+ String.valueOf((System.currentTimeMillis() - startTime)/1000));
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


    while ( docid < numberOfDocs) {

      Map<QueryToken, Integer> queryTokenCount = new HashMap<>();

      List<QueryTokenIndexData> indexData = new Vector<>();

      for (QueryToken token : query._tokens) {
        QueryTokenIndexData data;
        int nextDocId = -1;
        if (token.isPhrase()) {
          data = nextDocForPhrase(token, docid);
        } else {
          data = nextDocForWord(token, docid);
        }

        if (data == null || data.docId == -1) {
          return null;
        } else {
          indexData.add(data);
        }
      }


      boolean flag = true;

      for (int i = 0; i < indexData.size() - 1; i++) {
        if (indexData.get(i).docId.intValue() != indexData.get(i + 1).docId.intValue()) {
          flag = false;
        }
      }

      if (flag) {
        DocumentIndexed documentIndexed = _indexedDocs.get(indexData.get(0).docId);
        for (int i = 0; i < indexData.size(); i++) {
          documentIndexed.quertTokenCount.put(indexData.get(i).queryToken, indexData.get(i).count);
        }
        documentIndexed.totalNumberOfTokensInDoc = totalTokensPerDoc.get(documentIndexed._docid);
        return documentIndexed;
      }

      int maxDocId = -1;
      for (int i = 0; i < indexData.size(); i++) {
        if (indexData.get(i).docId > maxDocId)
          maxDocId = indexData.get(i).docId;
      }

      docid = maxDocId -1;
    }
    return null;

  }

  private QueryTokenIndexData nextDocForWord(QueryToken word, int docId){

    if(!_index.containsKey(word.getToken()))
        return null;
    LinkedHashMap<Integer,DocumentWordOccurrence> wordMap = _index.get(word.getToken());

    Set<Integer> keys = wordMap.keySet();


    QueryTokenIndexData data = new QueryTokenIndexData();

    //Collections.binarySearch(keys, docId);

    for(Integer key: keys){
      if(key > docId){
        data.queryToken = word;
        data.count = wordMap.get(key).occurrence.size();
        data.docId = key;
        return data;
      }
    }

    return null;

//    LinkedHashMap<Integer,DocumentWordOccurrence> wordMap = _index.get(word.getToken());
//
//    List<Integer> keys = new ArrayList<>(wordMap.keySet());
//
//
//    QueryTokenIndexData data = new QueryTokenIndexData();
//    data.queryToken = word;
//
//    docId++;
//    docId = 70;
//    int nextDocIdIndex = Collections.binarySearch(keys, docId);
//
//    if(nextDocIdIndex > 0){
//        int nextDocId = keys.get(nextDocIdIndex);
//      data.count = wordMap.get(nextDocId).occurrence.size();
//      data.docId = nextDocId;
//    }else {
//      int nextDocId = keys.get(nextDocIdIndex) * -1;
//      nextDocId --;
//      if(nextDocId >= numberOfDocs )
//        return null;
//      data.count = wordMap.get(nextDocId).occurrence.size();
//      data.docId = nextDocId;
//    }
//
//
//    return data;
  }


  private QueryTokenIndexData nextDocForPhrase(QueryToken phrase, Integer docId){
    QueryTokenIndexData returnData = new QueryTokenIndexData();
    String[] tokens = phrase.getToken().split(" ");

    while(docId < numberOfDocs) {
      List<QueryTokenIndexData> indexData = new Vector<>();


      for (String token : tokens) {
        QueryTokenIndexData data = nextDocForWord(new QueryToken(false, token), docId);
        //return null if any one of the token in the phrase is not in the doc
        if (data == null || data.docId == -1)
          return null;
        indexData.add(data);
      }

      boolean flag = true;

      for (int i = 0; i < indexData.size() - 1; i++) {
        if (indexData.get(i).docId.intValue() !=  indexData.get(i + 1).docId.intValue()) {
          flag = false;
        }
      }

      if (flag) {
        int currentPosition = nextPhraseInSameDoc(phrase, indexData.get(0).docId, -1);

        if (currentPosition == -1) {
          docId = indexData.get(0).docId;
        } else {
          int count = 1;

          while (currentPosition > -1) {
            currentPosition = nextPhraseInSameDoc(phrase, indexData.get(0).docId, currentPosition);
            if (currentPosition > -1)
              count++;
          }

          returnData.docId = indexData.get(0).docId;
          returnData.queryToken = phrase;
          returnData.count = count;

          return returnData;
        }
      } else {

        int maxDocId = -1;
        for (int i = 0; i < indexData.size(); i++) {
          if (indexData.get(i).docId > maxDocId)
            maxDocId = indexData.get(i).docId;
        }

        docId = maxDocId - 1;
      }

    }
    return null;

  }

  //returns next pos of phrase in docId after pos
  private  Integer nextPhraseInSameDoc(QueryToken phrase, Integer docId, int pos){

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
      if(positions.get(i).intValue() + 1 != positions.get(i+1).intValue()){
        flag = false;
      }
    }

    if(flag)
      return positions.get(0);

    return nextPhraseInSameDoc(phrase,docId,Collections.max(positions) - positions.size() + 1);


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

  @Override
  public int getTokensPerDoc(int docId){
    return totalTokensPerDoc.get(docId) ;
  }



  @Override
  public  int numDocs() { return numberOfDocs; }


  @Override
  public final int totalTermFrequency() {
    return totalTokensInCorpus;
  }


  @Override
  public int getQueryTokenCountInCorpus(QueryToken token){
      int docId = -1;
      int count = 0;

      Query newQuery = new QueryPhrase("");
      newQuery._tokens.clear();
      newQuery._tokens.add(token);

      DocumentIndexed documentIndexed = this.nextDoc(newQuery, docId);
      count = documentIndexed.quertTokenCount.get(token);

      while(documentIndexed != null){
        documentIndexed = this.nextDoc(newQuery, documentIndexed._docid);
        if(documentIndexed != null )
          count += documentIndexed.quertTokenCount.get(token);
      }

      return count;
  }

  private void WriteToIndexFile(Integer fileNumber) throws IOException {
    String indexFileName = _indexFile + fileNumber.toString();
    System.out.println("Creating index number " + fileNumber.toString());
    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFileName, false)));
    List<String> sortedKeys=new ArrayList(_index.keySet());
    Collections.sort(sortedKeys);

    for(String word : sortedKeys){
      Map<Integer, DocumentWordOccurrence> entry = _index.get(word);

      dataOut.writeUTF(word); // Write Term
      dataOut.writeInt(entry.size()); // Write number of postings for this term
      for(Map.Entry<Integer,DocumentWordOccurrence> occurrenceEntry : entry.entrySet()){
        dataOut.writeInt(occurrenceEntry.getKey()); //Write the docID
        ArrayList<Integer> occurrenceEntries = occurrenceEntry.getValue().occurrence;
        dataOut.writeInt(occurrenceEntries.size()); //Write the number of occurrences of the term for this docID.
        for(Integer occurrence : occurrenceEntries){
          dataOut.writeInt(occurrence); // Write number of occurrences in this doc and their positions
        }
      }
    }

    dataOut.flush();
    dataOut.close();
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
