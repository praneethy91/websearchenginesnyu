package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends IndexerInvertedOccurrence {

  public IndexerInvertedCompressed(Options options) {
    super(options);
  }

  @Override
  public void constructIndex() throws IOException {
    super._indexFile = _options._indexPrefix + "/invertedCompressedIndex.idx";
    super.constructIndex();
  }

  @Override
  public void loadIndex(Query query) throws IOException {

    if(query == null)
      return;
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

    loadCompressedIndex(wordsSet);

  }


  public void loadCompressedIndex(Set<String> queryTokens) throws IOException{

    long startTime = System.currentTimeMillis();
    _index.clear();
    distributedIndex.clear();

    for(String token : queryTokens) {
      // Open the file
      DataInputStream disComp = new DataInputStream(
              new BufferedInputStream(
                      new FileInputStream(_options._indexPrefix + "/invertedCompressedIndex.idx" + "_" + token.charAt(0))));

      while (disComp.available() > 0) {

        String term = disComp.readUTF();
        boolean isQueryToken = term.equals(token) ? true : false;
        int numOfDocs = getNextInt(disComp);
        int sumTillPrevDocId = 0;

        for (int i = 0; i < numOfDocs; i++) {
          int docId = sumTillPrevDocId + getNextInt(disComp);
          sumTillPrevDocId = docId;

          int numOfOcc = getNextInt(disComp);
          int sumTillPrevOcc = 0;
          for (int j = 0; j < numOfOcc; j++) {
            int place = sumTillPrevOcc + getNextInt(disComp);
            sumTillPrevOcc = place;
            if(isQueryToken){
              insertToken(term, docId,place,false,token);
            }

          }

        }
        if(isQueryToken) {
          break;
        }
      }
      //Close the input stream
      disComp.close();
    }

    loadCorpusStatistics();
    loadDocumentData();
    System.out.println("Time taken for loading index is "+ String.valueOf((System.currentTimeMillis() - startTime)/1000));
  }

  @Override
  void MergePostingsLists(int[] pointersToMerge,int endIndexPointersToMerge, DataInputStream[] disArr, String word) throws IOException {
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

    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_options._indexPrefix + "/invertedCompressedIndex.idx" + "_" + word.charAt(0), true)));
    dataOutputStream.writeUTF(word);
    int[] numberOfOccurences = new int[disArr.length];
    int totalOccurences = 0;
    for(int i = 0; i <= endIndexPointersToMerge; i++) {
      numberOfOccurences[pointersToMerge[i]] = disArr[pointersToMerge[i]].readInt();
      totalOccurences += numberOfOccurences[pointersToMerge[i]];
      occurenceListPQ.add(new OccurenceListPointer(disArr[pointersToMerge[i]].readInt(), pointersToMerge[i]));
    }

    dataOutputStream.write(Get(totalOccurences));
    int prevDocID = 0;
    while(!occurenceListPQ.isEmpty()) {
      OccurenceListPointer occurenceListPointer = occurenceListPQ.poll();
      int currDocID = occurenceListPointer._docID;
      dataOutputStream.write(Get(currDocID - prevDocID));
      prevDocID = currDocID;
      DataInputStream dis = disArr[occurenceListPointer._pointer];
      int occurrences = dis.readInt();
      dataOutputStream.write(Get(occurrences));
      int prevPosition = 0;
      for(int i = 0; i < occurrences; i++) {
        int currPosition = dis.readInt();
        dataOutputStream.write(Get(currPosition - prevPosition));
        prevPosition = currPosition;
      }

      numberOfOccurences[occurenceListPointer._pointer]--;
      if(numberOfOccurences[occurenceListPointer._pointer] != 0) {
        occurenceListPQ.add(new OccurenceListPointer(disArr[occurenceListPointer._pointer].readInt(), occurenceListPointer._pointer));
      }
    }

    dataOutputStream.flush();
    dataOutputStream.close();
  }

  public static byte[] Get(int num) throws IOException {

    LinkedList<Byte> bytePos = new LinkedList<>();
    int x = ((1 << 7) | (num & ((1 << 7) - 1)));
    bytePos.addFirst((byte) x);
    num = num >> 7;

    while (num != 0) {
      bytePos.addFirst((byte) (num & ((1 << 7) - 1)));
      num = num >> 7;
    }

    byte[] arr = new byte[bytePos.size()];
    int i = 0;
    for (Byte by : bytePos) {
      arr[i++] = by;
    }
    return arr;

  }

  private static int getNextInt(DataInputStream dis) throws IOException {
    int partialResult = 0;
    int curr;
    while ((curr = dis.read()) != -1) {

      if ((curr >> 7 == 1)) {
        partialResult = (partialResult << 7 | ((1 << 7) ^ curr));


        return partialResult;
      } else {
        partialResult = (partialResult << 7) | curr;
      }
    }
    throw new IOException();
  }


  @Override
  public int numDocs() {
    return super.numDocs();
  }

  @Override
  public int totalTermFrequency() {
    return super.totalTermFrequency();

  }

  @Override
  public Document getDoc(int docid) {
    return super.getDoc(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   */

  @Override
  public DocumentIndexed nextDoc(Query query, int docid) {
    return super.nextDoc(query,docid);

  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    return super.corpusDocFrequencyByTerm(term);
  }

  @Override
  public int corpusTermFrequency(String term) {
    return super.corpusTermFrequency(term);
  }

  @Override
  public int getTokensPerDoc(int docId) {
    return super.getTokensPerDoc(docId);
  }

  /**
   * @CS2580: Implement this to work with your RankerFavorite.
   */
  @Override
  public int documentTermFrequency(String term, int docid) {
    return super.documentTermFrequency(term,docid);
  }

  @Override
  public int getQueryTokenCountInCorpus(QueryToken token) {
    return super.getQueryTokenCountInCorpus(token);
  }
}