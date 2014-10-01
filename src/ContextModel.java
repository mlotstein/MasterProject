/**
 * Used to determine which sorts of contexts in a dependency parse tree are meaningful and which are not.
 * It maintains a state variable, patterns, each of can be matched against a different sort of meaningful path in a
 * dependency parse tree. The patterns are composed of predicate functions which select for either the nodes or edges
 * in a graphical representation of a dependency parse.
 *
 * The model used for context is based on the DepDM model described in Baroni, et. al. 2010
 * This is the description of DepDM from the DM paper:
 *
 * Our first DM model relies on the classic intuition that dependency paths are a good approximation to semantic
 * relations between words (Grefenstette 1994; Curran and Moens 2002; Pado and Lapata 2007; Rothenhausler and
 * Schutze 2009). DepDM is also the model with the least degree of link lexicalization among the three DM instances
 * we have built (its only lexicalized links are prepositions). DepDM includes the following noun-verb, noun-noun,
 * and adjective-noun links (in order to select more reliable dependencies and filter out possible parsing errors,
 * dependencies between words with more than five intervening items were discarded):
 * <ul>
 *     <li>sbj-intr</li>
 *     <li>sbj-tr</li>
 *     <li>sbj-tr</li>
 *     <li>obj</li>
 *     <li>iobj</li>
 *     <li>nmod</li>
 *     <li>coord</li>
 *     <li>prd</li>
 *     <li>verb</li>
 *     <li>preposition</li>
 * </ul>
 * @author  Max Lotstein
  */
public class ContextModel {
    /**
     * What follows is my attempts to describe each of the above links in terms of dependency parse features, and to
     * show what the entry into the tensor would look like. Note that all links (middle term) are delexicalized unless
     * otherwise noted. The arrow notation refers to directed edges in the dependency parse. NN and VB are meant to
     * represent classes of their respective types (e.g, VBZ, VBD, etc.).

     * WORKING in V1.0:
     * sbj_intr:    "The teacher is singing" : teacher, sbj_intr, sing
     *              "The soldier talked with his sergeant" : solider, sbj_intr, talk
     *              The pattern here is that the verb has no dependent connected via a dobj edge.
     *              NN ^ nsubj - VB - notdobj ^  Node
     * sbj_tr:      "The soldier is reading a book" : soldier, sbj_tr, read
     *              NN1 ^ nsubj - VB - dobj ^ NN2
     *              "The book is being read by the soldier" : soldier, sbj_tr, read
     *              This is the equivalent passive construction as the other case.
     *              NN ^ agent - VB
     * obj:         "The book is being read by the soldier" : book, obj, read
     *              NN ^ nsubjpass - VB
     *              "The soldier is reading a book" : book, obj, soldier
     *              VB - dobj ^ NN
     *              TODO: Add support for multiple direct objects that use coordination
     * iobj:        "The soldier gave the woman a book" : woman, iobj, gave
     *              VB - iobj^ NN
     *              "Paula passed her father the parcel" : father, iobj passed
     *              NN ^- nsubj - NN ^- xcomp - VBD
     *              "Simon gave his uncle a dity look" : uncle, iobj, gave
     *              NN ^- notnsubj - VBD - dobj : NN
     *              This one is tricky. We need to select the edge from the verb that isn't a direct object and
     *              isn't nsubj.
     * nmod:        "good teacher" : good, nmod, teacher
     *              JJ ^ amod - NN
     *              The entry is (JJ, nmod, NN)
     * coord:       "teachers and soldiers" : teacher, coord, soldier
     *              NN2 ^ conj - NN1 - cc ^
     *              TODO: Should this be extended for other classes (verbs, adjs) of words as well? e.g. I like to swim and fly.
     * prd:         "The soldier became sergeant" : sergeant, prd, became
     *              NN ^ nsubj - JJ - cop ^ VB
     *              "The woman became pregnant" : woman, prd, pregnant
     *              NN ^ nsubj - NN -  cop ^ VB
     * verb:        "The soldier talked with his sergeant" : soldier, verb, sergeant
     *              NN1 ^ nsubj - VB - prep ^ IN - pobj ^ NN2
     *              "The soldier is reading a book" : soldier, verb, book
     *              NN1 ^ nsubj - VB - dobj ^ NN2
     * preposition: VB - prep ^ IN -> pobj NN
     *              The entry is (NN, pre*, BV)  NOTE: all prepositional links are LEXICALIZED
     *
     * CANDIDATES FOR V2.0:
     * nmod:        JJ1 ^ amod - JJ2 ^ amod - NN (a chain of adjectives, using a comma)
     *              The two entries are (JJ1, nmod, NN) and (JJ2, nmod, NN)
     *              JJ1 ^ conj - JJ2 ^ amod NN (a chain of adjectives, using coordination)
     *              TODO: the above may be incorrect. See Stanford output for 'The young and happy man'
     *              The two entries are (JJ1, nmod, NN) and (JJ2, nmod, NN)
     *              NN1 - appos ^ NN2
     *              Apposition is arguably a sort of synonymy.
     *              TODO: consider for next version
     *              (NN1, nmod, NN2)
     *              NOTE: This is an extension beyond what DM does explicitly.
     * prd:         NN ^ nsubjpass - VB1 - auxpass ^ VB2
     *              TODO: the above rule over selects. While it is intended to capture 'The woman became pregnant' it also selects 'The man has been killed'
     *              The entry is (NN, prd, VB2)
     */
    /** A dependency path pattern is a bunch of predicates describing permissible nodes/edges and a name*/
    private DependencyPattern[] patterns;

