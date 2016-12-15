package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import de.bwaldvogel.liblinear.Model;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable{

  // This is where we will store the index file
  protected String _indexFile = _options._indexPrefix + "/invertedOccurrenceIndex.idx";
  protected String _corpusStatics = _options._indexPrefix + "/corpusStatistics.idx";
  private final String _documentStatistics = _options._indexPrefix + "/documentStatistics.idx";

  //The wiki corpus directory from where we will load files for constructing index
  private final String _wikiCorpusDir = _options._corpusPrefix;
  protected Map<String, LinkedHashMap<Integer,DocumentWordOccurrence>> _index = new HashMap<>();

  protected Map<String,Map<String, LinkedHashMap<Integer,DocumentWordOccurrence>>> distributedIndex = new HashMap<>();

  private HashMap<String, Integer> _termsToIntRepresentationMap = new HashMap<String, Integer>();
  private HashMap<String, Integer> _termsToNumDocsMap = new HashMap<String, Integer>();
  private HashMap<String, String> _newsFileToURLMap;

  private ArrayList<Model> _modelList = new ArrayList<Model>();

  protected StopWords stopWords;

  //Corpus statistics
  private Vector<Integer> totalTokensPerDoc =new Vector<>();
  int totalTokensInCorpus = 0;
  int numberOfDocs = 0;

  //We will also store the Documents in the DocumentIndexed vector for the rankers
  private Vector<DocumentIndexed> _indexedDocs = new Vector<>();

  public IndexerInvertedOccurrence(Options options) {
    super(options);
    stopWords = new StopWords();
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    class FileComparator implements Comparator<File> {

      @Override
      public int compare(File f1, File f2) {
        return f1.getName().compareTo(f2.getName());
      }
    }

    long startTime = System.currentTimeMillis();

    File dir = new File(_wikiCorpusDir);
    File[] directoryListing = dir.listFiles();
    if(directoryListing == null) {
      System.out.println("Specify the corpus directory with wiki files/symbolic link in engine.conf.");
      return;
    }
    Arrays.sort(directoryListing, new FileComparator());

    HtmlParser htmlParser = null;
    int docID = 0;
    Vector<String> tokens;
    int count = 0;
    int fileNumber = 1;

    //Clearing all Index files
    File indexDir = new File(_options._indexPrefix);
    indexDir.mkdir();
    File[] foundFiles = indexDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("inverted");
      }
    });

    if(foundFiles != null) {
      for (File file : foundFiles) {
        file.delete();
      }
    }

    try {
      for (File wikiFile : directoryListing) {
        try {

          if(wikiFile.isDirectory() == false) {

            //Parsing and extracting token;
            htmlParser = new HtmlParser(wikiFile, true);
            tokens = htmlParser.ParseGeneralTokens();

            //TODO: Document Id
            // Populating and adding DocumentIndexed for this document.
            DocumentIndexed docIndexed = new DocumentIndexed(docID);
            docIndexed.setTitle(htmlParser.getTitle());
            docIndexed.setUrl(htmlParser.getUrl());
            _indexedDocs.add(docIndexed);


            // Updating postings lists
            for (int pos = 0; pos < tokens.size(); pos++) {
              String token = tokens.elementAt(pos);
              insertToken(token, docID, pos, true, _index);
            }

            //Adding later as well formed documents only we should consider
            docID++;
            count++;
            totalTokensPerDoc.add(tokens.size());
            totalTokensInCorpus += tokens.size();
          }
        }
        catch (IllegalArgumentException e) {
          // A random non-wiki file, just skip this document
        }

        if (count >= 200) {
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
      System.out.println("Deleting the old index...");
      indexDir = new File(_options._indexPrefix);
      foundFiles = indexDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.matches(".*x\\d+");
        }
      });

      if(foundFiles != null) {
        for (File file : foundFiles) {
          file.delete();
        }
      }

      long endTime = System.currentTimeMillis();
      System.out.println("Done Indexing. Seconds taken to run indexing is : " + (endTime - startTime) / 1000);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void LoadModelData() throws IOException, ClassNotFoundException {
    ObjectInputStream reader =
            new ObjectInputStream(new FileInputStream(NewsClassificationConstants.termToIntFile));
    _termsToIntRepresentationMap = (HashMap<String, Integer>) reader.readObject();
    reader =
            new ObjectInputStream(new FileInputStream(NewsClassificationConstants.termToNumDocsFile));
    _termsToNumDocsMap = (HashMap<String, Integer>) reader.readObject();
    for (String category : NewsClassificationConstants.newsCategories) {
      File modelLoadFile = new File(NewsClassificationConstants.modelDir + "/" + category);
      Model model = Model.load(modelLoadFile);
      _modelList.add(model);
    }
  }

  protected void LoadNewsFileToURLData() throws IOException, ClassNotFoundException {
    BufferedReader reader =
            new BufferedReader(new FileReader(NewsClassificationConstants.newsFileToURLFile));
    String line;
    while((line = reader.readLine()) != null && !line.isEmpty()) {
      String[] split = line.split("\t");
      _newsFileToURLMap.put(split[0], split[1]);
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

    class StringComparator implements Comparator<PostingListPointer> {

      @Override
      public int compare(PostingListPointer o1, PostingListPointer o2) {
        return o1._word.compareTo(o2._word);
      }
    }

    System.out.println("Merging the index files...");

    DataInputStream[] disArr = new DataInputStream[fileNumber];
    PriorityQueue<PostingListPointer> postingListPQ = new PriorityQueue<PostingListPointer>(50, new StringComparator());

    for(int i = 0; i < fileNumber; i++) {
      disArr[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile + (i + 1))));
      String word = disArr[i].readUTF();
      postingListPQ.add(new PostingListPointer(word, i));
    }

    while(!postingListPQ.isEmpty()) {
      int[] pointersToMerge = new int[fileNumber];
      int i = 0;
      PostingListPointer postingListPointer = postingListPQ.poll();
      pointersToMerge[i] = postingListPointer._pointer;
      while(postingListPQ.peek() != null && postingListPQ.peek()._word.equals(postingListPointer._word)) {
        pointersToMerge[++i] = postingListPQ.poll()._pointer;
      }

      MergePostingsLists(pointersToMerge, i, disArr, postingListPointer._word);
      for(int j = 0; j <= i; j++) {
        if(disArr[pointersToMerge[j]].available() > 0) {
          postingListPQ.add(new PostingListPointer(disArr[pointersToMerge[j]].readUTF(), pointersToMerge[j]));
        }
      }
    }

    for(int i = 0; i < fileNumber; i++) {
      disArr[i].close();
    }
  }

  void MergePostingsLists(int[] pointersToMerge, int endIndexPointersToMerge, DataInputStream[] disArr, String word) throws IOException {

    class OccurenceListPointer {
      public int _docID;
      public int _pointer;
      public OccurenceListPointer(int docID, int pointer) {
        this._docID = docID;
        this._pointer = pointer;
      }
    }

    class DocIDComparator implements Comparator<OccurenceListPointer> {
      @Override
      public int compare(OccurenceListPointer o1, OccurenceListPointer o2) {
        if (o1._docID > o2._docID) {
          return 1;
        } else if (o2._docID > o1._docID) {
          return -1;
        } else {
          return 0;
        }
      }
    }

    PriorityQueue<OccurenceListPointer> occurenceListPQ = new PriorityQueue<OccurenceListPointer>(50, new DocIDComparator());

    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_indexFile + "_" + word.charAt(0), true)));

    int[] numberOfOccurences = new int[disArr.length];
    int totalOccurences = 0;
    for(int i = 0; i <= endIndexPointersToMerge; i++) {
      numberOfOccurences[pointersToMerge[i]] = disArr[pointersToMerge[i]].readInt();
      totalOccurences += numberOfOccurences[pointersToMerge[i]];
      occurenceListPQ.add(new OccurenceListPointer(disArr[pointersToMerge[i]].readInt(), pointersToMerge[i]));
    }

    // This condition is for skipping stop words in corpus which appear in more than 50% of docs
    if(stopWords.contains(word)) {
      while (!occurenceListPQ.isEmpty()) {
        OccurenceListPointer occurenceListPointer = occurenceListPQ.poll();
        DataInputStream dis = disArr[occurenceListPointer._pointer];
        int occurrences = dis.readInt();
        for (int i = 0; i < occurrences; i++) {
          dis.readInt(); // position in the occurrence
        }

        numberOfOccurences[occurenceListPointer._pointer]--;
        if (numberOfOccurences[occurenceListPointer._pointer] != 0) {
          occurenceListPQ.add(new OccurenceListPointer(disArr[occurenceListPointer._pointer].readInt(), occurenceListPointer._pointer));
        }
      }
    }
    else {
      dataOutputStream.writeUTF(word);
      dataOutputStream.writeInt(totalOccurences);
      while (!occurenceListPQ.isEmpty()) {
        OccurenceListPointer occurenceListPointer = occurenceListPQ.poll();
        dataOutputStream.writeInt(occurenceListPointer._docID);
        DataInputStream dis = disArr[occurenceListPointer._pointer];
        int occurrences = dis.readInt();
        dataOutputStream.writeInt(occurrences);
        for (int i = 0; i < occurrences; i++) {
          int position = dis.readInt();
          dataOutputStream.writeInt(position);
        }

        numberOfOccurences[occurenceListPointer._pointer]--;
        if (numberOfOccurences[occurenceListPointer._pointer] != 0) {
          occurenceListPQ.add(new OccurenceListPointer(disArr[occurenceListPointer._pointer].readInt(), occurenceListPointer._pointer));
        }
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

  private void writeCorpusStatistics() throws IOException{


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

  protected void loadCorpusStatistics() throws IOException{
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

  protected void loadDocumentData() throws IOException{
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

  private void insertToken(String token, int docID, int position, boolean isAbsolutePosition, Map<String, LinkedHashMap<Integer,DocumentWordOccurrence>> index ) {

    if (!index.containsKey(token)) {
      index.put(token, new LinkedHashMap<Integer, DocumentWordOccurrence>());
      index.get(token).put(docID, new DocumentWordOccurrence(docID, position));
    } else {
      if(!index.get(token).containsKey(docID)){
        index.get(token).put(docID, new DocumentWordOccurrence(docID, position));
      }else {
        if(isAbsolutePosition)
          index.get(token).get(docID).occurrence.add(position);
        else
          index.get(token).get(docID).occurrence.add(position);
      }
    }

  }


  protected void insertToken(String token, int docID, int position, boolean isAbsolutePosition, String s) {

    if(!distributedIndex.containsKey(s)){
      distributedIndex.put(s, new HashMap<String, LinkedHashMap<Integer,DocumentWordOccurrence>>());
    }

    insertToken(token,docID, position,isAbsolutePosition, distributedIndex.get(s));
  }

  @Override
  public void loadIndex(Query query) throws IOException {

    Set<String> wordsSet = new HashSet<>();
    for(QueryToken queryToken : query._tokens){
      if(queryToken.isPhrase()){
        for(String querySubTokens : queryToken.getToken().split(" ")){
          wordsSet.add(querySubTokens);
        }
      }else {
        wordsSet.add(queryToken.getToken());
      }
    }

    loadIndex(wordsSet);
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    CorpusAnalyzer analyzer = CorpusAnalyzer.Factory.getCorpusAnalyzerByOption(
            _options);
    LogMiner miner = LogMiner.Factory.getLogMinerByOption(SearchEngine.OPTIONS);
    _pageRanks = (Vector<Double>) analyzer.load();
    _numViews = (Vector<Double>) miner.load();
    LoadModelData();
    LoadNewsFileToURLData();
  }

  public void loadIndex(Set<String> queryTokens) throws IOException{

    _index.clear();
    distributedIndex.clear();

    for(String token : queryTokens) {
      // Open the file
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile + "_" + token.charAt(0))));

      while (dis.available() > 0) {
        String term = dis.readUTF();
        boolean isQueryToken = term.equals(token) ? true : false;
        int numberOfPostings = dis.readInt();
        for (int i = 0; i < numberOfPostings; i++) {
          int docID = dis.readInt();
          int numberOfOccurencesInThisDoc = dis.readInt();
          for (int j = 0; j < numberOfOccurencesInThisDoc; j++) {
            int position = dis.readInt();
            if(isQueryToken) {
              insertToken(term, docID, position, false, token);
            }
          }
        }

        if(isQueryToken) {
          break;
        }
      }
      //Close the input stream
      dis.close();
    }

    loadCorpusStatistics();
    loadDocumentData();
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
        DocumentIndexed documentIndexedClone = null;
        try {
          documentIndexedClone = (DocumentIndexed) documentIndexed.clone();
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }
        for (int i = 0; i < indexData.size(); i++) {
          documentIndexedClone.quertTokenCount.put(indexData.get(i).queryToken, indexData.get(i).count);
        }
        documentIndexedClone.totalNumberOfTokensInDoc = totalTokensPerDoc.get(documentIndexed._docid);
        return documentIndexedClone;
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
    if(!distributedIndex.containsKey(word.getToken())) {
      return null;
    }
    else if(!distributedIndex.get(word.getToken()).containsKey(word.getToken())) {
      return null;
    }
    LinkedHashMap<Integer,DocumentWordOccurrence> wordMap = distributedIndex.get(word.getToken()).get(word.getToken());

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

    List<Integer> occurrence = distributedIndex.get(word).get(word).get(docId).occurrence;

    for(int i = 0 ; i < occurrence.size() ; i++){
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
  public int totalTermFrequency() {
    return totalTokensInCorpus;
  }


  @Override
  public int getQueryTokenCountInCorpus (QueryToken token){
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

  @Override
  public Vector<TermProbability> getHighestTermProbabilitiesForDocs(Vector<Integer> docIds, int numTerms) {
    throw new UnsupportedOperationException("This indexer does not support Query similarity computation");
  }

  @Override
  public Collection<String> getCategories(String file) throws IOException {
    NewsClassifier newsClassifier = new NewsClassifier(file, _termsToIntRepresentationMap, _termsToNumDocsMap, _modelList);
    return newsClassifier.Classify();
  }
  @Override
  public String getURL(String file) throws IOException {
    return _newsFileToURLMap.get(file);
  }


  private void WriteToIndexFile(Integer fileNumber) throws IOException {
    String indexFileName = _indexFile + fileNumber.toString();
    System.out.println("Creating index number " + fileNumber.toString() + "...");
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
  }
}