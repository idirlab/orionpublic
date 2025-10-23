package viiq.otherClassifiers.randomEdgeSuggestion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.log4j.Logger;

public class RandomEdgeSuggestor {
	Config conf = null;
//	final Logger logger = Logger.getLogger(getClass());
	
	public RandomEdgeSuggestor(Config conf) {
		this.conf = conf;
	}
	
	public void learnModel() {
		// nothing to learn for now. will draw an edge uniformly at random.
	}
	
	public static int getRandomEdge(HashSet<Integer> candidateEdges) {
		int edge = 0;
		Random rand = new Random();
		int randindex = rand.nextInt((candidateEdges.size()-1) + 1) + 0;
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext() && randindex >=0) {
			edge = iter.next();
			randindex--;
		}
		return edge;
	}
}