    public ContextModel() {
        patterns = new DependencyPattern[] {
                new DependencyPattern("sbj_intr", new PredicateFunction[]{isnoun, togov, isnsubjnotpass, isverb, todep, isnotdobj, isnode}, 0, 1),
                new DependencyPattern("sbj_tr", new PredicateFunction[]{isnoun, togov, isnsubjnotpass, isverb, todep, isdobj, isnode}, 0, 1),
                new DependencyPattern("sbj_tr", new PredicateFunction[]{isnoun, togov, isagent, isverb}, 0, 1),
                new DependencyPattern("obj", new PredicateFunction[]{isnoun, togov, isnsubjpass, isverb}, 0, 1),
                //new DependencyPattern("obj", new PredicateFunction[]{isverb, todep, isdobj, isnoun}, 0, 1),
                new DependencyPattern("obj", new PredicateFunction[]{isnoun, togov, isdobj, isverb}, 0, 1),
                //new DependencyPattern("iobj", new PredicateFunction[]{isverb, todep, isiobj, isnoun}, 0, 1),
                new DependencyPattern("iobj", new PredicateFunction[]{isnoun, togov, isiobj, isverb}, 0, 1),
                new DependencyPattern("iobj", new PredicateFunction[]{isnoun, togov, ispobj, isto, togov, isprep, isverb, todep, isdobj, isnode}, 0, 2),
                new DependencyPattern("iobj", new PredicateFunction[]{isnoun, togov, isnsubj, isnoun, togov, isxcomp, isverb}, 0, 2),
                new DependencyPattern("iobj", new PredicateFunction[]{isnoun, togov, isnotsubj, isverb, todep, isdobj, isnoun}, 0, 1),
                //new DependencyPattern("nmod", new PredicateFunction[]{isadjective, togov, isamod, isnoun}, 0, 1),
                new DependencyPattern("nmod", new PredicateFunction[]{isnoun, todep, isamod, isadjective}, 0, 1),
                // new DependencyPattern("nmod", new PredicateFunction[]{isadjective, togov, isamod, isadjective, togov, isamod, isnoun}),
                // new DependencyPattern("nmod", new PredicateFunction[]{isadjective, togov, isconj, isadjective, togov, isamod, isnoun}),
                // new DependencyPattern("nmod", new PredicateFunction[]{isnoun, togov, isappos, isnoun}),
                new DependencyPattern("coord", new PredicateFunction[]{isnoun, togov, isconj, isnoun, todep, iscc}, 0, 1),
                new DependencyPattern("prd", new PredicateFunction[]{isnoun, togov, isnsubj, isadjective, todep, iscop, isverb}, 1, 2),
                new DependencyPattern("prd", new PredicateFunction[]{isnoun, togov, isnsubj, isnoun, todep, iscop, isverb}, 1, 2),
                new DependencyPattern("verb", new PredicateFunction[]{isnoun, togov, isnsubj, isverb, todep, isprep, isin, todep, ispobj, isnoun}, 0, 3),
                new DependencyPattern("verb", new PredicateFunction[]{isnoun, togov, isnsubj, isverb, todep, isdobj, isnoun}, 0, 2),
                // new DependencyPattern("prd", new PredicateFunction[]{isnoun, togov, isnsubjpass, isverb, todep, isauxpass, isverb}),
                new DependencyPattern("on", new PredicateFunction[]{isnoun, togov, ispobj, ison, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("upon", new PredicateFunction[]{isnoun, togov, ispobj, isupon, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("at", new PredicateFunction[]{isnoun, togov, ispobj, isat, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("among", new PredicateFunction[]{isnoun, togov, ispobj, isamong, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("between", new PredicateFunction[]{isnoun, togov, ispobj, isbetween, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("for", new PredicateFunction[]{isnoun, togov, ispobj, isfor, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("of", new PredicateFunction[]{isnoun, togov, ispobj, isof, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("with", new PredicateFunction[]{isnoun, togov, ispobj, iswith, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("by", new PredicateFunction[]{isnoun, togov, ispobj, isby, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("from", new PredicateFunction[]{isnoun, togov, ispobj, isfrom, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("within", new PredicateFunction[]{isnoun, togov, ispobj, iswithin, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("as", new PredicateFunction[]{isnoun, togov, ispobj, isas, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("into", new PredicateFunction[]{isnoun, togov, ispobj, isinto, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("under", new PredicateFunction[]{isnoun, togov, ispobj, isunder, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("than", new PredicateFunction[]{isnoun, togov, ispobj, isthan, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("along", new PredicateFunction[]{isnoun, togov, ispobj, isalong, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("throughout", new PredicateFunction[]{isnoun, togov, ispobj, isthroughout, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("like", new PredicateFunction[]{isnoun, togov, ispobj, islike, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("up", new PredicateFunction[]{isnoun, togov, ispobj, isup, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("above", new PredicateFunction[]{isnoun, togov, ispobj, isabove, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("if", new PredicateFunction[]{isnoun, togov, ispobj, isif, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("since", new PredicateFunction[]{isnoun, togov, ispobj, issince, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("without", new PredicateFunction[]{isnoun, togov, ispobj, iswithout, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("until", new PredicateFunction[]{isnoun, togov, ispobj, isuntil, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("beyond", new PredicateFunction[]{isnoun, togov, ispobj, isbeyond, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("unlike", new PredicateFunction[]{isnoun, togov, ispobj, isunlike, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("notwithstanding", new PredicateFunction[]{isnoun, togov, ispobj, isnotwithstanding, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("amongst", new PredicateFunction[]{isnoun, togov, ispobj, isamongst, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("that", new PredicateFunction[]{isnoun, togov, ispobj, isthat, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("against", new PredicateFunction[]{isnoun, togov, ispobj, isagainst, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("branch", new PredicateFunction[]{isnoun, togov, ispobj, isbranch, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("during", new PredicateFunction[]{isnoun, togov, ispobj, isduring, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("before", new PredicateFunction[]{isnoun, togov, ispobj, isbefore, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("though", new PredicateFunction[]{isnoun, togov, ispobj, isthough, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("after", new PredicateFunction[]{isnoun, togov, ispobj, isafter, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("below", new PredicateFunction[]{isnoun, togov, ispobj, isbelow, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("over", new PredicateFunction[]{isnoun, togov, ispobj, isover, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("except", new PredicateFunction[]{isnoun, togov, ispobj, isexcept, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("out", new PredicateFunction[]{isnoun, togov, ispobj, isout, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("about", new PredicateFunction[]{isnoun, togov, ispobj, isabout, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("per", new PredicateFunction[]{isnoun, togov, ispobj, isper, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("despite", new PredicateFunction[]{isnoun, togov, ispobj, isdespite, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("around", new PredicateFunction[]{isnoun, togov, ispobj, isaround, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("so", new PredicateFunction[]{isnoun, togov, ispobj, isso, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("through", new PredicateFunction[]{isnoun, togov, ispobj, isthrough, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("till", new PredicateFunction[]{isnoun, togov, ispobj, istill, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("behind", new PredicateFunction[]{isnoun, togov, ispobj, isbehind, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("towards", new PredicateFunction[]{isnoun, togov, ispobj, istowards, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("versus", new PredicateFunction[]{isnoun, togov, ispobj, isversus, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("outside", new PredicateFunction[]{isnoun, togov, ispobj, isoutside, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("across", new PredicateFunction[]{isnoun, togov, ispobj, isacross, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("toward", new PredicateFunction[]{isnoun, togov, ispobj, istoward, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("besides", new PredicateFunction[]{isnoun, togov, ispobj, isbesides, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("off", new PredicateFunction[]{isnoun, togov, ispobj, isoff, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("near", new PredicateFunction[]{isnoun, togov, ispobj, isnear, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("inside", new PredicateFunction[]{isnoun, togov, ispobj, isinside, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("round", new PredicateFunction[]{isnoun, togov, ispobj, isround, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("unto", new PredicateFunction[]{isnoun, togov, ispobj, isunto, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("atop", new PredicateFunction[]{isnoun, togov, ispobj, isatop, togov, isprep, isverb}, 0, 2),
                new DependencyPattern("down", new PredicateFunction[]{isnoun, togov, ispobj, isdown, togov, isprep, isverb}, 0, 2)};
    }

    public DependencyPattern[] getPatterns(){
        return this.patterns;
    }

    public PartOfSpeechNodeFunction getIsuseful() {
        return isuseful;
    }
    static final PartOfSpeechNodeFunction isnoun = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof NNNode;
        }
        public String toString() {
            return "NOUN";
        }
    };

    static final PartOfSpeechNodeFunction isnode = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof PartOfSpeechNode;
        }
        public String toString() {
        return "POSNode";
    }
    };

    static final PartOfSpeechNodeFunction isverb = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof VBNode;
        }
        public String toString() {
            return "VERB";
        }
    };
    static final PartOfSpeechNodeFunction isadjective = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof JJNode;
        }
        public String toString() {
            return "ADJ";
        }
    };

    static final PartOfSpeechNodeFunction isin = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof  INNode;
        }
        public String toString() {
            return "isin";
        }
    };

    static final PartOfSpeechNodeFunction isto = new PartOfSpeechNodeFunction() {
    @Override
    public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof  TONode;
            }
    public String toString() {
            return "ISTO";
            }
    };
    static final PartOfSpeechNodeFunction isuseful = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return (partOfSpeechNode instanceof  NNNode); //|| (partOfSpeechNode instanceof  JJNode) || (partOfSpeechNode instanceof  VBNode)|| (partOfSpeechNode instanceof PRP$Node);
        }
        public String toString() {
            return "isuseful";
        }
    };
    static final TypedDependencyEdgeFunction isnsubj = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof NSUBJEdge;
        }
        public String toString() {
            return "nsubj";
        }
    };
    static final TypedDependencyEdgeFunction isnsubjnotpass = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof NSUBJEdge && !(typedDependencyEdge instanceof NSUBJPASSEdge);
        }
        public String toString() {
            return "nsubjnotpass";
        }
    };
    static final TypedDependencyEdgeFunction isdobj = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof DOBJEdge;
        }
        public String toString() {
            return "dobj";
        }
    };
    static final NegatedTypedDependencyEdgeFunction isnotdobj = new NegatedTypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof DOBJEdge;
        }
        public String toString() {
            return "notdobj";
        }
    };
    static final TypedDependencyEdgeFunction isagent = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof AGENTEdge;
        }
        public String toString() {
            return "agent";
        }
    };
    static final TypedDependencyEdgeFunction isnsubjpass = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof NSUBJPASSEdge;
        }
        public String toString() {
            return "nsubjpass";
        }
    };
    static final TypedDependencyEdgeFunction isiobj = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof IOBJEdge;
        }
        public String toString() {
            return "iobj";
        }
    };
    static final TypedDependencyEdgeFunction isamod = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof AMODEdge;
        }
        public String toString() {
            return "amod";
        }
    };
    static final TypedDependencyEdgeFunction isconj = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof CONJEdge;
        }
        public String toString() {
            return "conj";
        }
    };
    static final TypedDependencyEdgeFunction isappos = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof APPOSEdge;
        }
        public String toString() {
            return "appos";
        }
    };
    static final TypedDependencyEdgeFunction iscop = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof COPEdge;
        }
        public String toString() {
            return "cop";
        }
    };
    static final TypedDependencyEdgeFunction isauxpass = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof AUXPASSEdge;
        }
        public String toString() {
            return "auxpass";
        }
    };
    static final TypedDependencyEdgeFunction isprep = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof PREPEdge;
        }
        public String toString() {
            return "prep";
        }
    };

    static final TypedDependencyEdgeFunction ispobj = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof POBJEdge;
        }
        public String toString() {
            return "pobj";
        }
    };

    static final TypedDependencyEdgeFunction isnotsubj = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof DEPEdge && !(typedDependencyEdge instanceof SUBJEdge);
        }
        public String toString() {
            return "notsubj";
        }
    };

    static final DirectionFunction togov = new DirectionFunction() {
        @Override
        public boolean isToGov() {
            return true;
        }
        public String toString() {
            return "togov";
        }
    };
    static final DirectionFunction todep = new DirectionFunction() {
        @Override
        public boolean isToGov() {
            return false;
        }
        public String toString() {
            return "todep";
        }
    };

    static final PartOfSpeechNodeFunction ison = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ONNode;
        }
        public String toString() {
            return "ONNode";
        }
    };
    static final PartOfSpeechNodeFunction isupon = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof UPONNode;
        }
        public String toString() {
            return "UPONNode";
        }
    };
    static final PartOfSpeechNodeFunction isat = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ATNode;
        }
        public String toString() {
            return "ATNode";
        }
    };
    static final PartOfSpeechNodeFunction isamong = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof AMONGNode;
        }
        public String toString() {
            return "AMONGNode";
        }
    };
    static final PartOfSpeechNodeFunction isbetween = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BETWEENNode;
        }
        public String toString() {
            return "BETWEENNode";
        }
    };
    static final PartOfSpeechNodeFunction isfor = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof FORNode;
        }
        public String toString() {
            return "FORNode";
        }
    };
    static final PartOfSpeechNodeFunction isof = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof OFNode;
        }
        public String toString() {
            return "OFNode";
        }
    };
    static final PartOfSpeechNodeFunction iswith = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof WITHNode;
        }
        public String toString() {
            return "WITHNode";
        }
    };
    static final PartOfSpeechNodeFunction isby = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BYNode;
        }
        public String toString() {
            return "BYNode";
        }
    };
    static final PartOfSpeechNodeFunction isfrom = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof FROMNode;
        }
        public String toString() {
            return "FROMNode";
        }
    };
    static final PartOfSpeechNodeFunction iswithin = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof WITHINNode;
        }
        public String toString() {
            return "WITHINNode";
        }
    };
    static final PartOfSpeechNodeFunction isas = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ASNode;
        }
        public String toString() {
            return "ASNode";
        }
    };
    static final PartOfSpeechNodeFunction isinto = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof INTONode;
        }
        public String toString() {
            return "INTONode";
        }
    };
    static final PartOfSpeechNodeFunction isunder = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof UNDERNode;
        }
        public String toString() {
            return "UNDERNode";
        }
    };
    static final PartOfSpeechNodeFunction isthan = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof THANNode;
        }
        public String toString() {
            return "THANNode";
        }
    };
    static final PartOfSpeechNodeFunction isalong = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ALONGNode;
        }
        public String toString() {
            return "ALONGNode";
        }
    };
    static final PartOfSpeechNodeFunction isthroughout = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof THROUGHOUTNode;
        }
        public String toString() {
            return "THROUGHOUTNode";
        }
    };
    static final PartOfSpeechNodeFunction islike = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof LIKENode;
        }
        public String toString() {
            return "LIKENode";
        }
    };
    static final PartOfSpeechNodeFunction isup = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof UPNode;
        }
        public String toString() {
            return "UPNode";
        }
    };
    static final PartOfSpeechNodeFunction isabove = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ABOVENode;
        }
        public String toString() {
            return "ABOVENode";
        }
    };
    static final PartOfSpeechNodeFunction isif = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof IFNode;
        }
        public String toString() {
            return "IFNode";
        }
    };
    static final PartOfSpeechNodeFunction issince = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof SINCENode;
        }
        public String toString() {
            return "SINCENode";
        }
    };
    static final PartOfSpeechNodeFunction iswithout = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof WITHOUTNode;
        }
        public String toString() {
            return "WITHOUTNode";
        }
    };
    static final PartOfSpeechNodeFunction isuntil = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof UNTILNode;
        }
        public String toString() {
            return "UNTILNode";
        }
    };
    static final PartOfSpeechNodeFunction isbeyond = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BEYONDNode;
        }
        public String toString() {
            return "BEYONDNode";
        }
    };
    static final PartOfSpeechNodeFunction isunlike = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof UNLIKENode;
        }
        public String toString() {
            return "UNLIKENode";
        }
    };
    static final PartOfSpeechNodeFunction isnotwithstanding = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof NOTWITHSTANDINGNode;
        }
        public String toString() {
            return "NOTWITHSTANDINGNode";
        }
    };
    static final PartOfSpeechNodeFunction isamongst = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof AMONGSTNode;
        }
        public String toString() {
            return "AMONGSTNode";
        }
    };
    static final PartOfSpeechNodeFunction isthat = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof THATNode;
        }
        public String toString() {
            return "THATNode";
        }
    };
    static final PartOfSpeechNodeFunction isagainst = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof AGAINSTNode;
        }
        public String toString() {
            return "AGAINSTNode";
        }
    };
    static final PartOfSpeechNodeFunction isbranch = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BRANCHNode;
        }
        public String toString() {
            return "BRANCHNode";
        }
    };
    static final PartOfSpeechNodeFunction isduring = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof DURINGNode;
        }
        public String toString() {
            return "DURINGNode";
        }
    };
    static final PartOfSpeechNodeFunction isbefore = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BEFORENode;
        }
        public String toString() {
            return "BEFORENode";
        }
    };
    static final PartOfSpeechNodeFunction isthough = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof THOUGHNode;
        }
        public String toString() {
            return "THOUGHNode";
        }
    };
    static final PartOfSpeechNodeFunction isafter = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof AFTERNode;
        }
        public String toString() {
            return "AFTERNode";
        }
    };
    static final PartOfSpeechNodeFunction isbelow = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BELOWNode;
        }
        public String toString() {
            return "BELOWNode";
        }
    };
    static final PartOfSpeechNodeFunction isover = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof OVERNode;
        }
        public String toString() {
            return "OVERNode";
        }
    };
    static final PartOfSpeechNodeFunction isexcept = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof EXCEPTNode;
        }
        public String toString() {
            return "EXCEPTNode";
        }
    };
    static final PartOfSpeechNodeFunction isout = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof OUTNode;
        }
        public String toString() {
            return "OUTNode";
        }
    };
    static final PartOfSpeechNodeFunction isabout = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ABOUTNode;
        }
        public String toString() {
            return "ABOUTNode";
        }
    };
    static final PartOfSpeechNodeFunction isper = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof PERNode;
        }
        public String toString() {
            return "PERNode";
        }
    };
    static final PartOfSpeechNodeFunction isdespite = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof DESPITENode;
        }
        public String toString() {
            return "DESPITENode";
        }
    };
    static final PartOfSpeechNodeFunction isaround = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof AROUNDNode;
        }
        public String toString() {
            return "AROUNDNode";
        }
    };
    static final PartOfSpeechNodeFunction isso = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof SONode;
        }
        public String toString() {
            return "SONode";
        }
    };
    static final PartOfSpeechNodeFunction isthrough = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof THROUGHNode;
        }
        public String toString() {
            return "THROUGHNode";
        }
    };
    static final PartOfSpeechNodeFunction istill = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof TILLNode;
        }
        public String toString() {
            return "TILLNode";
        }
    };
    static final PartOfSpeechNodeFunction isbehind = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BEHINDNode;
        }
        public String toString() {
            return "BEHINDNode";
        }
    };
    static final PartOfSpeechNodeFunction istowards = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof TOWARDSNode;
        }
        public String toString() {
            return "TOWARDSNode";
        }
    };
    static final PartOfSpeechNodeFunction isversus = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof VERSUSNode;
        }
        public String toString() {
            return "VERSUSNode";
        }
    };
    static final PartOfSpeechNodeFunction isoutside = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof OUTSIDENode;
        }
        public String toString() {
            return "OUTSIDENode";
        }
    };
    static final PartOfSpeechNodeFunction isacross = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ACROSSNode;
        }
        public String toString() {
            return "ACROSSNode";
        }
    };
    static final PartOfSpeechNodeFunction istoward = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof TOWARDNode;
        }
        public String toString() {
            return "TOWARDNode";
        }
    };
    static final PartOfSpeechNodeFunction isbesides = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof BESIDESNode;
        }
        public String toString() {
            return "BESIDESNode";
        }
    };
    static final PartOfSpeechNodeFunction isoff = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof OFFNode;
        }
        public String toString() {
            return "OFFNode";
        }
    };
    static final PartOfSpeechNodeFunction isnear = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof NEARNode;
        }
        public String toString() {
            return "NEARNode";
        }
    };
    static final PartOfSpeechNodeFunction isinside = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof INSIDENode;
        }
        public String toString() {
            return "INSIDENode";
        }
    };
    static final PartOfSpeechNodeFunction isround = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ROUNDNode;
        }
        public String toString() {
            return "ROUNDNode";
        }
    };
    static final PartOfSpeechNodeFunction isunto = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof UNTONode;
        }
        public String toString() {
            return "UNTONode";
        }
    };
    static final PartOfSpeechNodeFunction isatop = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof ATOPNode;
        }
        public String toString() {
            return "ATOPNode";
        }
    };
    static final PartOfSpeechNodeFunction isdown = new PartOfSpeechNodeFunction() {
        @Override
        public boolean checkNode(PartOfSpeechNode partOfSpeechNode) {
            return partOfSpeechNode instanceof DOWNNode;
        }
        public String toString() {
            return "DOWNNode";
        }
    };




    static final TypedDependencyEdgeFunction isxcomp = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof XCOMPEdge;
        }
        public String toString() {
            return "xcomp";
        }
    };

    static final TypedDependencyEdgeFunction iscc = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof CCEdge;
        }
        public String toString() {
            return "cc";
        }
    };
    static final TypedDependencyEdgeFunction isdep = new TypedDependencyEdgeFunction() {
        @Override
        public boolean checkEdge(TypedDependencyEdge typedDependencyEdge) {
            return typedDependencyEdge instanceof TypedDependencyEdge;
        }
        public String toString() {
            return "dep";
        }
    };

}