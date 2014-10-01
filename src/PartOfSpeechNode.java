import java.util.ArrayList;

/**
 * A high-level class for representing the hierarchy of POS tags. Both the names and the hierarchy is taken from the
 * Penn TreeBank set of POS tags.
 * Because the nodes are meant to represent a dependency parse, each has one governor and 0 or more dependents.
 * @author Max Lotstein
 */
public class PartOfSpeechNode {
    // Each node may have theoretically many dependents
    ArrayList<TypedDependencyEdge> dependents;
    // But it will have exactly one governor
    TypedDependencyEdge governor;
    // Each node corresponds to a word
    String word;
    boolean isStartNode;

    public PartOfSpeechNode(String word){
        this.word = word;
        this.dependents = new ArrayList<TypedDependencyEdge>();
    }

    public ArrayList<TypedDependencyEdge> selectDependents(TypedDependencyEdgeFunction f){
        ArrayList<TypedDependencyEdge> selectedDependents = new ArrayList<TypedDependencyEdge>();
        if (dependents == null){
            return null;
        }
        for(TypedDependencyEdge curDependent : dependents){
            if(f.checkEdge(curDependent)) {
                selectedDependents.add(curDependent);
            }
        }
        return selectedDependents;
    }

    public boolean checkType(PartOfSpeechNodeFunction f) {
        return f.checkNode(this);
    }

    public void setIsStartNode(PartOfSpeechNodeFunction f) {
        this.isStartNode = f.checkNode(this);
    }

    @Override
    public String toString(){
        return this.word;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PartOfSpeechNode)) {
            return false;
        }
        PartOfSpeechNode p = (PartOfSpeechNode) obj;
        return this.word.equals(p.word) && this.dependents.containsAll(p.dependents) && p.dependents.containsAll(this.dependents);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

/*    @Override
    public boolean equals(Object obj){
        if (!(obj instanceof PartOfSpeechNode)) {
            return false;
        }
        PartOfSpeechNode p = (PartOfSpeechNode) obj;
        Set<TypedDependencyEdge> pDeps = new HashSet<TypedDependencyEdge>(p.dependents);
        Set<TypedDependencyEdge> deps = new HashSet<TypedDependencyEdge>(this.dependents);
        return word.equals(p.word) && pDeps.containsAll(deps) && deps.containsAll(pDeps) &&
    }*/
}
