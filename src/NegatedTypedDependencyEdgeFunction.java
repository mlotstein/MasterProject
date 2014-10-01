/**
 * Created by Max on 6/3/2014.
 */
public interface NegatedTypedDependencyEdgeFunction extends TypedDependencyEdgeFunction {
    boolean checkEdge(TypedDependencyEdge typedDependencyEdge);
}
