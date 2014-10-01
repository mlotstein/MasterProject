/**
 * Author: Max Lotstein, lotsteim@uni-freiburg.de,mlotstein@gmail.com
 * Copyright 2014
 *
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads a file collected from Google's Syntactic N-Grams corpus, build an intermediate graphical
 * representation of every line, traverse the graph looking for paths deemed informative by the context model and
 * use those paths to build a tensor of co-occurrence statistics.
 *
 * @author Max Lotstein
 */
public  class DistributionalModel {
    /** represents co-occurrences of words in the corpus.*/
    private CooccurrenceTensor tensor;
    /** controls which sorts of co-occurrences are meaningful and which are not.*/
    private ContextModel cm;
    /** parses N-Gram from corpus and returns a graphical representation */
    private NGramParser nGramParser;
    /** These regex patterns will be used by the parse methods to select only tokens composed of alphabetical characters*/
    static final String onlyLettersPattern = "^[a-zA-Z]*$";
    static final String onlyNumbersPattern = "^[0-9]*$";

    /**
     * The constructor of the DistributionalModel
     */
    public DistributionalModel() {
        tensor = new CooccurrenceTensor();
        cm = new ContextModel();
        nGramParser = new NGramParser();
    }

    /**
     * The main loop which opens the NGram file, processes it, and prints the execution time.
     * @param args The name of the NGram file
     */
    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("Usage: java DistributionalModel <ngram-file>");
            System.exit(1);
        }
        double begin = System.currentTimeMillis();
        DistributionalModel distributionalModel = new DistributionalModel();

        try {
            File file = new File(args[0]);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(new FileInputStream(file),
                            "UTF-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                distributionalModel.processLine(line);
            }
            reader.close();
            System.out.println("Done with extraction!");
            System.out.println("Extraction Time:" + (System.currentTimeMillis() - begin) + "ms");
            System.out.println("The tensor has " + distributionalModel.tensor.wordMap.size() + " unique words.");
            distributionalModel.tensor.formatForDissect(new int[]{1});
            System.out.println("Total time:" + (System.currentTimeMillis() - begin) + "ms");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * This method accepts a single tab-separated line from the N-Gram corpus file, which represents a fragment of a
     * dependency parse tree, and uses helper methods to:
     *      (1) build a graphical representation
     *      (2) use that representation along with the context model to identify meaningful paths in that graph
     *      (3) take each meaningful co-occurrence and add it to the tensor
     * @param line A tab separated line from the Google N-Grams corpus. According to spec, each line has the following
     *             format:
     *             [head_word]\t[syntactic-ngram]\t[total_count]\t[counts_by_year]
     *             where:
     *                  - head_word is a string, e.g., 'cease'
     *                  - syntactic-ngram is a space-separated list
     *                  - total_count is an integer
     *                  - counts_by_year is actually a tab-separated list of [year],[int]
     */
    private void processLine(String line) {
        /**
         */
        String[] values = line.split("\\t");
        String nGram = values[1];
        long total_count = Integer.parseInt(values[2]);
        PartOfSpeechNode[] graph = nGramParser.parse(nGram);
        if (graph != null && total_count > 0) {
            HashSet<DependencyPath> coocurrences = extract(graph);
            for (DependencyPath d : coocurrences) {
                addToTensor(d, total_count);
            }
        }
    }

    /**
     * Accepts a dependency path and an integer representing co-occurrence frequency and updates the tensor object.
     *
     * NOTE: in previous versions, the tensor entry would be POS-sensitive but in this version, it is POS-agnostic.
     * @param d a DependencyPath
     * @param freq an integer, representing the number of times the dependency path was observed in the corpus
     */
    public void addToTensor(DependencyPath d, long freq) {
        // UPDATE: as per 7-17-2014, tensor will now be POS-agnostic
        // The tensor requires that words be stored word_POS so we must extract the POS tag from the class name
        //String firstPOSClassName = d.words.get(firstWordIndex).getClass().toString();
        //String secondPOSClassName =  d.words.get(secondWordIndex).getClass().toString();
        //String posOne = firstPOSClassName.substring(firstPOSClassName.indexOf(' ')
        //        + 1, firstPOSClassName.lastIndexOf('N'));
        //String posTwo = secondPOSClassName.substring(secondPOSClassName.indexOf(' ')
        //        + 1, secondPOSClassName.lastIndexOf('N'));

        String firstWord = d.words.get(d.firstWordNumber).word; //+ "_" + posOne;
        String secondWord = d.words.get(d.secondWordNumber).word; //+ "_" +posTwo;
        tensor.add(firstWord, d.patternName, secondWord, freq);
    }

    /**
     * Given an array of nodes that comprise a graph, use syntactic nGram of arcs, update the tensor to reflect
     * meaningful associations between pairs of content words.
     * @param graph An array of PartOfSpeechNodes that represents a graphical representation of the dependency parse
     * @return A set of dependency paths
     */
    public HashSet<DependencyPath> extract(PartOfSpeechNode[] graph) {
        // A list of entries to ultimately be put into the tensor
        // Each tensor entry is <word1, link, word2>.
        HashSet<DependencyPath> entries = new HashSet<DependencyPath>();
        // For each elt
        for (int nodeNum = 0; nodeNum < graph.length; nodeNum++) {
            // If the current word depends is an acceptable start node
            if (graph[nodeNum].isStartNode) {
                // Then, for each possible pattern
                for (DependencyPattern d : cm.getPatterns()) {
                    // Add all informative paths
                    Set<DependencyPath> newPaths = DFSfindPathsRecursive(graph[nodeNum], 0, d, new DependencyPath(d.name, d.firstWordNumber, d.secondWordNumber), new ArrayList<PartOfSpeechNode>());
                    if (newPaths != null) {
                        entries.addAll(newPaths);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * This method explores a graph depth-first, looking for paths satisfying criteria, and returns all such paths.
     * @param node a node, representing the starting point for the remaining steps in the path
     * @param funcNum the current 'step' in the pattern (as each step is a PredicateFunction (constraint)
     * @param pattern a DependencyPattern object that describes criteria for the set of paths that must be returned
     * @param path An object that includes the words seen so far as well as the name of the pattern
     * @param visited An array of already-visited nodes
     * @return A set of paths that satisfy the criteria
     */
    private HashSet<DependencyPath> DFSfindPathsRecursive(PartOfSpeechNode node, int funcNum, DependencyPattern pattern, DependencyPath path, ArrayList<PartOfSpeechNode> visited) {
        // If the current graph[nodeNum] matches the first thing in the path and we haven't visited yet
        if(node.checkType((PartOfSpeechNodeFunction) pattern.predicates[funcNum]) && !visited.contains(node)) {
            // Add visited node to the list of explored nodes
            visited.add(node);
            // Create a new dependency path object, add the new word to it
            HashSet<DependencyPath> newCompletePaths = new HashSet<DependencyPath>();
            DependencyPath newPath = new DependencyPath(pattern.name, path.words, pattern.firstWordNumber, pattern.secondWordNumber);
            newPath.words.add(node);
            // If this is the last thing in the path
            if (pattern.functionsRemaining(funcNum) == 0) {
                // Add the new path to the list of paths
                newCompletePaths.add(newPath);
            } else {
                // Otherwise, we need to consider all possible resulting paths
                // The direction of the edge is specified in the next predicate function
                DirectionFunction df = (DirectionFunction) pattern.predicates[funcNum + 1];
                // If the edge points to the governor of the current node, there is only one, so we can check to see if
                // it matches and is unseen
                if (df.isToGov() && node.governor != null &&
                        node.governor.checkType((TypedDependencyEdgeFunction) pattern.predicates[funcNum + 2])) {
                    // If this is then the last thing in the path, add the resulting path to the list of possible paths
                    if (pattern.functionsRemaining(funcNum) == 2) {
                        newCompletePaths.add(newPath);
                    } else {
                        // If this edge isn't the last predicate function, we need to look recursively at the governor
                        // of the current node and add the resulting paths to the list of possible paths
                        ArrayList<PartOfSpeechNode> newVisitedList = new ArrayList<PartOfSpeechNode>(visited);
                        HashSet<DependencyPath> newPaths = DFSfindPathsRecursive(node.governor.tail, funcNum + 3,
                                pattern, newPath, newVisitedList);
                        if (newPaths != null) {
                            for (DependencyPath p : newPaths) {
                                if (p != null) {
                                    newCompletePaths.add(p);
                                }
                            }
                        }
                    }
                    // If the edge doesn't point to the governor, then it must point to a dependent
                    // If this node actually has dependents

                } else if (!df.isToGov() && node.dependents != null) {
                    ArrayList<TypedDependencyEdge> dependents =
                            node.selectDependents((TypedDependencyEdgeFunction) pattern.predicates[funcNum + 2]);
                    // If the predicate is negated -- ie, should return 0
                    if (pattern.predicates[funcNum + 2] instanceof NegatedTypedDependencyEdgeFunction) {
                        if (dependents.size() == 0 && pattern.functionsRemaining(funcNum) == 2) {
                                newCompletePaths.add(newPath);
                            } else {
                                // TODO Consider case where a negated typeddep is NOT the final predicate function
                            }
                    } else {
                        // Then we need to consider all possible dependents that match the edge constraint
                        // For each matching edge
                        for (TypedDependencyEdge e : dependents) {
                            // If this edge is the last thing in the path
                            if (pattern.functionsRemaining(funcNum) == 2) {
                                newCompletePaths.add(newPath);
                            } else {
                                // Find all possible paths recursively
                                ArrayList<PartOfSpeechNode> newVisitedList = new ArrayList<PartOfSpeechNode>(visited);
                                HashSet<DependencyPath> newPaths = DFSfindPathsRecursive(e.head, funcNum + 3, pattern, newPath, newVisitedList);
                                if (newPaths != null) {
                                    for (DependencyPath d : newPaths) {
                                        // If the path is not null, add it
                                        if (d != null) {
                                            newCompletePaths.add(d);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return newCompletePaths;
        }
        return null;
    }

    private class NGramParser {
        public NGramParser() {
        }
        /**
         * Constructs a graph, given a space-separated list that represent dependency information of a tree fragment, s.t.
         * that each word is a node, and each word is connected to its head via directed edges.
         * @param nGram a space-separated list, each elt of the form 'word/pos-tag/dep-label/head-index'
         *              From the README file: Items in the "arcs" dataset represent direct relations between two content
         *              words. The arcs datasets reflect direct head-modifier relations, which are the predominant source
         *              of information in current-day syntax-based lexical-semantic models.
         *              Example:
         *              statistical/JJ/amod/2 efficiency/NN/pobj/0
         *              efficient/JJ/amod/0 or/CC/cc/1 effective/JJ/conj/1
         * @return an array representing (via depenendents/governor of each elt. of array) a graph
         */
        public PartOfSpeechNode[] parse(String nGram) {
            // The list of items is separated by whitespace characters
            String[] elts = nGram.split(" ");
            // Each elt refers to a unique node
            PartOfSpeechNode[] nodes = new PartOfSpeechNode[elts.length];
            // ... and a unique edge
            TypedDependencyEdge[] edges = new TypedDependencyEdge[elts.length];
            // This will store the tail of every edge such that governorIndex[i] = j reflects the connection (i <- j)
            int[] governorIndex = new int[elts.length];
            // For each elt, unpack its contents and construct a node and vertex
            for(int eNum = 0; eNum < elts.length; eNum++){
                // Each elt contains is a bunch of things, delimited by '/'
                String[] tokens = elts[eNum].split("/");
                String curPosTag, curDepLabel;
                int curGovIndex;
                String curWord = tokens[0];
                // If the current word has is not strictly letters or strictly numbers characters, skip the n-gram
                if (!curWord.matches(onlyLettersPattern) && !curWord.matches(onlyNumbersPattern) || (curWord.length() == 1 && !curWord.equals("a"))) {
                    return null;
                }
                if (tokens.length !=  4) {
                    return null;
                } else {
                    curPosTag = tokens[1];
                    curDepLabel = tokens[2];
                    try {
                        curGovIndex = Integer.parseInt(tokens[3]);
                    } catch (NumberFormatException e) {
                        System.out.println(e.toString());
                        return null;
                    }
                }

                // Add the curGovIndex to the array of heads, to be processed after we have all nodes/edges
                governorIndex[eNum] = curGovIndex;
                try {
                    Class<?> nodeClass;
                    // The class for the node is simply the part-of-speech tag + the word 'Node'
                    if (curPosTag.equals("IN")) {
                        nodeClass = Class.forName(curWord.toUpperCase() + "Node");
                    } else {
                        nodeClass = Class.forName(curPosTag + "Node");
                    }
                    Constructor<?> nodeConstructor = nodeClass.getConstructor(String.class);
                    // The node knows what word it is
                    nodes[eNum] = (PartOfSpeechNode) nodeConstructor.newInstance(curWord);
                    // Start nodes are those that are 'useful' -- ie, occur are terminal nodes in a pattern
                    nodes[eNum].setIsStartNode(cm.getIsuseful());
                    // The class for the edge is simply the dependency label, capitalized, plus the word 'Edge'
                    edges[eNum] = (TypedDependencyEdge) Class.forName(curDepLabel.toUpperCase() + "Edge").getConstructor().newInstance();
                    // At present, we can only guarantee that we know the head of each edge (the current node) and the
                    // governor of each node
                    // If we are at the root node of the tree fragment, then the node has no governor, so it's set to null
                    if (curGovIndex != 0) {
                        edges[eNum].head = nodes[eNum];
                        nodes[eNum].governor = edges[eNum];
                    } else {
                        edges[eNum] = null;
                        nodes[eNum].governor = null;
                    }
                } catch (ClassNotFoundException e) {
                    // If there is no such class, then completely ignore the NGram
                    //System.err.println(e.toString());
                    // Since these are generally due to non-standard dependency edges or parts of speech, e.g. abbrev
                    //System.err.println(e.toString());
                    return null;
                } catch (NoSuchMethodException e) {
                    System.err.println(e.toString());
                } catch (InstantiationException e) {
                    System.err.println(e.toString());
                } catch (IllegalAccessException e) {
                    System.err.println(e.toString());
                } catch (InvocationTargetException e) {
                    System.err.println(e.toString());
                }
            }
            // Complete the graph structure by iterating over nodes and capturing dependent info
            // For each node
            for (int eNum = 0; eNum < governorIndex.length; eNum++){
                // If it has as governor
                if (nodes[eNum].governor != null && governorIndex[eNum] != 0) {
                    // Add it to the list of dependents of its governor
                    nodes[governorIndex[eNum] - 1].dependents.add(edges[eNum]);
                    // Set the tail end of the node's edge to point at the governor node
                    edges[eNum].tail = nodes[governorIndex[eNum] - 1];
                }
            }
            return nodes;
        }
    }
}