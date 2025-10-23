package viiq.otherClassifiers.naiveBayesian;

import java.util.HashMap;

public class NaiveBayesianHelper {
	
	public void populatePriorCounts(String[] eles, HashMap<Integer, Probs> priorProbability) {
		int prop = Integer.parseInt(eles[0]);
		Probs probs = new Probs();
		probs.totalProb = Integer.parseInt(eles[1]);
		probs.totalDenominator = Integer.parseInt(eles[2]);
		probs.specificProb = Integer.parseInt(eles[3]);
		probs.specificDenominator = Integer.parseInt(eles[4]);
		priorProbability.put(prop, probs);
	}
	
	public void populatePosteriorCounts(String[] eles, HashMap<Integer, HashMap<Integer, Probs>> posteriorProbability) {
		// find probability of : p(a|b)
		int b = Integer.parseInt(eles[0]);
		int a = Integer.parseInt(eles[1]);
		Probs probs = new Probs();
		probs.totalProb = Integer.parseInt(eles[2]);
		probs.totalDenominator = Integer.parseInt(eles[3]);
		probs.specificProb = Integer.parseInt(eles[4]);
		probs.specificDenominator = Integer.parseInt(eles[5]);
		HashMap<Integer, Probs> condprob;
		if(posteriorProbability.containsKey(b)) {
			condprob = posteriorProbability.get(b);
		}
		else {
			condprob = new HashMap<Integer, Probs>();
		}
		condprob.put(a, probs);
		posteriorProbability.put(b, condprob);
	}
	
	/*public void populatePriorProbability(String[] eles, HashMap<Integer, Probs> priorProbability) {
		int prop = Integer.parseInt(eles[0]);
		Probs probs = new Probs();
		probs.totalProb = Double.parseDouble(eles[1]);
		probs.specificProb = Double.parseDouble(eles[2]);
		priorProbability.put(prop, probs);
	}
	
	public void populatePosteriorProbability(String[] eles, HashMap<Integer, HashMap<Integer, Probs>> posteriorProbability) {
		// find probability of : p(a|b)
		int b = Integer.parseInt(eles[0]);
		int a = Integer.parseInt(eles[1]);
		Probs probs = new Probs();
		probs.totalProb = Double.parseDouble(eles[2]);
		probs.specificProb = Double.parseDouble(eles[3]);
		HashMap<Integer, Probs> condprob;
		if(posteriorProbability.containsKey(b)) {
			condprob = posteriorProbability.get(b);
		}
		else {
			condprob = new HashMap<Integer, Probs>();
		}
		condprob.put(a, probs);
		posteriorProbability.put(b, condprob);
	}*/
}
