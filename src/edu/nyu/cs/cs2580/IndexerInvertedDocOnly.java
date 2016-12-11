package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;

import java.io.*;
import java.util.*;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDocOnly extends Indexer implements Serializable {


  // We will be storing the inverted doc only representation in this data structure
  public Map<String, Vector<Integer>> _index = new HashMap<>();

  // This is where we will store the index file
  private final String _indexFile = _options._indexPrefix + "//invertedDocOnlyIndex.idx";
  private final String _corpusStatics = _options._indexPrefix + "/corpusStatistics.idx";
  private final String _documentStatistics = _options._indexPrefix + "/documentStatistics.idx";

  //The wiki corpus directory from where we will load files for constructing index
  private final String _wikiCorpusDir = _options._corpusPrefix;

  //TODO: don't need to have this at all
  private Map<String, Vector<Integer>> distributedIndex = new HashMap<>();

  //We will also store the Documents in the DocumentIndexed vector for the rankers
  private Vector<DocumentIndexed> _indexedDocs = new Vector<>();

  //Corpus statistics
  private Vector<Integer> totalTokensPerDoc = new Vector<>();
  int totalTokensInCorpus = 0;
  int numberOfDocs = 0;

  public IndexerInvertedDocOnly(Options options) {
    super(options);
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
    Set<String> tokens;
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
            htmlParser = new HtmlParser(wikiFile);
            tokens = htmlParser.ParseTokensNoDuplicates();

            // Populating and adding DocumentIndexed for this document.
            DocumentIndexed docIndexed = new DocumentIndexed(docID);
            docIndexed.setTitle(htmlParser.getTitle());
            docIndexed.setUrl(htmlParser.getUrl());
            _indexedDocs.add(docIndexed);

            // Updating postings lists
            for (String token : tokens) {
              if (!_index.containsKey(token)) {
                _index.put(token, new Vector<Integer>());
                _index.get(token).add(docID);
              } else {
                _index.get(token).add(docID);
              }
            }

            docID++;
            count++;
            totalTokensPerDoc.add(tokens.size());
            totalTokensInCorpus += tokens.size();
          }
        } catch (IllegalArgumentException e) {
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

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void loadIndex(Query query) throws IOException {

    Set<String> wordsSet = new HashSet<>();
    for (QueryToken queryToken : query._tokens) {
      if (queryToken.isPhrase()) {
        for (String querySubTokens : queryToken.getToken().split(" ")) {
          wordsSet.add(querySubTokens);
        }
      } else {
        wordsSet.add(queryToken.getToken());
      }
    }

    loadIndex(wordsSet);
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    return;
  }

  public void loadIndex(Set<String> queryTokens) throws IOException {

    _index.clear();
    distributedIndex.clear();

    for (String token : queryTokens) {
      // Open the file
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile + "_" + token.charAt(0))));

      while (dis.available() > 0) {
        String term = dis.readUTF();
        boolean isQueryToken = term.equals(token) ? true : false;
        int numberOfPostings = dis.readInt();
        for (int i = 0; i < numberOfPostings; i++) {
          int docID = dis.readInt();
          if (isQueryToken) {
            insertToken(docID, term);
          }
        }

        if (isQueryToken) {
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
  public int numDocs() {
    return numberOfDocs;
  }

  @Override
  public int totalTermFrequency() {
    return totalTokensInCorpus;
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

    while (docid < numberOfDocs) {

      Map<QueryToken, Integer> queryTokenCount = new HashMap<>();

      List<IndexerInvertedDocOnly.QueryTokenIndexData> indexData = new Vector<>();

      for (QueryToken token : query._tokens) {
        IndexerInvertedDocOnly.QueryTokenIndexData data;
        if (token.isPhrase()) {
          return null;
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
        } catch (Exception e) {
          e.printStackTrace();
        }
        for (int i = 0; i < indexData.size(); i++) {
          documentIndexedClone.quertTokenCount.put(indexData.get(i).queryToken, 1);
        }
        documentIndexedClone.totalNumberOfTokensInDoc = totalTokensPerDoc.get(documentIndexed._docid);
        return documentIndexedClone;
      }

      int maxDocId = -1;
      for (int i = 0; i < indexData.size(); i++) {
        if (indexData.get(i).docId > maxDocId)
          maxDocId = indexData.get(i).docId;
      }

      docid = maxDocId - 1;
    }
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
  public int getTokensPerDoc(int docId) {
    return 0;
  }

  @Override
  public int documentTermFrequency(String term, int docid) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }

  @Override
  public int getQueryTokenCountInCorpus(QueryToken token) {
    return 0;
  }

  @Override
  public Vector<TermProbability> getHighestTermProbabilitiesForDocs(Vector<Integer> docIds, int numTerms) {
    throw new UnsupportedOperationException("This indexer does not support Query similarity computation");
  }

  private void insertToken(int docID, String s) {

    if (!distributedIndex.containsKey(s)) {
      distributedIndex.put(s, new Vector<Integer>());
    }

    distributedIndex.get(s).add(docID);
  }

  private void WriteToIndexFile(Integer fileNumber) throws IOException {
    String indexFileName = _indexFile + fileNumber.toString();
    System.out.println("Creating index number " + fileNumber.toString() + "...");
    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFileName, false)));
    List<String> sortedKeys = new ArrayList(_index.keySet());
    Collections.sort(sortedKeys);

    for (String word : sortedKeys) {
      Vector<Integer> entry = _index.get(word);
      dataOut.writeUTF(word); // Write Term
      dataOut.writeInt(entry.size()); // Write number of postings for this term
      for (Integer docID : entry) {
        dataOut.writeInt(docID); //Write the docID
      }
    }

    dataOut.flush();
    dataOut.close();
  }

  private void writeCorpusStatistics() throws IOException {


    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_corpusStatics, false)));

    dataOut.writeInt(numberOfDocs);
    dataOut.writeInt(totalTokensInCorpus);
    dataOut.writeInt(totalTokensPerDoc.size());

    for (Integer entry : totalTokensPerDoc) {
      dataOut.writeInt(entry);
    }

    dataOut.flush();
    dataOut.close();

  }

  private void loadCorpusStatistics() throws IOException {
    // Open the file
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_corpusStatics)));

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

  private void loadDocumentData() throws IOException {
    // Open the file
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_documentStatistics)));

    for (int i = 0; i < numDocs(); i++) {
      DocumentIndexed doc = new DocumentIndexed(i);
      doc.setTitle(dis.readUTF());
      doc.setUrl(dis.readUTF());
      _indexedDocs.add(doc);
    }

    dis.close();
  }

  public class QueryTokenIndexData {
    QueryToken queryToken;
    Integer count;
    Integer docId;
  }

  private void writeDocumentData() throws IOException {
    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_documentStatistics, false)));

    for (DocumentIndexed doc : _indexedDocs) {
      dataOut.writeUTF(doc.getTitle());
      dataOut.writeUTF(doc.getUrl());
    }

    dataOut.flush();
    dataOut.close();
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

    for (int i = 0; i < fileNumber; i++) {
      disArr[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile + (i + 1))));
      String word = disArr[i].readUTF();
      postingListPQ.add(new PostingListPointer(word, i));
    }

    while (!postingListPQ.isEmpty()) {
      int[] pointersToMerge = new int[fileNumber];
      int i = 0;
      PostingListPointer postingListPointer = postingListPQ.poll();
      pointersToMerge[i] = postingListPointer._pointer;
      while (postingListPQ.peek() != null && postingListPQ.peek()._word.equals(postingListPointer._word)) {
        pointersToMerge[++i] = postingListPQ.poll()._pointer;
      }

      MergePostingsLists(pointersToMerge, i, disArr, postingListPointer._word);
      for (int j = 0; j <= i; j++) {
        if (disArr[pointersToMerge[j]].available() > 0) {
          postingListPQ.add(new PostingListPointer(disArr[pointersToMerge[j]].readUTF(), pointersToMerge[j]));
        }
      }
    }

    for (int i = 0; i < fileNumber; i++) {
      disArr[i].close();
    }
  }

  private void MergePostingsLists(int[] pointersToMerge, int endIndexPointersToMerge, DataInputStream[] disArr, String word) throws IOException {

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

    PriorityQueue<OccurenceListPointer> occurenceListPQ = new PriorityQueue<OccurenceListPointer> (50, new DocIDComparator());

    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_indexFile + "_" + word.charAt(0), true)));
    dataOutputStream.writeUTF(word);
    int[] numberOfOccurences = new int[disArr.length];
    int totalOccurences = 0;
    for (int i = 0; i <= endIndexPointersToMerge; i++) {
      numberOfOccurences[pointersToMerge[i]] = disArr[pointersToMerge[i]].readInt();
      totalOccurences += numberOfOccurences[pointersToMerge[i]];
      occurenceListPQ.add(new OccurenceListPointer(disArr[pointersToMerge[i]].readInt(), pointersToMerge[i]));
    }

    dataOutputStream.writeInt(totalOccurences);
    while (!occurenceListPQ.isEmpty()) {
      OccurenceListPointer occurenceListPointer = occurenceListPQ.poll();
      dataOutputStream.writeInt(occurenceListPointer._docID);
      numberOfOccurences[occurenceListPointer._pointer]--;
      if (numberOfOccurences[occurenceListPointer._pointer] != 0) {
        occurenceListPQ.add(new OccurenceListPointer(disArr[occurenceListPointer._pointer].readInt(), occurenceListPointer._pointer));
      }
    }

    dataOutputStream.flush();
    dataOutputStream.close();
  }

  private IndexerInvertedDocOnly.QueryTokenIndexData nextDocForWord(QueryToken word, int docId) {
    if (!distributedIndex.containsKey(word.getToken())) {
      return null;
    }

    Vector<Integer> wordMap = distributedIndex.get(word.getToken());
    IndexerInvertedDocOnly.QueryTokenIndexData data = new IndexerInvertedDocOnly.QueryTokenIndexData();

    if(docId >= wordMap.get(wordMap.size() - 1)) {
      return null;
    }

    int index = binarySearch(wordMap, docId);
    data.queryToken = word;
    data.docId = wordMap.get(index);
    return data;
  }

  //Returns index of key just greater than key;
  private int binarySearch(Vector<Integer> arr, int key) {
    int low = 0, high = arr.size();
    while (low != high) {
      int mid = (low + high) / 2;
      if (arr.get(mid) <= key) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }
    return low;
  }
}