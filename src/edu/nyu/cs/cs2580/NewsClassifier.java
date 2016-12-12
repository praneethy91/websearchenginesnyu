package edu.nyu.cs.cs2580;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NewsClassifier {

  private File _articleToBeClassified ;
  private FeatureNode[] _toPredictInstance;
  private HashMap<String, Integer> _termsToIntRepresentationMap;
  private HashMap<String, Integer> _termsToNumDocsMap;
  private ArrayList<Model> _modelList;
  private int _totalDocs;

  public NewsClassifier(String filePath,
                        HashMap<String, Integer> termsToIntRepresentationMap,
                        HashMap<String, Integer> termsToNumDocsMap,
                        ArrayList<Model> modelList) {
    _articleToBeClassified = new File(filePath);
    _modelList = modelList;
    _termsToIntRepresentationMap = termsToIntRepresentationMap;
    _termsToNumDocsMap = termsToNumDocsMap;
    _totalDocs = _termsToIntRepresentationMap.get(NewsClassificationConstants.totalDocsKey);
  }

  public Collection<String> Classify() throws IOException {
    ArrayList<String> categories = new ArrayList<>();
    Iterator<Model> modelIterator = _modelList.iterator();
    if(convertFileIntoModelInstance()) {
      for (String category : NewsClassificationConstants.newsCategories) {
        if (belongsToCategory(modelIterator.next())) {
          categories.add(category);
        }
      }
    }

    if(categories.isEmpty()) {
      categories.add("uncategorized");
    }

    return categories;
  }

  private boolean belongsToCategory(Model model) throws IOException {
    double predictedDocClass = Linear.predict(model, _toPredictInstance);
    if(predictedDocClass == 1.0) {
      return true;
    }

    return false;
  }

  private boolean convertFileIntoModelInstance() throws IOException {
    HtmlParser parser = new HtmlParser(_articleToBeClassified, true);
    Vector<String> wordsInDoc = parser.ParseGeneralTokens();
    List<FeatureNode> featureNodes = new LinkedList<FeatureNode>();
    if(wordsInDoc == null || wordsInDoc.size() == 0) {
      return false;
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
      if(!_termsToIntRepresentationMap.containsKey(word)) {
        continue;
      }

      Integer wordId = _termsToIntRepresentationMap.get(word);
      double termsInDoc = wordsInDoc.size();
      double wordInNumDocs = _termsToNumDocsMap.get(word);
      //TF-IDF term
      double featureWeight = (termFrequency/termsInDoc)*Math.log(_totalDocs/wordInNumDocs)/5;

      //Storing in the index the label
      featureNodes.add(new FeatureNode(wordId + 1, featureWeight));
    }

    Collections.sort(featureNodes, new Comparator<FeatureNode>() {
      @Override
      public int compare(FeatureNode o1, FeatureNode o2) {
        return o1.getIndex() > o2.getIndex() ? 1 : -1;
      }
    });

    _toPredictInstance = featureNodes.toArray(new FeatureNode[featureNodes.size()]);
    return true;
  }
}
