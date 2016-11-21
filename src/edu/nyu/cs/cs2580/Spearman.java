package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

/**
 * Created by sankethpurwar on 11/20/16.
 */
public class Spearman {


    HashMap<Integer,Double> pageRank = new HashMap<>();
    HashMap<Integer,Double> numViewIndex = new HashMap<>();

    public static void main(String[] args){

        if(args.length != 2){
            System.out.println("Incorrect arguments. Please enter file path to pagerank and numview index file");
        }

        Spearman spearman = new Spearman();

        try {
            spearman.load(args);
        } catch (IOException e) {
            e.printStackTrace();
        }

        double z ;

        LinkedHashMap pageRankMap  = (LinkedHashMap) sortByValue(spearman.pageRank);
        LinkedHashMap numViewMap  = (LinkedHashMap) sortByValue(spearman.numViewIndex);

        int size = pageRankMap.size() < numViewMap.size() ? pageRankMap.size() : numViewMap.size();
        z = (double)(size*(size+1))/(2*size);

        double numerator = 0.0;
        double denominatorPageRank = 0.0;
        double denominatorNumView  = 0.0;

        for(int i = 0 ; i < size ; i++){
            int numViewRank = new ArrayList<Integer>(numViewMap.keySet()).indexOf(i);
            int pageRankRank = new ArrayList<Integer>(pageRankMap.keySet()).indexOf(i);
            numViewRank++;
            pageRankRank++;
            numerator += ((numViewRank - z)*(pageRankRank - z));
            denominatorNumView +=  ((numViewRank - z)*(numViewRank - z));
            denominatorPageRank += ((pageRankRank - z)*(pageRankRank - z));
        }
        double denominator = Math.sqrt(denominatorNumView*denominatorPageRank);
        System.out.println("value:" + numerator/denominator);
    }


    public void load(String[] args) throws IOException {
        // Open the file
        FileInputStream fstream = new FileInputStream(args[0]);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;
        //Read File Line By Line
        while ((strLine = br.readLine()) != null)   {
            // Print the content on the console
            String[] lineArray  = strLine.split(":");
            int docIndex = java.lang.Integer.parseInt(lineArray[0]);
            while (docIndex > pageRank.size()){
                pageRank.put(pageRank.size(), 0.0);
            }
            if(docIndex > pageRank.size()){

            }
            pageRank.put(java.lang.Integer.parseInt(lineArray[0]), java.lang.Double.parseDouble(lineArray[1]));
        }

        //Close the input stream
        br.close();

        System.out.println("Loading using " + this.getClass().getName());
        Scanner sc = new Scanner(new File(args[1]));

        int docid = 0;
        while (sc.hasNext()){
            numViewIndex.put(docid,Double.parseDouble(sc.next()));
            docid++;

        }


    }

    public static Map<Integer, Double> sortByValue(Map<Integer, Double> map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                if( ((Comparable) ((Map.Entry) (o2)).getValue()) == (((Map.Entry) (o1)).getValue())){
                    return ((Comparable) ((Map.Entry) (o2)).getKey()).compareTo(((Map.Entry) (o1)).getKey());
                }
                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
