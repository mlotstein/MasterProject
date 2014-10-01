import java.util.ArrayList;

/**
 * Created by Max on 5/26/2014.
 */
public class NNPNode extends NNNode {
    public NNPNode(String word) {
        super(word);
    }

    public static void main(String[] args) {

        NNPNode nnpNode = new NNPNode(null);
        AGENTEdge a = new AGENTEdge();
        ArrayList<TypedDependencyEdge> children = new ArrayList<TypedDependencyEdge>();
        children.add(a);
        nnpNode.dependents = children;
        System.out.println("The dependents that are of type AgentEdge:" + nnpNode.selectDependents(new TypedDependencyEdgeFunction() {
            @Override
            public boolean checkEdge(TypedDependencyEdge e) {
                if (e instanceof AGENTEdge) {
                    return true;
                } else {
                    return false;
                }

            }
        }));
        System.out.println("The dependents that are instances of ArgumentEdge:" + nnpNode.selectDependents(new TypedDependencyEdgeFunction() {
            @Override
            public boolean checkEdge(TypedDependencyEdge e) {
                if (e instanceof ARGEdge) {
                    return true;
                } else {
                    return false;
                }

            }
        }));
        System.out.println("The chlidren that are instances of AuxiliaryEdge:" + nnpNode.selectDependents(new TypedDependencyEdgeFunction() {
            @Override
            public boolean checkEdge(TypedDependencyEdge e) {
                if (e instanceof AUXEdge) {
                    return true;
                } else {
                    return false;
                }

            }
        }));
        System.out.println("The chlidren that are not instances of AuxiliaryEdge:" + nnpNode.selectDependents(new TypedDependencyEdgeFunction() {
            @Override
            public boolean checkEdge(TypedDependencyEdge e) {
                if (!(e instanceof AUXEdge)) {
                    return true;
                } else {
                    return false;
                }

            }
        }));

    }
}
