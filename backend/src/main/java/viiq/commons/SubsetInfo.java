package viiq.commons;
import java.util.HashSet;

public class SubsetInfo {
  //total denominator count for the decision path
  public int denominatorCount;
  //lineids in the user log that subsumes the decision path
  public HashSet<Integer> lineIDs;
}
