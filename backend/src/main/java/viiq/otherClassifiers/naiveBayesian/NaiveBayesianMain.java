package viiq.otherClassifiers.naiveBayesian;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class NaiveBayesianMain {
	Config conf = null;
//	final Logger logger = Logger.getLogger(getClass());
	
	HashMap<Integer, HashMap<Integer, Probs>> posteriorProbability = new HashMap<Integer, HashMap<Integer,Probs>>();
	HashMap<Integer, Probs> priorProbability = new HashMap<Integer, Probs>();
	
	// these variables can be used as "m" in M-estimate. p(a|b) = (n_c + mp)/(n + m), where "m" is one of the two below and p=1/m.
	int totalFullCount = 0;
	int totalSpecificCount = 0;
	
	public NaiveBayesianMain(Config conf) {
		this.conf = conf;
	}
	
	/**
	 * load the various prior and conditional probabilities obtained from training data.
	 */
	public void learnModel() {
		try {
			NaiveBayesianHelper nbh = new NaiveBayesianHelper();
			FileReader fr = new FileReader(conf.getOutputFilePath(PropertyKeys.bayesianPriorProbabilities));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			// the first line has the total count and specific count values. This can be used as "m" in M-estimate.
			line = br.readLine();
			String[] l = line.split(",");
			totalFullCount = Integer.parseInt(l[0]);
			totalSpecificCount = Integer.parseInt(l[1]);
			
			while((line = br.readLine()) != null) {
				String[] eles = line.split(",");
				if(eles.length == 5) {
					// there is only one edge, and 4 counts in this line.
					nbh.populatePriorCounts(eles, priorProbability);
				}
				else if(eles.length == 6){
					// there are 2 edges and 4 counts in this line.
					// this has the value p(a|b), with the file storing the following details:
					// b, a, total_prob, totalDenominator, specific_prob, specificDenominator
					nbh.populatePosteriorCounts(eles, posteriorProbability);
				}
			}
			br.close();
			System.out.println("Done learning the bayesian model");
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * MUST learnModel() before calling this method.
	 * finds the edge with the highest log probability among the candidate edges, given the history.
	 * if there is no history, prior probabilities of each candidate is returned back.
	 * @param history
	 * @param candidateEdges
	 * @return
	 */
	public int findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		int bestEdge = 0;
		double bestLogProb = -100000.0;
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext()) {
			int edge = iter.next();
			double logProb = getBayesProbabilityTotal(history, edge);
		//	System.out.println(edge + " : " + logProb);
			if(logProb > bestLogProb) {
				bestLogProb = logProb;
				bestEdge = edge;
			}
		}
		return bestEdge;
	}
	
	private double getBayesProbabilityTotal(ArrayList<Integer> history, int edge) {
		double logProb = 0.0;
		double logPriorOfEdge = 0.0;
		if(!priorProbability.containsKey(edge)) {
			logPriorOfEdge = Math.log(1.0/(totalFullCount + totalFullCount));
		}
		else {
			logPriorOfEdge = getLogValue(priorProbability.get(edge).totalProb, priorProbability.get(edge).totalDenominator, totalFullCount);
		}
		logProb += logPriorOfEdge;
		if(posteriorProbability.containsKey(edge)) {
			HashMap<Integer, Probs> condProbs = posteriorProbability.get(edge);
			// if history is empty, only the above value is returned. else sum it up.
			for(int e : history) {
				if(condProbs.containsKey(e)) {
					logProb += getLogValue(condProbs.get(e).totalProb, condProbs.get(e).totalDenominator, totalFullCount);
				}
				else {
					// give in the default non-zero probability value.
					logProb += Math.log(1.0/(totalFullCount + totalFullCount));
				}
			}
		}
		return logProb;
	}
	
	private double getBayesProbabilitySpecific(ArrayList<Integer> history, int edge) {
		double logProb = 0.0;
		double logPriorOfEdge = 0.0;
		if(!priorProbability.containsKey(edge)) {
			logPriorOfEdge = Math.log(1.0/(totalSpecificCount + totalSpecificCount));
		}
		else {
			logPriorOfEdge = getLogValue(priorProbability.get(edge).specificProb, priorProbability.get(edge).specificDenominator, totalSpecificCount);
		}
		logProb += logPriorOfEdge;
		HashMap<Integer, Probs> condProbs;
		if(posteriorProbability.containsKey(edge)) {
			condProbs = posteriorProbability.get(edge);
			// if history is empty, only the above value is returned. else sum it up.
			for(int e : history) {
				if(condProbs.containsKey(e)) {
					logProb += getLogValue(condProbs.get(e).specificProb, condProbs.get(e).specificDenominator, totalSpecificCount);
				}
				else {
					// give in the default non-zero probability value.
					logProb += Math.log(1.0/(totalSpecificCount + totalSpecificCount));
				}
			}
		}
		return logProb;
	}
	
	private double getLogValue(int num, int denom, int m) {
		double numerator = (num + 1)*1.0;
		return Math.log((numerator)/(denom + m));
	}
	
}

final class Probs {
	int totalProb;
	int specificProb;
	int totalDenominator;
	int specificDenominator;
}
