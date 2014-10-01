/**
 * This class is used by ContextModel to express patterns of nodes and edges in a graphical representation of a
 * dependency parsed. A pattern is simply a set of constraints, which are here referred to as predicates, since they
 * are boolean, as well as indices of the words to store in the tensor.
 * @author Max Lotstein
 */
public class DependencyPattern {
    String name;
    PredicateFunction[] predicates;
    int firstWordNumber, secondWordNumber;

    public DependencyPattern(String name, PredicateFunction[] predicates, int firstWordNumber, int secondWordNumber){
        this.name = name;
        this.predicates = predicates;
        this.firstWordNumber = firstWordNumber;
        this.secondWordNumber = secondWordNumber;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(name + ": ");
        for (int i = 0; i < predicates.length; i++) {
            if (predicates[i] instanceof PartOfSpeechNodeFunction) {
                s.append(predicates[i].toString());
            } else if (predicates[i] instanceof DirectionFunction) {
                if (predicates[i].toString().equals("togov")) {
                    s.append("<-");
                } else {
                    s.append("-");
                }
            } else if (predicates[i] instanceof TypedDependencyEdgeFunction) {
                if (predicates[i - 1].toString().equals("togov")) {
                    s.append(predicates[i].toString() + "-");
                } else {
                    s.append(predicates[i].toString() + "->");
                }
            }
        }

        return s.toString();
    }

    public int functionsRemaining(int functionsCompared) {
        return this.predicates.length - functionsCompared - 1;
    }
}
