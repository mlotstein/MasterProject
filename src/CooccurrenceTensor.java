import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.BiMap;

/**
 * Created by Max on 8/20/2014.
 */
public class CooccurrenceTensor {

    HashMap<Integer, HashMap<Integer, HashMap<Integer, Long>>> tensor;
    BiMap<String, Integer> wordMap;
    BiMap<String, Integer> linkMap;
    int numWords;
    int numLinks;

    public CooccurrenceTensor() {
        this.tensor = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Long>>>();
        this.wordMap = HashBiMap.create();
        this.linkMap = HashBiMap.create();
    }

    public void add(String word1, String link, String word2, Long score) {
        int word1Num, linkNum, revLinkNum, word2Num;
        if (wordMap.containsKey(word1)) {
            word1Num = wordMap.get(word1);
        } else {
            wordMap.put(word1, numWords);
            word1Num = numWords;
            numWords++;
        }
        if (wordMap.containsKey(word2)) {
            word2Num = wordMap.get(word2);
        } else {
            wordMap.put(word2, numWords);
            word2Num = numWords;
            numWords++;
        }
        if (linkMap.containsKey(link)) {
            linkNum = linkMap.get(link);
            revLinkNum = linkMap.get(link + "_rev");

        } else {
            linkMap.put(link, numLinks);
            linkNum = numLinks;
            numLinks++;
            linkMap.put(link + "_rev", numLinks);
            revLinkNum = numLinks;
            numLinks++;
        }
        if (this.tensor.containsKey(word1Num)){
            // and it has occurred with this link
            if (this.tensor.get(word1Num).containsKey(linkNum)) {
                // and both have occurred with second word
                if (this.tensor.get(word1Num).get(linkNum).containsKey(word2Num)) {
                    // then simply add to the scoreuency
                    score += this.tensor.get(word1Num).get(linkNum).get(word2Num);
                    // and update the value in the tensor
                    this.tensor.get(word1Num).get(linkNum).put(word2Num, score);
                } else {
                    // we need to add a new entry
                    this.tensor.get(word1Num).get(linkNum).put(word2Num, score);
                }
            } else {
                // we need to create the inner hasmatrixap
                HashMap<Integer, Long> innerHM1 = new HashMap<Integer, Long>();
                innerHM1.put(word2Num,  score);
                // and then put it into the top-level hasmatrixap
                this.tensor.get(word1Num).put(linkNum, innerHM1);
            }
        } else {
            // we need to create the inner hasmatrixap
            HashMap<Integer, Long> innerHM1 = new HashMap<Integer, Long>();
            innerHM1.put(word2Num, score);
            // and then create the middle hasmatrixap
            HashMap<Integer, HashMap<Integer, Long>> middleHM1 = new HashMap<Integer, HashMap<Integer, Long>>();
            middleHM1.put(linkNum, innerHM1);
            // and then put it into the top-level hasmatrixap
            this.tensor.put(word1Num, middleHM1);
        }
    }

    /**
     * Takes the 3D tensor and linearizes two dimensions to make it a 2D tensor (or tensor).
     * Currently ignores parameter and matricizes to Word x Link-Word tensor
     * @param dimNums This array of integers in the range [1 - 3] represents which dimension will serve as the
     *                rows of the resulting tensor.
     * @return  A 2D tensor form of the 3D tensor.
     */
    private HashMap<String, HashMap<String, Long>> matricize(int[] dimNums ) {
        // TODO: make matricize use the parameters
        HashMap<String, HashMap<String, Long>> tensor2D = new HashMap<String, HashMap<String, Long>>();
        // For every key in the outer level of the tensor
        for (Map.Entry<Integer, HashMap<Integer, HashMap<Integer, Long>>> outer : this.tensor.entrySet()) {
            Integer outerKey = outer.getKey();
            String outerKeyString = wordMap.inverse().get(outerKey);
            for (Map.Entry<Integer, HashMap<Integer, Long>> middle : outer.getValue().entrySet()) {
                Integer middleKey = middle.getKey();
                String middleKeyString = linkMap.inverse().get(middleKey);
                for (Map.Entry<Integer, Long> inner : middle.getValue().entrySet()) {
                    Integer innerKey = inner.getKey();
                    String innerKeyString = wordMap.inverse().get(innerKey);
                    if (tensor2D.containsKey(outerKeyString)) {
                        tensor2D.get(outerKeyString).put(middleKeyString + "_" + innerKeyString, inner.getValue());
                    } else {
                        HashMap<String, Long> newEntry = new HashMap<String, Long>();
                        newEntry.put(middleKey + "_" + innerKey, inner.getValue());
                        tensor2D.put(outerKeyString, newEntry);
                    }
                }
            }
        }
        return tensor2D;
    }


    /**
     * Writes the tensor to disk in a format immediately readable by DISSECT
     * DISSECT needs three files:
     *      (1) a file called rows of the format [word1]\n[word2]...
     *      (2) a file called cols, using the same format
     *      (3) a sparse format tensor file in the format [word1] [word2] [freq]
     * @param dimNums This argument, which will be passed on to matricize, determines which dimensions will be used for
     *                the rows of the tensor.
     */
    public void formatForDissect(int[] dimNums) {


        HashMap<String, HashMap<String, Long>> matrix = matricize(dimNums);
        HashSet<String> features = new HashSet<String>();
        Writer rowWriter = null;
        Writer colWriter = null;
        Writer mWriter = null;

        try {
            long sysTime = System.currentTimeMillis();
            rowWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("row" + sysTime +".rows"), "utf-8"));
            colWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("col" + sysTime +".cols"), "utf-8"));
            mWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("tensor" + sysTime +".sm"), "utf-8"));
            // Loop over rows in tensor and write both the row names and the sparse data to files
            for (Map.Entry<String, HashMap<String, Long>> row : matrix.entrySet()) {
                String word = row.getKey();
                rowWriter.write(word + "\n");
                HashMap<String, Long> hm  = row.getValue();
                for (Map.Entry< String,Long> col : hm.entrySet()) {
                    mWriter.write(word + " " + col.getKey() + " " + col.getValue() + "\n");
                    features.add(col.getKey());
                }
            }

            // Loop over the column names
            // NOTE they are not in any particular order
            Iterator iter = features.iterator();
            while (iter.hasNext()) colWriter.write(iter.next() + "\n");

        } catch (IOException ex) {
            // report
        } finally {
            try {
                rowWriter.close();
                colWriter.close();
                mWriter.close();
            } catch (NullPointerException ex) {
                System.err.println(ex.toString());
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }

    }
}
