import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is used to store sequences of nodes and edges that correspond to meaningful co-occurrences of words.
 * patternName is the name of the DependencyPattern
 * words is the list of words
 * @author Max
 */
public class DependencyPath {
    String patternName;
    ArrayList<PartOfSpeechNode> words;
    int firstWordNumber, secondWordNumber;

    public DependencyPath(String patternName, ArrayList<PartOfSpeechNode> words, int firstWordNumber, int secondWordNumber) {
        this.patternName = patternName;
        this.words = words;
        this.firstWordNumber = firstWordNumber;
        this.secondWordNumber = secondWordNumber;
    }

    public DependencyPath(String patternName, int firstWordNumber, int secondWordNumber) {
        this.patternName = patternName;
        this.words = new ArrayList<PartOfSpeechNode>();
        this.firstWordNumber = firstWordNumber;
        this.secondWordNumber = secondWordNumber;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(patternName);
        if (this.words != null) {
            s.append(this.words.toString());
        }

        return s.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DependencyPath)) {
            return false;
        }
        DependencyPath p = (DependencyPath) obj;
        PartOfSpeechNode[] thisArray = this.words.toArray(new PartOfSpeechNode[p.words.size()]);
        PartOfSpeechNode[] objArray = p.words.toArray(new PartOfSpeechNode[p.words.size()]);
        // NOTE: patternName is intentionally NOT considered for equality comparison
        return (Arrays.deepEquals(thisArray, objArray));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
