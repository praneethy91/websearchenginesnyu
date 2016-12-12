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

    if (query == null)
      return;
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

    loadCompressedIndex(wordsSet);

  }


  public void loadCompressedIndex(Set<String> queryTokens) throws IOException {

    long startTime = System.currentTimeMillis();
    _index.clear();
    distributedIndex.clear();

    for (String token : queryTokens) {
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
            if (isQueryToken) {
              insertToken(term, docId, place, false, token);
            }

          }

        }
        if (isQueryToken) {
          break;
        }
      }
      //Close the input stream
      disComp.close();
    }

    loadCorpusStatistics();
    loadDocumentData();
    System.out.println("Time taken for loading index is " + String.valueOf((System.currentTimeMillis() - startTime) / 1000));
  }

  @Override
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
    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_options._indexPrefix + "/invertedCompressedIndex.idx" + "_" + word.charAt(0), true)));

    int[] numberOfOccurences = new int[disArr.length];
    int totalOccurences = 0;
    for (int i = 0; i <= endIndexPointersToMerge; i++) {
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
      int prevDocID = 0;
      dataOutputStream.writeUTF(word);
      dataOutputStream.write(Get(totalOccurences));
      while (!occurenceListPQ.isEmpty()) {
        OccurenceListPointer occurenceListPointer = occurenceListPQ.poll();
        int currDocID = occurenceListPointer._docID;
        dataOutputStream.write(Get(currDocID - prevDocID));
        prevDocID = currDocID;
        DataInputStream dis = disArr[occurenceListPointer._pointer];
        int occurrences = dis.readInt();
        dataOutputStream.write(Get(occurrences));
        int prevPosition = 0;
        for (int i = 0; i < occurrences; i++) {
          int currPosition = dis.readInt();
          dataOutputStream.write(Get(currPosition - prevPosition));
          prevPosition = currPosition;
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
    return super.nextDoc(query, docid);

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
    return super.documentTermFrequency(term, docid);
  }

  @Override
  public int getQueryTokenCountInCorpus(QueryToken token) {
    return super.getQueryTokenCountInCorpus(token);
  }

  @Override
  public Vector<TermProbability> getHighestTermProbabilitiesForDocs(Vector<Integer> sortedDocIds, int numTerms){

    Vector<TermProbability> result = new Vector<TermProbability>();

    class TermProbabilityComparator implements Comparator<TermProbability> {
      @Override
      public int compare(TermProbability p1, TermProbability p2) {
        if (p1.getProbability() > p2.getProbability()) {
          return 1;
        } else if (p2.getProbability() > p1.getProbability()) {
          return -1;
        } else {
          return 0;
        }
      }
    }

    PriorityQueue<TermProbability> termProbabilitiesPQ = new PriorityQueue<TermProbability>(5, new TermProbabilityComparator());

    try {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_corpusStatics)));
      int docIdsTotalTerms = 0;
      outerLoop:
      while (dis.available() > 0) {
        dis.readInt(); //number of docs
        dis.readInt(); //total tokens in corpus
        int size = dis.readInt();
        int sortedDocIdIndex = 0;
        for (int i = 0; i < size; i++) {
          if(sortedDocIdIndex >= sortedDocIds.size()) {
            break outerLoop;
          }
          int totalDocTerms = dis.readInt();
          if(i == sortedDocIds.get(sortedDocIdIndex)) {
            sortedDocIdIndex++;
            docIdsTotalTerms += totalDocTerms;
          }
        }
      }

      dis.close();

      File indexDir = new File(_options._indexPrefix);
      final String indexFilesPrefix = "invertedCompressedIndex.idx" + "_";
      File[] foundFiles = indexDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith(indexFilesPrefix);
        }
      });

      if(foundFiles == null) {
        return result;
      }

      for(File foundFile : foundFiles) {
        DataInputStream disComp = new DataInputStream(
                new BufferedInputStream(
                        new FileInputStream(foundFile)));
        while (disComp.available() > 0) {

          int sortedDocIdIndex = 0;
          int termOccurrencesInDocs = 0;
          String term = disComp.readUTF();

          int numOfDocs = getNextInt(disComp);
          int sumTillPrevDocId = 0;
          for (int i = 0; i < numOfDocs; i++) {
            int docId = sumTillPrevDocId + getNextInt(disComp);
            sumTillPrevDocId = docId;
            int numOfOcc = getNextInt(disComp);
            if(sortedDocIdIndex < sortedDocIds.size() &&
                    docId == sortedDocIds.get(sortedDocIdIndex)) {
              sortedDocIdIndex++;
              termOccurrencesInDocs += numOfOcc;
            }
            for (int j = 0; j < numOfOcc; j++) {
              getNextInt(disComp);
            }
          }
          if(termOccurrencesInDocs != 0) {
            double probability = ((double)termOccurrencesInDocs)/docIdsTotalTerms;
            termProbabilitiesPQ.add(new TermProbability(term, probability));
          }
          if(termProbabilitiesPQ.size() > numTerms) {
            termProbabilitiesPQ.poll();
          }
        }
        //Close the input stream
        disComp.close();
      }
    }
    catch (FileNotFoundException ex) {
      return result;
    }
    catch (IOException ex) {
      return result;
    }

    double sumProbabilities = 0.0;
    while(!termProbabilitiesPQ.isEmpty()) {
      TermProbability termProbability = termProbabilitiesPQ.poll();
      sumProbabilities+= termProbability.getProbability();
      result.add(termProbability);
    }

    Collections.reverse(result);

    //Normalizing the probabilities
    for(int i = 0; i < result.size(); i++) {
      TermProbability termProbability = result.get(i);
      termProbability.setProbability(termProbability.getProbability()/sumProbabilities);
    }

    return result;
  }
}