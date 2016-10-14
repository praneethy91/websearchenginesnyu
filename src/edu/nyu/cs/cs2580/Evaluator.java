package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

/**
 * Evaluator for HW1.
 * 
 * @author fdiaz
 * @author congyu
 */
class Evaluator {

    //TODO: remove  hardcoded file. How will prof enter the file name ?
    static String resultsFile = "/Users/sankethpurwar/Desktop/Assignments/testoutput.txt";

    //TODO: remove  hardcoded query
    static String currentQuery = "bing";

  public static class DocumentRelevances {
    private Map<Integer, Double> relevances = new HashMap<Integer, Double>();
    private Map<Integer, Double> gains = new HashMap<Integer, Double>();
    
    public DocumentRelevances() { }
    
    public void addDocument(int docid, String grade) {
      relevances.put(docid, convertToBinaryRelevance(grade));
      gains.put(docid, convertToGainRelevance(grade));
    }
    
    public boolean hasRelevanceForDoc(int docid) {
      return relevances.containsKey(docid);
    }
    
    public double getRelevanceForDoc(int docid) {
      return relevances.get(docid);
    }

    public boolean hasGainForDoc(int docid){
      return gains.containsKey(docid);
    }

    public double getGainForDoc(int docid){
      return gains.get(docid);
    }
    
    private static double convertToBinaryRelevance(String grade) {
      if (grade.equalsIgnoreCase("Perfect") ||
          grade.equalsIgnoreCase("Excellent") ||
          grade.equalsIgnoreCase("Good")) {
        return 1.0;
      }
      return 0.0;
    }


    private static double convertToGainRelevance(String grade){
     return LabelGainEnum.valueOf(grade.toUpperCase()).gain;
    }
  }
  
  /**
   * Usage: java -cp src edu.nyu.cs.cs2580.Evaluator [labels] [metric_id]
   */
  public static void main(String[] args) throws IOException {
    Map<String, DocumentRelevances> judgements =
        new HashMap<String, DocumentRelevances>();
    SearchEngine.Check(args.length == 2, "Must provide labels and metric_id!");
    readRelevanceJudgments(args[0], judgements);
    //evaluateStdin(Integer.parseInt(args[1]), judgments);

    precision(3, judgements);
    //fMeasure(1,judgements);
    //averagePrecision(judgements);
    //reciprocalRank(judgements);
    //NDCG(3,judgements);
  }

  public static void readRelevanceJudgments(
      String judgeFile, Map<String, DocumentRelevances> judgements)
      throws IOException {
    String line = null;
    BufferedReader reader = new BufferedReader(new FileReader(judgeFile));
    while ((line = reader.readLine()) != null) {
      // Line format: query \t docid \t grade
      Scanner s = new Scanner(line).useDelimiter("\t");
      String query = s.next();
      DocumentRelevances relevances = judgements.get(query);
      if (relevances == null) {
        relevances = new DocumentRelevances();
        judgements.put(query, relevances);
      }
      relevances.addDocument(Integer.parseInt(s.next()), s.next());
      s.close();
    }
    reader.close();
  }

  // @CS2580: implement various metrics inside this function
  public static void evaluateStdin(
      int metric, Map<String, DocumentRelevances> judgments)
          throws IOException {

    BufferedReader reader =
        new BufferedReader(new InputStreamReader(System.in));
    List<Integer> results = new ArrayList<Integer>();
    String currentQuery = "bing";
    String line = null;
    while ((line = reader.readLine()) != null) {
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if (!query.equals(currentQuery)) {
        if (results.size() > 0) {
          switch (metric) {
          case -1:
            evaluateQueryInstructor(currentQuery, results, judgments);
            break;
          case 0:
            break;
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
          case 6:
          default:
            // @CS2580: add your own metric evaluations above, using function
            // names like evaluateQueryMetric0.
            System.err.println("Requested metric not implemented!");
          }
          results.clear();
        }
        currentQuery = query;
      }
      results.add(Integer.parseInt(s.next()));
      s.close();
    }
    reader.close();
    if (results.size() > 0) {
      evaluateQueryInstructor(currentQuery, results, judgments);
    }
  }
  
