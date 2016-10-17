package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments.RankerType;
/**
 * Created by sankethpurwar on 10/15/16.
 */
public class EvaluationGenerator {

    public static class RankerResult{
        //(rank,docid )
        public Map<Integer, Integer> ranking = new HashMap<Integer, Integer>();
    }

    public static class RankerResultsFile {
        private Map<String, RankerResult> rankerResults = new HashMap<String, RankerResult>();

        public void addResult(String query, Integer docId, Integer rank){
            RankerResult rankerResult = rankerResults.get(query);
            if(rankerResult == null){
                rankerResult = new RankerResult();
                rankerResults.put(query,rankerResult);
            }
            rankerResult.ranking.put(rank,docId);
        }
    }

    public static void evaluateAllRankers(String labelsFile){
        Map<String, Evaluator.DocumentRelevances> judgements =
                new HashMap<String, Evaluator.DocumentRelevances>();
        try {

            readRelevanceJudgments(labelsFile, judgements);
        }catch (IOException e){
            System.out.println("IOException while reading labels file");
        }
        File folder = new File("results");
        try {
            listFilesForFolder(folder,judgements);
        }catch(IOException e){
            System.out.println("IOException");
        }


    }

    public static void readRelevanceJudgments(
            String judgeFile, Map<String, Evaluator.DocumentRelevances> judgements)
            throws IOException {
        String line = null;
        BufferedReader reader = new BufferedReader(new FileReader(judgeFile));
        while ((line = reader.readLine()) != null) {
            // Line format: query \t docid \t grade
            Scanner s = new Scanner(line).useDelimiter("\t");
            String query = s.next();
            Evaluator.DocumentRelevances relevances = judgements.get(query);
            if (relevances == null) {
                relevances = new Evaluator.DocumentRelevances();
                judgements.put(query, relevances);
            }
            relevances.addDocument(Integer.parseInt(s.next()), s.next());
            s.close();
        }
        reader.close();
    }


    public static void listFilesForFolder(final File folder, Map<String, Evaluator.DocumentRelevances> judgements) throws IOException{
        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory() && fileEntry.getName().charAt(4) != '3') {
                System.out.println(fileEntry.getName());
                evaluateFile(fileEntry.getName(), judgements);
            }
        }
    }

    public static void evaluateFile(String fileName, Map<String, Evaluator.DocumentRelevances> judgements) throws IOException{
            String result = "";
            RankerResultsFile rankerResultsFile = loadRankerResultsFile(fileName);
            Iterator it = rankerResultsFile.rankerResults.entrySet().iterator();
            while (it.hasNext()) {

                Map.Entry pair = (Map.Entry)it.next();
                String query = (String)pair.getKey();
                result += query + "\t";
                Evaluator.DocumentRelevances relevances = (Evaluator.DocumentRelevances)judgements.get(query);
                RankerResult rankerResult = (RankerResult)pair.getValue();
                result += Evaluator.precision(1,relevances, rankerResult) + "\t";
                result += Evaluator.precision(5,relevances, rankerResult) + "\t";
                result += Evaluator.precision(10,relevances,rankerResult) + "\t";
                result += Evaluator.recall(1,relevances,rankerResult) + "\t";
                result += Evaluator.recall(5,relevances,rankerResult) + "\t";
                result += Evaluator.recall(10,relevances,rankerResult) + "\t";
                result += Evaluator.fMeasure(1,relevances,rankerResult) + "\t";
                result += Evaluator.fMeasure(5,relevances,rankerResult) + "\t";
                result += Evaluator.fMeasure(10,relevances,rankerResult) + "\t";
                result += Evaluator.precisionAtRecall(relevances,rankerResult);
                result += Evaluator.averagePrecision(relevances,rankerResult) + "\t";
                result += Evaluator.NDCG(1,relevances,rankerResult) + "\t";
                result += Evaluator.NDCG(5,relevances,rankerResult) + "\t";
                result += Evaluator.NDCG(10,relevances,rankerResult) + "\t";
                result += Evaluator.reciprocalRank(relevances,rankerResult) + "\t";

                System.out.println(result);
                result +=  "\n";
            }

        StringBuilder path = new StringBuilder("results/"+fileName);
        path.setCharAt(12, '3');

        File file = new File(path.toString());
        File parent = file.getParentFile();
        if(!parent.exists() && !parent.mkdirs()){
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }

        try(PrintWriter out = new PrintWriter(new FileOutputStream(file, false))){
            out.println(result);
        }
        catch (FileNotFoundException e) {
            System.out.println( "Cannot write to results file");
        }


    }

    public static RankerResultsFile loadRankerResultsFile(String filename){
        RankerResultsFile rankerResultsFile = new RankerResultsFile();
        filename = "results/"+filename;
        BufferedReader reader = getBufferReader(filename);
        String line = "";
        Integer rank = 0;
        String currentQuery = null;

        try {
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                Scanner s = new Scanner(line).useDelimiter("\t");
                String query = s.next();
                Integer docId = Integer.parseInt(s.next());
                if (currentQuery == null || !currentQuery.equalsIgnoreCase(query)) {
                    rank = 1;
                    currentQuery = query;
                } else {
                    rank++;
                }
                rankerResultsFile.addResult(query, docId, rank);
            }
        }catch (IOException e){
            System.out.println("IO Exception while reading results file");

        }
        return rankerResultsFile;
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

}
