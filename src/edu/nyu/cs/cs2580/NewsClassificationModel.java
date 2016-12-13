package edu.nyu.cs.cs2580;

import de.bwaldvogel.liblinear.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NewsClassificationModel {
  private HashMap<String, Integer> termsToIntRepresentationMap = new HashMap<String, Integer>();
  private HashMap<String, Integer> termsToNumDocsMap = new HashMap<String, Integer>();

  private String _binaryClassifierCategory;

  private int totalDocs = 0;
  private int totalBinaryclassifierCategoryArticles = 0;
  private int falseNegatives = 0;
  private int truePositives = 0;
  private int falsePositives = 0;
  private int trueNegatives = 0;
  private final double trainingDocumentsRatio = 0.7;
  private final double C = 32; // cost of constraints violation, got by lots of experimentation
  private final double eps = 0.01; // stopping criteria, got by lots of experimentation
  private int numFeatures = 0;
  private int numberOfTrainingExamples = 0;

  private double[] predictOutputArray;
  private FeatureNode[][] trainingSet;

  private static HashSet<String> newsCategoriesSet = new HashSet<String>();

  public NewsClassificationModel() {
    for(String category: NewsClassificationConstants.newsCategories) {
      newsCategoriesSet.add(category);
    }
  }

  public static void main(String[] args) throws Exception {
    boolean h = false;
    for(String category: NewsClassificationConstants.newsCategories) {
      if(!h) {
        NewsClassificationModel newsClassificationModel = new NewsClassificationModel();
        newsClassificationModel.ComputeAndSaveMLModel(category);
        newsClassificationModel.TestMLModelAndPrintStatistics(category);
      }
      h = true;
    }
  }

  public void ComputeAndSaveMLModel(String binaryClassifierCategory) throws Exception {
    _binaryClassifierCategory = binaryClassifierCategory;

    for(String category : newsCategoriesSet) {
      for(String newsCompany: NewsClassificationConstants.newsCompanies){
        TrainingSetProcessor(category, newsCompany);
      }
    }
    termsToIntRepresentationMap.put(NewsClassificationConstants.totalDocsKey, totalDocs);

    //Start of building classification model
    //Add words as feature attributes with tf-idf feature values
    numFeatures = termsToIntRepresentationMap.size();
    numberOfTrainingExamples = totalDocs;

    predictOutputArray = new double[numberOfTrainingExamples];
    trainingSet = new FeatureNode[numberOfTrainingExamples][];

    int index = 0;
    for(String category : newsCategoriesSet) {
      for(String newsCompany: NewsClassificationConstants.newsCompanies){
        index = CreateModelData(category, newsCompany, binaryClassifierCategory, index);
      }
    }

    Problem problem = new Problem();
    problem.l = trainingSet.length;
    problem.n = numFeatures;
    problem.x = trainingSet;
    problem.y = predictOutputArray;

    SolverType solver = SolverType.L2R_L2LOSS_SVC_DUAL;

    Parameter parameter = new Parameter(solver, C, eps);
    double ratio1 = (totalBinaryclassifierCategoryArticles*1.0)/totalDocs;
    parameter.setWeights(new double[] {ratio1, 1 - ratio1},new int[] {0, 1});
    Model model = Linear.train(problem, parameter);
    File modelSaveFile = new File(NewsClassificationConstants.modelDir + "\\" + binaryClassifierCategory);
    modelSaveFile.getParentFile().mkdirs();
    modelSaveFile.createNewFile();
    model.save(modelSaveFile);

    ObjectOutputStream writer =
            new ObjectOutputStream(new FileOutputStream(NewsClassificationConstants.termToIntFile, false));
    writer.writeObject(this.termsToIntRepresentationMap);
    writer.close();

    writer =
            new ObjectOutputStream(new FileOutputStream(NewsClassificationConstants.termToNumDocsFile, false));
    writer.writeObject(this.termsToNumDocsMap);
    writer.close();
  }

  public void TestMLModelAndPrintStatistics(String binaryClassifierCategory) throws Exception {
    File modelLoadFile = new File(NewsClassificationConstants.modelDir + "/" + binaryClassifierCategory);
    Model model = Model.load(modelLoadFile);

    int index = 0;
    for(String category : NewsClassificationConstants.newsCategories) {
      for(String newsCompany : NewsClassificationConstants.newsCompanies) {
        index = TestingSetProcessor(category, newsCompany, binaryClassifierCategory, model, index);
      }
    }

    PrintConfusionMatrix();
    PrintAverageErrorRate(); //Also known as average accuracy
  }

  private int CreateModelData(String category, String newsCompany, String binaryClassifierCategory, int index) throws Exception {
    Path corpusPathFox = Paths.get(NewsClassificationConstants.filesToTrainModelDir, category, newsCompany);
    File[] foundFiles = corpusPathFox.toFile().listFiles();
    int begin = 0;
    int end = (int)Math.floor(foundFiles.length * trainingDocumentsRatio);
    SortFiles(foundFiles);
    for(int i = begin; i <= end; i++) {
      index = PopulateModelData(foundFiles[i], index, binaryClassifierCategory, true, null);
    }
    return index;
  }

  private void TrainingSetProcessor(String category, String newsCompany) throws IOException {
    Path corpusPathFox = Paths.get(NewsClassificationConstants.filesToTrainModelDir, category, newsCompany);
    File[] foundFiles = corpusPathFox.toFile().listFiles();
    int begin = 0;
    int end = (int)Math.floor(foundFiles.length * trainingDocumentsRatio);
    SortFiles(foundFiles);
    for(int i = begin; i <= end; i++) {
      ProcessFile(foundFiles[i]);
    }
  }

  private int TestingSetProcessor(String category, String newsCompany, String binaryClassifierCategory, Model model, int index) throws Exception {
    Path corpusPathFox = Paths.get(NewsClassificationConstants.filesToTrainModelDir, category, newsCompany);
    File[] foundFiles = corpusPathFox.toFile().listFiles();
    int begin = (int)Math.floor(foundFiles.length * trainingDocumentsRatio) + 1;
    int end = foundFiles.length - 1;
    SortFiles(foundFiles);
    for(int i = begin; i <= end; i++) {
      index = PopulateModelData(foundFiles[i], index, binaryClassifierCategory, false, model);
    }
    return index;
  }

  private void SortFiles(File[] foundFiles) {
    class FileComparator implements Comparator<File>{
      public int compare(File a, File b) {
        return a.getName().compareTo(b.getName());
      }
    }
    Arrays.sort(
            foundFiles,
            new FileComparator());
  }

  private void ProcessFile(File file) throws IOException {
    HtmlParser parser = new HtmlParser(file, true);
    Vector<String> wordsInDoc = parser.ParseGeneralTokens();
    if(wordsInDoc == null || wordsInDoc.size() == 0) {
      return;
    }
    String prev = wordsInDoc.get(0);
    boolean flag = true;
    for(String word : wordsInDoc) {
      if(!termsToIntRepresentationMap.containsKey(word)) {
        termsToIntRepresentationMap.put(word, termsToIntRepresentationMap.size());
      }
      if(!prev.equals(word)) {
        if(!termsToNumDocsMap.containsKey(prev)) {
          termsToNumDocsMap.put(prev, 1);
        }
        else if(flag) {
          termsToNumDocsMap.put(prev, termsToNumDocsMap.get(prev) + 1);
          flag = false;
        }
      }
      prev = word;
    }
    termsToNumDocsMap.put(prev, 1);
    totalDocs++;
    if(file.getParentFile().getParentFile().getName().equals(_binaryClassifierCategory)) {
      totalBinaryclassifierCategoryArticles++;
    }
  }

  private int PopulateModelData(File file, int index, String binaryClassifierCategory, boolean isTrain, Model model) throws Exception {
    HtmlParser parser = new HtmlParser(file, true);
    Vector<String> wordsInDoc = parser.ParseGeneralTokens();
    List<FeatureNode> featureNodes = new LinkedList<FeatureNode>();
    if(wordsInDoc == null || wordsInDoc.size() == 0) {
      return index;
    }

    String docClass = file.getParentFile().getParentFile().getName();
    double docCategoryFeature = docClass.equals(binaryClassifierCategory) ? 1 : 0;

    if(isTrain) {
      //Storing in label
      predictOutputArray[index] = docCategoryFeature;
    }

    HashMap<String, Integer> docWordFrequency = new HashMap<String, Integer>();
    for(String word : wordsInDoc) {
      if(!docWordFrequency.containsKey(word)) {
        docWordFrequency.put(word, 1);
      }
      else {
        docWordFrequency.put(word, docWordFrequency.get(word) + 1);
      }
    }

    for(String word: docWordFrequency.keySet()) {
      double termFrequency = docWordFrequency.get(word);
      if(!termsToIntRepresentationMap.containsKey(word)) {
        continue;
      }

      Integer wordId = termsToIntRepresentationMap.get(word);
      double termsInDoc = wordsInDoc.size();
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
        truePositives++;
      } else if (actualDocClass == 1.0 && predictedDocClass == 0.0) {
        falseNegatives++;
      } else if (actualDocClass == 0.0 && predictedDocClass == 0.0) {
        trueNegatives++;
      } else if (actualDocClass == 0.0 && predictedDocClass == 1.0) {
        falsePositives++;
      }
    }
    index++;
    return index;
  }

  private void PrintAverageErrorRate() {
    System.out.println();
    double averageAccuracy = 0.5*((double) truePositives /(truePositives + falseNegatives) +
            (double) trueNegatives /(trueNegatives + falsePositives));
    System.out.println(String.format("Average accuracy: %f", averageAccuracy));
    System.out.println();
  }

  private void PrintConfusionMatrix() {
    System.out.println();
    System.out.println("Confusion matrix below:");
    System.out.print(trueNegatives + " ");
    System.out.print(falsePositives);
    System.out.println();
    System.out.print(falseNegatives + " ");
    System.out.print(truePositives);
    System.out.println();
  }
}