  public static void evaluateQueryInstructor(
      String query, List<Integer> docids,
      Map<String, DocumentRelevances> judgments) {
    double R = 0.0;
    double N = 0.0;
    for (int docid : docids) {
      DocumentRelevances relevances = judgments.get(query);
      if (relevances == null) {
        System.out.println("Query [" + query + "] not found!");
      } else {
        if (relevances.hasRelevanceForDoc(docid)) {
          R += relevances.getRelevanceForDoc(docid);
        }
        ++N;
      }
    }
    System.out.println(query + "\t" + Double.toString(R / N));
  }

  public static void NDCG(int at, Map<String, DocumentRelevances> judgements ) throws IOException{

   float NDCG =  DGC(at,judgements) / IDGC(at,judgements);
    System.out.println(NDCG);
  }

  public static float DGC(int at, Map<String, DocumentRelevances> judgements ) throws IOException{

    float discountedGainTotal = 0;
    int resultNumber = 0;
    BufferedReader reader = getBufferReader(resultsFile);
    String line = null;


    DocumentRelevances relevances = judgements.get(currentQuery);

    while ((line = reader.readLine()) != null ) {
      resultNumber++;
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if(query.equals(currentQuery)){

        if (relevances == null) {
          System.out.println("Query [" + currentQuery + "] not found!");
        } else {
          int docId =  Integer.parseInt(s.next());
          if(relevances.hasGainForDoc(docId))
            discountedGainTotal += ((relevances.getGainForDoc(docId)*Math.log(2))/Math.log(resultNumber+1));
        }
      }
    }
    System.out.println("DCG "+ discountedGainTotal);

    return discountedGainTotal;

  }

  public static float IDGC(int at, Map<String, DocumentRelevances> judgements ) throws IOException{

    float discountedGainTotal = 0;
    int resultNumber = 0;


    BufferedReader reader = getBufferReader(resultsFile);
    String line = null;

    Map<Integer, Double> idealGains = new HashMap<>();

    DocumentRelevances relevances = judgements.get(currentQuery);



    while ((line = reader.readLine()) != null && at > 0) {
      at--;
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if(query.equals(currentQuery)){

        if (relevances == null) {
          System.out.println("Query [" + currentQuery + "] not found!");
        } else {
          int docId =  Integer.parseInt(s.next());
          if(relevances.hasGainForDoc(docId))
            idealGains.put(docId,relevances.getGainForDoc(docId));
        }
      }
    }

    idealGains = EvaluatorUtils.sortByValue(idealGains);

    Iterator it = idealGains.entrySet().iterator();

    while (it.hasNext()) {
      resultNumber++;
      Map.Entry pair = (Map.Entry)it.next();

      discountedGainTotal += (((Double) pair.getValue()*Math.log(2))/Math.log(resultNumber+1));

    }


    System.out.println("IDCG "+ discountedGainTotal);
    return discountedGainTotal;

  }



  public static void reciprocalRank(Map<String, DocumentRelevances> judgements ) throws IOException{

      BufferedReader reader = getBufferReader(resultsFile);
      String line = null;
      int resultNumber = 0;


    DocumentRelevances relevances = judgements.get(currentQuery);

    try {
      reader =
              new BufferedReader(new InputStreamReader(new FileInputStream(resultsFile)));
    }catch (FileNotFoundException e){
      System.out.println("File not found");
    }

    int totalRelevantDocuments = getRelevantDocumentsCount(judgements.get(currentQuery));
    int relevantDocuments = 0;

    while ((line = reader.readLine()) != null ) {
      resultNumber++;
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if(query.equals(currentQuery)){

        if (relevances == null) {
          System.out.println("Query [" + currentQuery + "] not found!");
        } else {
          int docId =  Integer.parseInt(s.next());
          if(relevances.hasRelevanceForDoc(docId) && relevances.getRelevanceForDoc(docId) == 1.0) {
            System.out.println((float) 1 / resultNumber);
            return;
          }
        }
      }
    }

    System.out.println(0);
  }

