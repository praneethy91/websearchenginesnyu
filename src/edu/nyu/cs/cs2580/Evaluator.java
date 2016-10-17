package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.EvaluationGenerator.*;

import static edu.nyu.cs.cs2580.EvaluationGenerator.getBufferReader;

/**
 * Evaluator for HW1.
 *
 * @author fdiaz
 * @author congyu
 */
class Evaluator {


    public static class DocumentRelevances {
        private Map<Integer, Double> relevances = new HashMap<Integer, Double>();
        private Map<Integer, Double> gains = new HashMap<Integer, Double>();

        public DocumentRelevances() {
        }

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

        public boolean hasGainForDoc(int docid) {
            return gains.containsKey(docid);
        }

        public double getGainForDoc(int docid) {
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


        private static double convertToGainRelevance(String grade) {
            return LabelGainEnum.valueOf(grade.toUpperCase()).gain;
        }
    }

    /**
     * Usage: java -cp src edu.nyu.cs.cs2580.Evaluator [labels] [metric_id]
     */
    public static void main(String[] args)  {

        SearchEngine.Check(args.length == 2, "Must provide labels and metric_id!");

        EvaluationGenerator.evaluateAllRankers(args[0]);
        //evaluateStdin(Integer.parseInt(args[1]), judgments);

//    precision(3, judgements);
//    fMeasure(1,judgements);
//    averagePrecision(judgements);
//    reciprocalRank(judgements);
//    NDCG(3,judgements);
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

    public static float NDCG(int at, DocumentRelevances relevances, RankerResult result)  {

        float ideal = IDGC(at, relevances, result);
        if(ideal == 0)
            return 0;
        float NDCG = DGC(at, relevances, result) / ideal;
        return NDCG;
    }

    public static float DGC(int rankAt, DocumentRelevances relevances, RankerResult result) {

        float discountedGainTotal = 0;



        Iterator it = result.ranking.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            int docId = (Integer) pair.getValue();
            if ((Integer) pair.getKey() <= rankAt) {
                if (relevances.hasGainForDoc(docId))
                    discountedGainTotal += ((relevances.getGainForDoc(docId) * Math.log(2)) / Math.log((Integer) pair.getKey() + 1));
            } else
                break;
        }
        return discountedGainTotal;
    }

    public static float IDGC(int rankAt,DocumentRelevances relevances, RankerResult result)  {

        float discountedGainTotal = 0;

        Map<Integer, Double> idealGains = new HashMap<>();


        Iterator it = result.ranking.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            int docId = (Integer) pair.getValue();
            if ((Integer) pair.getKey() <= rankAt) {
                if (relevances.hasGainForDoc(docId))
                    idealGains.put(docId, relevances.getGainForDoc(docId));
                else
                    idealGains.put(docId, (double) LabelGainEnum.BAD.gain);

            } else
                break;
        }

        idealGains = EvaluatorUtils.sortByValue(idealGains);
        it = idealGains.entrySet().iterator();
        int rank = 0;
        while (it.hasNext()) {
            rank++;
            Map.Entry pair = (Map.Entry) it.next();

            discountedGainTotal += (((Double) pair.getValue() * Math.log(2)) / Math.log(rank + 1));

        }
        return discountedGainTotal;

    }


    public static float reciprocalRank(DocumentRelevances relevances, RankerResult result) throws IOException {


        Iterator it = result.ranking.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            int docId = (Integer) pair.getValue();
            if (relevances.hasRelevanceForDoc(docId) && relevances.getRelevanceForDoc(docId) == 1.0) {
                return (float) 1 / (Integer) pair.getKey();
            }
        }


        return 0;
    }

    public static float averagePrecision(DocumentRelevances relevances, RankerResult result) throws IOException {


        float recall = 0;
        int avgCount = 0;
        float sum = 0;


        Iterator it = result.ranking.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            float currentRecall = recall((Integer) pair.getKey(), relevances, result);
            if (currentRecall > recall) {
                recall = currentRecall;
                sum += precision((Integer) pair.getKey(), relevances, result);
                avgCount++;
            }
        }


        float avgPrecision = sum / avgCount;

        return avgPrecision;


    }

    public static float fMeasure(int rankAt, DocumentRelevances relevances, RankerResult result) throws IOException {
        float precision = precision(rankAt, relevances, result);
        float recall = recall(rankAt, relevances, result);
        if(precision == 0 && recall == 0){
            return 0;
        }
        float fMeasure = (2 * precision * recall) / (precision + recall);

        return fMeasure;
    }

    public static String precisionAtRecall(DocumentRelevances relevances, RankerResult result) {
        // set of (recall,precision)
        Map<Double, Double>  PRSet = new HashMap<Double, Double>();
        PRSet.put(0.0,0.0);
        Iterator it = result.ranking.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry) it.next();
            Integer rank = (Integer) pair.getKey();
            Integer docId = (Integer) pair.getValue();

            Double precision = (double) precision(rank,relevances,result);
            Double recall = (double) recall(rank,relevances,result);

            if(PRSet.get(recall) != null && PRSet.get(recall) > precision ){

            }else {
                PRSet.put(recall,precision);
            }
        }

        Double currentRecall = 0.1;
        int currentIndex = 1;

        List recalls = new ArrayList(PRSet.keySet());

        String values = "0.0\t";
        while(currentRecall <= 1.0 && currentIndex < recalls.size()){

            if(currentRecall < (Double)recalls.get(currentIndex) && currentRecall > (Double)recalls.get(currentIndex - 1) ){
                Double x1 = (Double)recalls.get(currentIndex -1);
                Double x2 = (Double)recalls.get(currentIndex );
                Double y1 = PRSet.get(x1);
                Double y2 = PRSet.get(x2);

                Double y = y1 + ((y2-y1)/(x2-x1))*(currentRecall-x1);
                System.out.println("recall " + currentRecall +" Precision "+ y);
                values += y+"\t";
                currentRecall += 0.1;
            }else {
                currentIndex++;
            }
        }

        return values;
    }

    public static float precision(int rankAt, DocumentRelevances relevances, RankerResult result)  {





        int relevantDocuments = 0;

        Iterator it = result.ranking.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if ((Integer) pair.getKey() <= rankAt) {
                int docId = (Integer) pair.getValue();
                if (relevances.hasRelevanceForDoc(docId) && relevances.getRelevanceForDoc(docId) == 1.0) {
                    relevantDocuments++;
                }
            } else
                break;
        }

        float precision = (float) relevantDocuments / rankAt;
        System.out.println("Precision " + precision);
        return precision;
    }

    public static float recall(int rankAt, DocumentRelevances relevances, RankerResult result)  {


//    String line = null;
//    int documentsConsidered = rankAt;



        int totalRelevantDocuments = getRelevantDocumentsCount(relevances);
        int relevantDocuments = 0;

        Iterator it = result.ranking.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if ((Integer) pair.getKey() <= rankAt) {
                int docId = (Integer) pair.getValue();
                if (relevances.hasRelevanceForDoc(docId) && relevances.getRelevanceForDoc(docId) == 1.0) {
                    relevantDocuments++;
                }
            } else
                break;
        }

        float recall = (float) relevantDocuments / totalRelevantDocuments;
        System.out.println("Recall " + recall);
        return recall;
    }


    public static int getRelevantDocumentsCount(DocumentRelevances documentRelevances) {
        int relevantDocuments = 0;
        Iterator it = documentRelevances.relevances.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if ((Double) pair.getValue() == 1.0)
                relevantDocuments++;
        }
        return relevantDocuments;
    }

}
