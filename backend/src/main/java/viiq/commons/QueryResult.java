package viiq.commons;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.HashMap;

public class QueryResult {
	public LinkedHashMap<Integer, HashSet<Integer>> values;
	public ArrayList<ArrayList<Integer>> tuples;
	// public LinkedHashMap<Integer, Integer> nodeToTupleCol;
	// public HashMap<Integer, LinkedHashSet<Integer>> nodeValues;
	// public int curSortedCol;
}
