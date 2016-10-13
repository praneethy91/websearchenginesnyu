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
  public static class DocumentRelevances {
    private Map<Integer, Double> relevances = new HashMap<Integer, Double>();
    
    public DocumentRelevances() { }
    
    public void addDocument(int docid, String grade) {
      relevances.put(docid, convertToBinaryRelevance(grade));
    }
    
    public boolean hasRelevanceForDoc(int docid) {
      return relevances.containsKey(docid);
    }
    
    public double getRelevanceForDoc(int docid) {
      return relevances.get(docid);
    }
    
    private static double convertToBinaryRelevance(String grade) {
      if (grade.equalsIgnoreCase("Perfect") ||
          grade.equalsIgnoreCase("Excellent") ||
          grade.equalsIgnoreCase("Good")) {
        return 1.0;
      }
      return 0.0;
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

    //precision(5, judgments);
    //fMeasure(1,judgements);
    averagePrecision(judgements);
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

  public static void averagePrecision( Map<String, DocumentRelevances> judgements  ) throws IOException{
    //TODO: remove  hardcoded file. How will prof enter the file name ?
    String resultsFile = "/Users/sankethpurwar/Desktop/Assignments/testoutput.txt";
    BufferedReader reader = null;
    String line = null;
    String currentQuery = "bing";
    int lineNumber = 0; //TO know current result number
    float recall = 0;
    int avgCount = 0;
    float sum = 0;
    //TODO: remove  hardcoded query
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

    //TODO: remove  hardcoded file. How will prof enter the file name ?
    String resultsFile = "/Users/sankethpurwar/Desktop/Assignments/testoutput.txt";
    BufferedReader reader = null;
    String line = null;
    String currentQuery = "bing";
    int documentsConsidered = at;

    //TODO: remove  hardcoded query
    DocumentRelevances relevances = judgements.get(currentQuery);

    try {
       reader =
              new BufferedReader(new InputStreamReader(new FileInputStream(resultsFile)));
    }catch (FileNotFoundException e){
      System.out.println("File not found");
    }

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

    //TODO: remove  hardcoded file. How will prof enter the file name ?
    String resultsFile = "/Users/sankethpurwar/Desktop/Assignments/testoutput.txt";
    BufferedReader reader = null;
    String line = null;
    String currentQuery = "bing";
    int documentsConsidered = at;

    //TODO: remove  hardcoded query
    DocumentRelevances relevances = judgements.get(currentQuery);

    try {
      reader =
              new BufferedReader(new InputStreamReader(new FileInputStream(resultsFile)));
    }catch (FileNotFoundException e){
      System.out.println("File not found");
    }

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