  public static void averagePrecision( Map<String, DocumentRelevances> judgements  ) throws IOException{


      BufferedReader reader = getBufferReader(resultsFile);
    String line = null;
    int lineNumber = 0; //TO know current result number
    float recall = 0;
    int avgCount = 0;
    float sum = 0;

    DocumentRelevances relevances = judgements.get(currentQuery);

    int totalRelevantDocuments = getRelevantDocumentsCount(judgements.get(currentQuery));
    int relevantDocuments = 0;

    while ((line = reader.readLine()) != null ) {
      lineNumber++;
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if(query.equals(currentQuery)){

        if (relevances == null) {
          System.out.println("Query [" + currentQuery + "] not found!");
        } else {
          int docId =  Integer.parseInt(s.next());
          float currentRecall = recall(lineNumber,judgements);
          if(currentRecall > recall){
            recall = currentRecall;
            sum += precision(lineNumber,judgements);
            avgCount++;
          }
        }
      }
    }

    float avgPrecision = sum/avgCount;
    System.out.println("Average Precision "+avgPrecision);


  }

  public static void fMeasure(int at, Map<String, DocumentRelevances> judgements  ) throws IOException{
    float precision = precision(at,judgements);
    float recall = recall(at, judgements);
    float fMeasure = (2*precision*recall)/(precision+recall);
    System.out.println("FMeasure "+ fMeasure);
  }

  public static float precision(int at,  Map<String, DocumentRelevances> judgements ) throws IOException{


    String line = null;

    int documentsConsidered = at;
      BufferedReader reader = getBufferReader(resultsFile);

    DocumentRelevances relevances = judgements.get(currentQuery);



    int relevantDocuments = 0;
    while ((line = reader.readLine()) != null && documentsConsidered > 0) {
      documentsConsidered--;
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if(query.equals(currentQuery)){

        if (relevances == null) {
          System.out.println("Query [" + currentQuery + "] not found!");
        } else {
          int docId =  Integer.parseInt(s.next());
          if (relevances.hasRelevanceForDoc(docId) && relevances.getRelevanceForDoc(docId) == 1.0 ) {
            relevantDocuments++;
          }
        }
      }
    }
    float precision = (float)relevantDocuments/at;
    System.out.println("Precision "+precision);
    return precision;
  }

  public static float recall(int at,  Map<String, DocumentRelevances> judgements ) throws IOException{


    String line = null;
    int documentsConsidered = at;

      BufferedReader reader = getBufferReader(resultsFile);
    DocumentRelevances relevances = judgements.get(currentQuery);

    int totalRelevantDocuments = getRelevantDocumentsCount(judgements.get(currentQuery));
    int relevantDocuments = 0;

    while ((line = reader.readLine()) != null && documentsConsidered > 0) {
      documentsConsidered--;
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if(query.equals(currentQuery)){

        if (relevances == null) {
          System.out.println("Query [" + currentQuery + "] not found!");
        } else {
          int docId =  Integer.parseInt(s.next());
          if (relevances.hasRelevanceForDoc(docId) && relevances.getRelevanceForDoc(docId) == 1.0 ) {
            relevantDocuments++;
          }
        }
      }
    }

    float recall = (float)relevantDocuments/totalRelevantDocuments;
    System.out.println("Recall " + recall);
    return recall;
  }

  public static BufferedReader getBufferReader(String resultsFile){
      BufferedReader reader = null;

      try {
          reader =
                  new BufferedReader(new InputStreamReader(new FileInputStream(resultsFile)));
      }catch (FileNotFoundException e){
          System.out.println("File not found");
      }

      return reader;
  }


  public static int getRelevantDocumentsCount( DocumentRelevances documentRelevances){
    int relevantDocuments = 0;
    Iterator it = documentRelevances.relevances.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();
      if((Double)pair.getValue() == 1.0)
        relevantDocuments++;
    }
    return relevantDocuments;
  }

}
