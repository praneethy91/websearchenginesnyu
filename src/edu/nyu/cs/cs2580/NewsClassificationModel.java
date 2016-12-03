package edu.nyu.cs.cs2580;

import de.bwaldvogel.liblinear.*;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.sql.Array;
import java.util.*;

/**
 * Created by Praneeth on 11/30/2016.
 */
public class NewsClassificationModel {
  private static HashMap<String, Integer> termsToIntRepresentationMap = new HashMap<String, Integer>();
  private static HashMap<Integer, HashSet<String>> docToCategoryMap = new HashMap<Integer, HashSet<String>>();
  private static HashMap<String, Integer> termsToNumDocsMap = new HashMap<String, Integer>();
  private static int totalDocs = 0;
  private static int newsPredictionsFailed;
  private static int newsPredictionsPassed;
  private static int nonNewsPredictionsFailed;
  private static int nonNewsPredictionsPassed;
  private static double[] predictOutputArray;
  private static FeatureNode[][] trainingSet;
  private static int numFeatures;
  private static int numberOfTrainingExamples;
  private static final String modelDir = "data\\model";

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
        return name.contains("lyrl2004_tokens_test_pt0")
                || name.contains("lyrl2004_tokens_test_pt1")
                || name.contains("lyrl2004_tokens_test_pt2")
                || name.contains("train");
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

    //Start of Weka classification
    //Add words as feature attributes
    numFeatures = termsToIntRepresentationMap.size();
    numberOfTrainingExamples = totalDocs;

    predictOutputArray = new double[numberOfTrainingExamples];
    trainingSet = new FeatureNode[numberOfTrainingExamples][];

    //Filling the training set with instances now.
    foundFiles = corpusDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.contains("lyrl2004_tokens_test_pt0")
                || name.contains("lyrl2004_tokens_test_pt1")
                || name.contains("lyrl2004_tokens_test_pt2")
                || name.contains("train");
      }
    });

    int index = 0;
    for (File file: foundFiles) {
      index = PopulateTrainingSet(file, index, true, null);
    }

    Problem problem = new Problem();
    problem.l = trainingSet.length;
    problem.n = numFeatures;
    problem.x = trainingSet;
    problem.y = predictOutputArray;

    SolverType solver = SolverType.L2R_L2LOSS_SVC_DUAL; // -s 2
    double C = 32; // cost of constraints violation
    double eps = 0.01; // stopping criteria

    Parameter parameter = new Parameter(solver, C, eps);
    Model model = Linear.train(problem, parameter);
    File modelSaveFile = new File(modelDir + "\\EnvironmentNatureTravelTourism");
    modelSaveFile.getParentFile().mkdirs();
    modelSaveFile.createNewFile();
    model.save(modelSaveFile);
    model = model.load(modelSaveFile);

    //Evaluating the test data now
    foundFiles = corpusDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.contains("lyrl2004_tokens_test_pt3");
      }
    });

    boolean h = false;
    index = 0;
    for (File file: foundFiles) {
      if(!h) {
        index = PopulateTrainingSet(file, index, false, model);
      }
      h = true;
    }

    //Evaluation
    //Evaluation eval = new Evaluation(isTrainingSet);
    //eval.evaluateModel(cModel, isTestingSet);

    //Print evaluation summary
    //String strSummary = eval.toSummaryString();
    //System.out.println(strSummary);

    System.out.print(nonNewsPredictionsPassed + " ");
    System.out.print(nonNewsPredictionsFailed);
    System.out.println();
    System.out.print(newsPredictionsFailed + " ");
    System.out.print(newsPredictionsPassed);


    // double[][] cmMatrix = eval.confusionMatrix();
    /*for (int i = 0; i < cmMatrix.length; i++) {
      for (int j = 0; j < cmMatrix[i].length; j++) {
        System.out.print(cmMatrix[i][j] + " ");
      }
      System.out.println();
    }*/

    //TODO: (confusion matrix) double[][] cmMatrix = eTest.confusionMatrix();
  }

  private static int PopulateTrainingSet(File file, int index, boolean isTrain, Model model) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(file));
    String line;
    HashSet<String> categories = new HashSet<String>(Arrays.asList("GENV", "GTOUR", "GWEA"));
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
        List<FeatureNode> featureNodes = new LinkedList<FeatureNode>();

        //TODO: Get way to get classification for each specific label
        HashSet<String> intersection = new HashSet<>(categories);
        intersection.retainAll(docToCategoryMap.get(docID));
        double docCategoryFeature = intersection.size() > 0 ? 1 : 0;

        if(isTrain) {
          //Storing in label
          predictOutputArray[index] = docCategoryFeature;
        }

        int i = 0;
        while(i < wordsInDoc.length) {
          String word = wordsInDoc[i].trim();
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
          double featureWeight = (termFrequency/termsInDoc)*Math.log(totalDocs/wordInNumDocs)/5;

          //Storing in the index the label
          featureNodes.add(new FeatureNode(wordId + 1, featureWeight));
        }

        Collections.sort(featureNodes, new Comparator<FeatureNode>() {
          @Override
          public int compare(FeatureNode o1, FeatureNode o2) {
            return o1.getIndex() > o2.getIndex() ? 1 : -1;
          }
        });

        FeatureNode[] featuresArr = featureNodes.toArray(new FeatureNode[featureNodes.size()]);

        if(isTrain) {
          trainingSet[index] = featuresArr;
        }
        else {
          Feature[] toPredictInstance = featuresArr;
          double actualDocClass = docCategoryFeature;
          double predictedDocClass = Linear.predict(model, toPredictInstance);

          if (actualDocClass == 1.0 && predictedDocClass == 1.0) {
            newsPredictionsPassed++;
          } else if (actualDocClass == 1.0 && predictedDocClass == 0.0) {
            newsPredictionsFailed++;
          } else if (actualDocClass == 0.0 && predictedDocClass == 0.0) {
            nonNewsPredictionsPassed++;
          } else if (actualDocClass == 0.0 && predictedDocClass == 1.0) {
            nonNewsPredictionsFailed++;
          }
        }
        index++;

      }
    }
    return index;
  }
}