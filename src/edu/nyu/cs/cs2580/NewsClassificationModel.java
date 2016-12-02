package edu.nyu.cs.cs2580;

import sun.security.jgss.GSSHeader;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.core.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

/**
 * Created by Praneeth on 11/30/2016.
 */
public class NewsClassificationModel {
  private static HashMap<String, Integer> termsToIntRepresentationMap = new HashMap<String, Integer>();
  private static HashMap<Integer, HashSet<String>> docToCategoryMap = new HashMap<Integer, HashSet<String>>();
  private static HashMap<String, Integer> termsToNumDocsMap = new HashMap<String, Integer>();
  private static int totalDocs = 0;

  public static void main(String[] args) throws Exception {
    ComputeSVMForDocuments();
  }

  public static void ComputeSVMForDocuments() throws Exception {
    HashMap<String, Integer> categoryToInt = new HashMap<String, Integer>();
    categoryToInt.put("GHEA", 1);
    categoryToInt.put("GSCI", 2);
    categoryToInt.put("GSPO", 3);
    categoryToInt.put("GPOL", 4);
    categoryToInt.put("GVOTE", 4);
    categoryToInt.put("GCRIM", 5);
    categoryToInt.put("GDEF", 5);
    categoryToInt.put("GDIP", 5);
    categoryToInt.put("GDIS", 5);
    categoryToInt.put("GJOB", 5);
    categoryToInt.put("GMIL", 5);
    categoryToInt.put("GODD", 5);
    categoryToInt.put("GWELF", 5);
    categoryToInt.put("GENT", 6);
    categoryToInt.put("GFAS", 6);
    categoryToInt.put("GPRO", 7);
    categoryToInt.put("GOBIT", 7);
    categoryToInt.put("GREL", 8);
    categoryToInt.put("GVIO", 9);
    categoryToInt.put("GENV", 10);
    categoryToInt.put("GTOUR", 10);
    categoryToInt.put("GWEA", 10);

    String fileDirString =  "data\\reuters";
    File corpusDir = new File(fileDirString);
    File[] foundFiles = corpusDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("rcv1");
      }
    });

    for(File file: foundFiles) {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while((line = br.readLine()) != null) {
        if(line.trim().equals("")) {
          continue;
        }

        String[] split = line.split("\\s+");
        String category = split[0].trim();
        if(categoryToInt.containsKey(category)) {
          String docIDString = split[1].trim();
          int docID = Integer.parseInt(docIDString);
          if (!docToCategoryMap.containsKey(docID)) {
            HashSet<String> categories = new HashSet<String>();
            categories.add(category);
            docToCategoryMap.put(docID, categories);
          } else {
            docToCategoryMap.get(docID).add(category);
          }
        }
      }
    }

    corpusDir = new File(fileDirString);
    foundFiles = corpusDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.contains("train");
      }
    });

    for (File file: foundFiles) {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while((line = br.readLine()) != null) {
        if(line.trim().equals("")) {
          continue;
        }

        if(line.charAt(0) == '.' && line.charAt(1) == 'I') {
          Integer docID = Integer.parseInt(line.substring(3).trim());
          if(!docToCategoryMap.containsKey(docID))
          {
            continue;
          }

          totalDocs++;
          br.readLine(); // skip .W
          String[] wordsInDoc = br.readLine().split("\\s+");
          String prev = wordsInDoc[0].trim();
          for(String word : wordsInDoc) {
            word = word.trim();
            if(!termsToIntRepresentationMap.containsKey(word)) {
              termsToIntRepresentationMap.put(word, termsToIntRepresentationMap.size());
            }
            if(!prev.equals(word)) {
              if(!termsToNumDocsMap.containsKey(prev)) {
                termsToNumDocsMap.put(prev, 1);
              }
              else {
                termsToNumDocsMap.put(prev, termsToNumDocsMap.get(prev) + 1);
              }
            }
            prev = word;
          }
          termsToNumDocsMap.put(prev, 1);
        }
      }
    }

    ArrayList<Attribute> fvWekaAttributes = new ArrayList<Attribute>();

    //Start of Weka classification
    //Add words as feature attributes
    int numFeatures = termsToIntRepresentationMap.size();
    for(int i = 1; i <= numFeatures; i++) {
      fvWekaAttributes.add(new Attribute(String.valueOf("feature" + i)));
    }

    //Add the news categories as labels
    ArrayList<String> newsClassCategories = new ArrayList<String>();
    for(int i = 0; i <= 1; i++) {
      newsClassCategories.add(String.valueOf(i));
    }

    fvWekaAttributes.add(new Attribute("NewsClass", newsClassCategories));



    //Filling the training set with instances now.
    foundFiles = corpusDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.contains("train");
      }
    });

    Instances isTrainingSet = null;
    for (File file: foundFiles) {
      isTrainingSet = CreateInstances(fvWekaAttributes, numFeatures, file);
    }

    //Filling the testing set with instances now.
    foundFiles = corpusDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.contains("test");
      }
    });

    Instances isTestingSet = null;
    boolean h = false;
    for (File file: foundFiles) {
      if(!h) {
        isTestingSet = CreateInstances(fvWekaAttributes, numFeatures, file);
      }
      h = true;
    }

    // Training model naive bayes
    Classifier cModel = (Classifier)new NaiveBayes();
    cModel.buildClassifier(isTrainingSet);

    //Cross-validation evaluation
    Evaluation eval = new Evaluation(isTrainingSet);
    eval.evaluateModel(cModel, isTestingSet);

    //Print evaluation summary
    String strSummary = eval.toSummaryString();
    System.out.println(strSummary);

    //TODO: (confusion matrix) double[][] cmMatrix = eTest.confusionMatrix();
  }

  private static Instances CreateInstances(ArrayList<Attribute> fvWekaAttributes, int numFeatures, File file) throws IOException {

    // Create an empty training set
    Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, 10);
    // Set class index
    isTrainingSet.setClassIndex(fvWekaAttributes.size() - 1);

    BufferedReader br = new BufferedReader(new FileReader(file));
    String line;
    while((line = br.readLine()) != null) {
      if(line.trim().equals("")) {
        continue;
      }

      if(line.charAt(0) == '.' && line.charAt(1) == 'I') {
        Integer docID = Integer.parseInt(line.substring(3).trim());
        if (!docToCategoryMap.containsKey(docID)) {
          continue;
        }

        br.readLine(); // Skip .W line
        String[] wordsInDoc = br.readLine().split("\\s+");

        //
        Instance iExample = new SparseInstance(numFeatures + 1);

        //TODO: Get way to get classification for each specific label
        int docCategoryFeature = docToCategoryMap.get(docID).contains("GHEA") ? 1 : 0;

        //Storing in last index the class label
        iExample.setValue((Attribute) fvWekaAttributes.get(numFeatures), String.valueOf(docCategoryFeature));
        int i = 0;
        while(i < wordsInDoc.length) {
          String word = wordsInDoc[i];
          int j = i;
          double termFrequency = 1;
          while(j + 1 < wordsInDoc.length && wordsInDoc[j + 1].equals(word)) {
            termFrequency++;
            j++;
          }
          i = ++j;
          if(!termsToIntRepresentationMap.containsKey(word)) {
            continue;
          }

          Integer wordId = termsToIntRepresentationMap.get(word);
          double termsInDoc = wordsInDoc.length;
          double wordInNumDocs = termsToNumDocsMap.get(word);
          //TF-IDF term
          double featureWeight = (termFrequency/termsInDoc)*Math.log(totalDocs/wordInNumDocs);

          //Storing in the index the label
          iExample.setValue((Attribute) fvWekaAttributes.get(wordId), featureWeight);
        }
        isTrainingSet.add(iExample);
      }
    }
    return isTrainingSet;
  }
}