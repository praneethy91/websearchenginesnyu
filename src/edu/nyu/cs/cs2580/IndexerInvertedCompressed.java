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
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    super.constructIndex();
    constructCompressedIndex();
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
      DataInputStream disComp = new DataInputStream(new BufferedInputStream(new FileInputStream(_indexFile.substring(0,11) + "compressed_"+_indexFile.substring(11) + "_" + token.charAt(0))));

      while (disComp.available() > 0) {

        String term = disComp.readUTF();
        boolean isQueryToken = term.equals(token) ? true : false;
        int numOfDocs = getNextInt(disComp);
        for (int i = 0; i < numOfDocs; i++) {
          int docId = getNextInt(disComp);
          int numOfOcc = getNextInt(disComp);
          for (int j = 0; j < numOfOcc; j++) {
            int place = getNextInt(disComp);
            if(isQueryToken){
              insertToken(term, docId,place,false,token);
            }

          }

        }
//        if(isQueryToken) {
//          break;
//        }
      }
      //Close the input stream
      disComp.close();
    }

    loadCorpusStatistics();
    loadDocumentData();
    System.out.println("Time taken for loading index is "+ String.valueOf((System.currentTimeMillis() - startTime)/1000));
  }


  public void constructCompressedIndex() throws IOException {
  try {
    File dir = new File(_options._indexPrefix);
    File[] directoryListing = dir.listFiles();

    for (File file : directoryListing) {
      if (file.isFile()) {
        if (file.getName().contains("invertedOccurrenceIndex")) {
          // Open the file
          DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file.getAbsolutePath())));

          //create a new file for compression

          File compFile = new File(_options._indexPrefix + "/compressed_" + file.getName());

          if (compFile.exists())
            compFile.delete();

          compFile.createNewFile();


          DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(compFile, false)));


          while (dis.available() > 0) {
            String term = dis.readUTF();
            dataOut.writeUTF(term);

            int numberOfPostings = dis.readInt();

            dataOut.write(Get(numberOfPostings));

            for (int i = 0; i < numberOfPostings; i++) {
              int docID = dis.readInt();
              dataOut.write(Get(docID));


              int numberOfOccurrencesInThisDoc = dis.readInt();
              dataOut.write(Get(numberOfOccurrencesInThisDoc));

              for (int j = 0; j < numberOfOccurrencesInThisDoc; j++) {
                int position = dis.readInt();
                dataOut.write(Get(position));
              }
            }
          }
          //Close the input stream
          dis.close();
          dataOut.flush();
          dataOut.close();
          file.delete();
        }

      }

    }
  }catch (Exception e){
    e.printStackTrace();
  }

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


  public void constructDecompressedFiles() throws IOException {

    try {
      File dir = new File(_options._indexPrefix);
      File[] decompDirectory = dir.listFiles();

      for (File file : decompDirectory) {
        if (file.isFile()) {
          if (file.getName().contains("compressed_")) {

            DataInputStream disComp = new DataInputStream(new BufferedInputStream(new FileInputStream(file.getAbsolutePath())));

            File decompFile = new File(_options._indexPrefix + "/"+file.getName().substring(11));
            DataOutputStream decompDataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(decompFile, true)));


            if (decompFile.exists())
              decompFile.delete();

            decompFile.createNewFile();


            while (disComp.available() > 0) {

              String term = disComp.readUTF();
              decompDataOut.writeUTF(term);

              int numOfDocs = getNextInt(disComp);
              decompDataOut.writeInt(numOfDocs);

              for (int i = 0; i < numOfDocs; i++) {
                int docId = getNextInt(disComp);
                decompDataOut.writeInt(docId);

                int numOfOcc = getNextInt(disComp);
                decompDataOut.writeInt(numOfOcc);

                for (int j = 0; j < numOfOcc; j++) {

                  int place = getNextInt(disComp);
                  decompDataOut.writeInt(place);
                }

              }

            }
            //Close the input stream
            decompDataOut.flush();
            decompDataOut.close();
            disComp.close();
            //file.delete();
          }

        }

      }
    }catch (Exception e){
      e.printStackTrace();
    }

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
