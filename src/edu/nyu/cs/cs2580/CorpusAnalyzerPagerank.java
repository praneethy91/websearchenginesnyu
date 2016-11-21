package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.util.concurrent.Exchanger;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

    final String docIDIndexFile = _options._indexPrefix + "/docIDIndex.idx";
    final String pageRankFile = _options._indexPrefix + "/pageRank.idx";
    final String googleMatrix = _options._indexPrefix + "/googleMatrix.idx";
    ArrayList<String> docNameList = new ArrayList<>();
    HashMap<String, Integer> docNameToDocId = new HashMap<>();
    Vector<Double> pageRank = new Vector<>();
    Vector<Double> pageRankSquared = new Vector<>();

    double lambda = 0.9;
    boolean squareGoogleMatrix = true;
    HashMap<Integer, HashMap<Integer, Double>> graph = new HashMap<>();

    public CorpusAnalyzerPagerank(Options options) {
        super(options);
    }

    /**
     * This function processes the corpus as specified inside {@link _options}
     * and extracts the "internal" graph structure from the pages inside the
     * corpus. Internal means we only store links between two pages that are both
     * inside the corpus.
     * <p>
     * Note that you will not be implementing a real crawler. Instead, the corpus
     * you are processing can be simply read from the disk. All you need to do is
     * reading the files one by one, parsing them, extracting the links for them,
     * and computing the graph composed of all and only links that connect two
     * pages that are both in the corpus.
     * <p>
     * Note that you will need to design the data structure for storing the
     * resulting graph, which will be used by the {@link compute} function. Since
     * the graph may be large, it may be necessary to store partial graphs to
     * disk before producing the final graph.
     *
     * @throws IOException
     */

    @Override
    public void prepare() throws IOException {

        System.out.println("Preparing " + this.getClass().getName());
        File folder = new File(_options._corpusPrefix);

        createDocIdIndex();
        loadDocIDIndex();
        File[] directoryListing = folder.listFiles();
        Arrays.sort(directoryListing, new FileComparator());

        for (final File fileEntry : directoryListing) {
            if (!fileEntry.isDirectory() && !fileEntry.isHidden()) {
                try {
                    new WikiParser(fileEntry);
                } catch (Exception e) {
                    continue;
                }

                Set<Integer> linkedNodes = new HashSet<Integer>();
                HeuristicLinkExtractor extractor = new CorpusAnalyzerPagerank.HeuristicLinkExtractor(fileEntry);
                String docName = extractor.getNextInCorpusLinkTarget();

                while (docName != null) {
                    if (docNameToDocId.get(docName) != null)
                        linkedNodes.add(docNameToDocId.get(docName));
                    docName = extractor.getNextInCorpusLinkTarget();
                }
                double value = (double) 1 / linkedNodes.size();

                List<Integer> linkedNodesList = new ArrayList<Integer>(linkedNodes);

                for (int i = 0; i < linkedNodes.size(); i++) {
                    if (!graph.containsKey(linkedNodesList.get(i))) {
                        graph.put(linkedNodesList.get(i), new HashMap<Integer, Double>());
                    }
                    HashMap<Integer, Double> temp = graph.get(linkedNodesList.get(i));
                    temp.put(docNameToDocId.get(fileEntry.getName()), value);
                }
            }
        }

        for (int i = 0; i < docNameToDocId.size(); i++) {
            if (!graph.containsKey(i)) {
                graph.put(i, new HashMap<Integer, Double>());
            }
        }
        return;
    }

    /**
     * This function computes the PageRank based on the internal graph generated
     * by the {@link prepare} function, and stores the PageRank to be used for
     * ranking.
     * <p>
     * Note that you will have to store the computed PageRank with each document
     * the same way you do the indexing for HW2. I.e., the PageRank information
     * becomes part of the index and can be used for ranking in serve mode. Thus,
     * you should store the whatever is needed inside the same directory as
     * specified by _indexPrefix inside {@link _options}.
     *
     * @throws IOException
     */


    @Override
    public void compute() throws IOException {
        System.out.println("Computing using " + this.getClass().getName());
        int totalNumberOfDocs = docNameList.size();

        for (Map.Entry<Integer, HashMap<Integer, Double>> incomingLink : graph.entrySet()) {
            for (Map.Entry<Integer, Double> link : incomingLink.getValue().entrySet()) {
                link.setValue(lambda * link.getValue() + (1 - lambda) / totalNumberOfDocs);
            }
        }

        savePageRankToFile(graph);

        return;
    }

    private void savePageRankToFile(HashMap<Integer, HashMap<Integer, Double>> graph) {
        deleteFileIfExists(pageRankFile);
        File fout = new File(pageRankFile);
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(fout, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        int totalNumberOfDocs = docNameList.size();

        for (Map.Entry<Integer, HashMap<Integer, Double>> incomingLink : graph.entrySet()) {
            double pageRank = 0.0;
            for (Map.Entry<Integer, Double> link : incomingLink.getValue().entrySet()) {
                pageRank += link.getValue();
            }
            pageRank += (1 - lambda) * (docNameList.size() - incomingLink.getValue().size()) / totalNumberOfDocs;
            try {
                if (squareGoogleMatrix) {
                    while (incomingLink.getKey() > pageRankSquared.size()) {
                        pageRankSquared.add(pageRankSquared.size(), 0.0);
                    }
                    pageRankSquared.add(incomingLink.getKey(), pageRank);
                }
                else {
                    bw.write(incomingLink.getKey() + ":" + pageRank);
                    bw.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (squareGoogleMatrix) {

            for (Map.Entry<Integer, HashMap<Integer, Double>> incomingLink : graph.entrySet()) {
                double pageRankValue = 0.0;
                int[] cache = new int[graph.size()];
                for (Map.Entry<Integer, Double> link : incomingLink.getValue().entrySet()) {
                    pageRankValue += (link.getValue() * pageRankSquared.get(link.getKey()));
                    cache[link.getKey()] = 1;
                }
                for(int i = 0; i < pageRank.size(); i++) {
                    if(! (cache[i] == 1))
                    pageRankValue += pageRankSquared.get(i)*(1-lambda)/graph.size();
                }
                try {

                    bw.write(incomingLink.getKey() + ":" + pageRankValue);
                    bw.newLine();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createDocIdIndex() {

        deleteFileIfExists(docIDIndexFile);
        File fout = new File(docIDIndexFile);
        FileOutputStream fos = null;

        //Make the index directory if not exists
        File indexDir = new File(_options._indexPrefix);
        indexDir.mkdir();

        try {
            fos = new FileOutputStream(fout, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        int docIDIndex = 0;
        File folder = new File(_options._corpusPrefix);

        File[] directoryListing = folder.listFiles();
        Arrays.sort(directoryListing, new FileComparator());

        for (final File fileEntry : directoryListing) {
            if (!fileEntry.isDirectory() && !fileEntry.isHidden()) {
                try {
                    new WikiParser(fileEntry);
                } catch (Exception e) {
                    continue;
                }

                try {
                    bw.write(docIDIndex + ":" + fileEntry.getName());
                    bw.newLine();
                    docIDIndex++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void deleteFileIfExists(String fileName) {
        File f = new File(fileName);
        f.delete();
    }

    public void loadDocIDIndex() {

        try {
            // Open the file
            FileInputStream fstream = new FileInputStream(docIDIndexFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;
            docNameList.clear();
            docNameToDocId.clear();
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                String[] lineArray = strLine.split(":");
                docNameList.add(Integer.parseInt(lineArray[0]), lineArray[1]);
                docNameToDocId.put(lineArray[1], Integer.parseInt(lineArray[0]));
            }

            //Close the input stream
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * During indexing mode, this function loads the PageRank values computed
     * during mining mode to be used by the indexer.
     *
     * @throws IOException
     */
    @Override
    public Object load() throws IOException {
        System.out.println("Loading using " + this.getClass().getName());

        // Open the file
        FileInputStream fstream = new FileInputStream(pageRankFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;
        //Read File Line By Line
        while ((strLine = br.readLine()) != null) {
            // Print the content on the console
            String[] lineArray = strLine.split(":");
            int docIndex = Integer.parseInt(lineArray[0]);
            while (docIndex > pageRank.size()) {
                pageRank.add(pageRank.size(), 0.0);
            }
            if (docIndex > pageRank.size()) {

            }
            pageRank.add(Integer.parseInt(lineArray[0]), Double.parseDouble(lineArray[1]));

        }

        //Close the input stream
        br.close();
        return pageRank;
    }

    private void squareGoogleMatrix(HashMap<Integer, HashMap<Integer, Double>> graph) {
        try {
            DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(googleMatrix, false)));

            dataOut.writeInt(graph.size());

            for (int i = 0; i < graph.size(); ++i) {
                double sum = 0.0;
                System.out.println(i);
                for (int j = 0; j < graph.size(); ++j) {


                    Iterator it = graph.get(i).entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        int k = (int) pair.getKey();
                        double a, b;
                        if (!graph.containsKey(i)) {
                            a = (1 - lambda) / graph.size();
                        } else {
                            if (!graph.get(i).containsKey(k)) {
                                a = (1 - lambda) / graph.size();
                            } else {
                                a = graph.get(i).get(k) + (1 - lambda) / graph.size();
                            }
                        }

                        if (!graph.containsKey(k)) {
                            b = (1 - lambda) / graph.size();
                        } else {
                            if (!graph.get(k).containsKey(j)) {
                                b = (1 - lambda) / graph.size();
                            } else {
                                b = graph.get(k).get(j) * lambda + (1 - lambda) / graph.size();
                            }
                        }
                        sum += a * b;
                    }
                    //dataOut.writeDouble(sum);
                }
                sum += ((graph.size() - graph.get(i).size()) * (1 - lambda)) / graph.size();
                System.out.println("Sum:" + sum);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class FileComparator implements Comparator<File> {

        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }
}
