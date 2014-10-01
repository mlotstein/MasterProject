/**
 * @author Max Lotstein
 */
public class TypedDependencyEdge {
    // The head end refers to the dependent
    PartOfSpeechNode head;
    // While the tail refers to the governor
    PartOfSpeechNode tail;

    public TypedDependencyEdge(){
    }
    public boolean checkType(TypedDependencyEdgeFunction f){
        return f.checkEdge(this);
    }
}
