package edu.nyu.cs.cs2580;

import java.io.*;
import java.security.InvalidParameterException;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Praneeth on 11/13/2016.
 */
public class Bhattacharyya {
  public static void main(String[] args) throws FileNotFoundException, IOException {
    if(args == null || args.length != 2) {
      throw new InvalidParameterException("Pass in the input and output file paths to compute the coefficient");
    }
    String inputFilePath = args[0];
    String outputFilePath = args[1];
    Vector<HashMap<String, Double>> queryRepMaps = new Vector<HashMap<String, Double>>();
    Vector<String> queryRepTerms = new Vector<String>();

    InputStream is = new FileInputStream(inputFilePath);
    BufferedReader bufInput = new BufferedReader(new InputStreamReader(is));

    String line = null;
    while((line = bufInput.readLine()) != null && !line.isEmpty()) {
      String[] lineSplitArr;
      if((lineSplitArr = line.split(":")).length > 1) {
        String queryRepTerm = lineSplitArr[0];
        String[] repTermPair = lineSplitArr[1].split("\t");
        String repTerm = repTermPair[0];
        double repTermProb = Double.parseDouble(repTermPair[1]);

        queryRepTerms.add(queryRepTerm);
        HashMap<String, Double> map =  new HashMap<String, Double>();
        map.put(repTerm, repTermProb);
        queryRepMaps.add(map);
      }
      else {
        String[] repTermPair = lineSplitArr[0].split("\t");
        String repTerm = repTermPair[0];
        double repTermProb = Double.parseDouble(repTermPair[1]);
        queryRepMaps.get(queryRepMaps.size() - 1).put(repTerm, repTermProb);
      }
    }

    bufInput.close();

    FileWriter fw = new FileWriter(outputFilePath, false);
    BufferedWriter bufOutput = new BufferedWriter(fw);

    for(int i = 0; i < queryRepTerms.size(); i++) {
      for(int j = i + 1; j < queryRepTerms.size(); j++) {
        bufOutput.write(queryRepTerms.get(i));
        bufOutput.write("\t");
        bufOutput.write(queryRepTerms.get(j));
        bufOutput.write("\t");

        double coefficient = 0.0;
        HashMap<String, Double> query1Map = queryRepMaps.get(i);
        HashMap<String, Double> query2Map = queryRepMaps.get(j);
        for(Map.Entry<String, Double> query1Entry : query1Map.entrySet()) {
          if(query2Map.containsKey(query1Entry.getKey())) {
            coefficient += Math.sqrt(query2Map.get(query1Entry.getKey())*query1Entry.getValue());
          }
        }
        bufOutput.write(Double.toString(coefficient));
        bufOutput.write("\n");
      }
    }

    bufOutput.close();
  }
}
