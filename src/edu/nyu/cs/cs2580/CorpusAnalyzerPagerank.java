package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

  class FileComparator implements Comparator<File> {

    @Override
    public int compare(File f1, File f2) {
      return f1.getName().compareTo(f2.getName());
    }
  }

  ArrayList<String> docNameList = new ArrayList<>();
  HashMap<String, Integer> docNameToDocId = new HashMap<>();

  final String docIDIndexFile = _options._indexPrefix + "/docIDIndex.idx";
  final String pageRankFile = _options._indexPrefix + "/pageRank.idx";

  //double[][] graph ;
  double lambda = 0.1;
  HashMap<Integer,HashMap<Integer,Double>> graph = new HashMap<>();

  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {

    System.out.println("Preparing " + this.getClass().getName());
    File folder = new File(_options._corpusPrefix);

    createDocIdIndex();
    loadDocIDIndex();
    File[] directoryListing = folder.listFiles();
    Arrays.sort(directoryListing, new FileComparator());

    for (final File fileEntry : directoryListing) {
      if (!fileEntry.isDirectory() &&!fileEntry.isHidden()) {
        try {
          new WikiParser(fileEntry);
        }
        catch (Exception e) {
          continue;
        }

        ArrayList<Integer> linkedNodes = new ArrayList<>();
        HeuristicLinkExtractor extractor =  new CorpusAnalyzerPagerank.HeuristicLinkExtractor(fileEntry);
        String docName = extractor.getNextInCorpusLinkTarget();


        while(docName != null){

          if(docNameToDocId.get(docName) != null)
            linkedNodes.add(docNameToDocId.get(docName));
            docName = extractor.getNextInCorpusLinkTarget();
        }
        double value = (double)1/linkedNodes.size();

        for(int i  = 0 ; i < linkedNodes.size() ; i++) {
          if(!graph.containsKey(linkedNodes.get(i))){
            graph.put(linkedNodes.get(i),new HashMap<>());
          }
          HashMap<Integer, Double> temp = graph.get(linkedNodes.get(i));
          temp.put(docNameToDocId.get(fileEntry.getName()), value);
        }
      }
    }

    return;
  }



  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */



  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    int totalNumberOfDocs = docNameList.size();

    for(Map.Entry<Integer, HashMap<Integer, Double>> incomingLink : graph.entrySet()){
      for(Map.Entry<Integer,Double> link : incomingLink.getValue().entrySet()){
          link.setValue(lambda*link.getValue() + (1-lambda)/totalNumberOfDocs);
      }
    }

    savePageRankToFile(graph);
    return;
  }

  private void savePageRankToFile(HashMap<Integer,HashMap<Integer,Double>> graph) {
    deleteFileIfExists(pageRankFile);
    File fout = new File(pageRankFile);
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(fout, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

    int totalNumberOfDocs = docNameList.size();

    for(Map.Entry<Integer, HashMap<Integer, Double>> incomingLink : graph.entrySet()){
      double pageRank = 0.0;
      for(Map.Entry<Integer,Double> link : incomingLink.getValue().entrySet()){
        pageRank += link.getValue();
      }
      pageRank += (1-lambda)*(docNameList.size() - incomingLink.getValue().size())/totalNumberOfDocs;
      try {
        bw.write(incomingLink.getKey()+":"+pageRank);
        bw.newLine();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  private void createDocIdIndex() {

    deleteFileIfExists(docIDIndexFile);
    File fout = new File(docIDIndexFile);
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(fout, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

    int docIDIndex = 0;
    File folder = new File(_options._corpusPrefix);

    File[] directoryListing = folder.listFiles();
    Arrays.sort(directoryListing, new FileComparator());

    for (final File fileEntry : directoryListing) {
      if (!fileEntry.isDirectory() && !fileEntry.isHidden()) {
        try {
          new WikiParser(fileEntry);
        }
        catch (Exception e) {
          continue;
        }

        try {
          bw.write(docIDIndex+":"+fileEntry.getName());
          bw.newLine();
          docIDIndex++;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    try {
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void deleteFileIfExists(String fileName){
    File f = new File(fileName);
    f.delete();
  }
  public void loadDocIDIndex() throws IOException {

    // Open the file
    FileInputStream fstream = new FileInputStream(docIDIndexFile);
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

    String strLine;
    docNameList.clear();
    docNameToDocId.clear();
  //Read File Line By Line
    while ((strLine = br.readLine()) != null)   {
      // Print the content on the console
      String[] lineArray  = strLine.split(":");
      docNameList.add(Integer.parseInt(lineArray[0]),lineArray[1]);
      docNameToDocId.put(lineArray[1],Integer.parseInt(lineArray[0]));
    }

  //Close the input stream
    br.close();

  }

  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    return null;
  }

  public void buildTransitionMatrix() {

  }
}
