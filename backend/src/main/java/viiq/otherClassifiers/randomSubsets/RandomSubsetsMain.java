package viiq.otherClassifiers.randomSubsets;

import viiq.graphQuerySuggestionMain.Config;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class RandomSubsetsMain {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	ArrayList<logDetails> userLog = new ArrayList<logDetails>();
	
	public RandomSubsetsMain(Config conf) {
		this.conf = conf;
	}
	
	/**
	 * The various values of randomSubsetStrategy are:
	 *  # 0: Choose "QuerySuggestion.RandomForest.NumberOfRandomSubsets" number of random subsets
		# 1: The number of subsets is 1, the subset is always the entire history.
		# 2: The number of subsets is equal to the size of history, Choose exactly one entry of the history in every iteration
	 */
	int randomSubsetStrategy;
	double numOfRandomSubsets;
	int logSizeThreshold;
	int weightedAvgFormula;
	
	int totalFullCnt = 0;
	int totalSpecificCnt = 0;
	
	/**
	 * This method reads in the entire log and stores it.. This is rudimentary for now. will surely require smarter index in future.
	 */
	public void learnModel() {
		randomSubsetStrategy = Integer.parseInt(conf.getProp(PropertyKeys.randomSubsetStrategy));
		numOfRandomSubsets = Double.parseDouble(conf.getProp(PropertyKeys.numberOfRandomSubsets));
		logSizeThreshold = Integer.parseInt(conf.getProp(PropertyKeys.userLogSizeThreshold));
		weightedAvgFormula = Integer.parseInt(conf.getProp(PropertyKeys.weightedAverage));
		String trainingDataFilePath = conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile);
		readUserLogToMemory(trainingDataFilePath);
	}
	
	private void readUserLogToMemory(String trainingDataFilePath) {
		try{
			FileReader fr = new FileReader(trainingDataFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				logDetails ld = new logDetails();
				HashSet<Integer> edges = new HashSet<Integer>();
				String[] propsCount = line.split(",");
				int newEdgeIndex = propsCount.length - 2 - 1;
				for(int i=0; i<= newEdgeIndex; i++) {
					edges.add(Integer.parseInt(propsCount[i]));
				}
				ld.edges = edges;
				ld.totalCount = Integer.parseInt(propsCount[propsCount.length - 2]);
				ld.specificCount = Integer.parseInt(propsCount[propsCount.length - 1]);
				totalFullCnt += ld.totalCount;
				totalSpecificCnt += ld.specificCount;
				userLog.add(ld);
			}
			br.close();
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
		// data structure to hold the number of random subsets for which a candidate edge was the best choice in.
		HashMap<Integer, ArrayList<EdgeSupport>> candidateEdgeSupportCount;
		if(history.isEmpty()) {
			bestEdge = getLargestSupportCandidateFromFullLog(candidateEdges);
		}
		else {
			candidateEdgeSupportCount = new HashMap<Integer, ArrayList<EdgeSupport>>();
			//HashSet<MutableString> seenSubsets = new HashSet<MutableString>();
			int numsubs = 0;
			double numberOfSubsets = getNumberOfRandomSubsets(history);
			for(int i=0; i<numberOfSubsets; i++) {
				ArrayList<Integer> randomSubset = getRandomSubset(history, i);
				//ArrayList<Long> randomSubset = getRandomSubset(history);
				/*MutableString rss = new MutableString(randomSubset.toString());
				if(seenSubsets.contains(rss))
					continue;
				seenSubsets.add(rss);
				numsubs++;*/
				ArrayList<logDetails> userLogSubset = new ArrayList<logDetails>();
				int denominatorCount = getSubsetUserLog(randomSubset, userLogSubset);
				populateCandidateSupport(userLogSubset, candidateEdges, candidateEdgeSupportCount, denominatorCount);
			}
			logger.debug("number of subsets = " + numsubs);
			bestEdge = getBestCandidate(candidateEdgeSupportCount);
		}
		return bestEdge;
	}
	
	private ArrayList<Integer> getRandomSubset(ArrayList<Integer> history, int subsetNumber) {
		ArrayList<Integer> randomSubset = new ArrayList<Integer>();
		if(randomSubsetStrategy == 0) {
			randomSubset = getRandomSubset(history);
		}
		else if(randomSubsetStrategy == 1) {
			// choose all entries in the history as the subset
			for(int i=0; i<history.size(); i++) {
				randomSubset.add(history.get(i));
			}
		}
		else if(randomSubsetStrategy == 2) {
			// In this scenario, the number of random subsets to be chosen MUST be equal to the size of the history. So it should NOT fail here.
			randomSubset.add(history.get(subsetNumber));
		}
		return randomSubset;
	}
	
	private double getNumberOfRandomSubsets(ArrayList<Integer> history) {
		double numSubsets = numOfRandomSubsets;
		if(randomSubsetStrategy == 0) {
			numSubsets = numOfRandomSubsets;
		}
		else if(randomSubsetStrategy == 1) {
			numSubsets = 1;
		}
		else if(randomSubsetStrategy == 2) {
			numSubsets = history.size();
		}
		return numSubsets;
	}
	
	/**
	 * This method assumes the user log to use is the entire user log. Just count thru the entire user log instead of a subset of it.
	 * @param candidateEdges
	 * @return
	 */
	private int getLargestSupportCandidateFromFullLog(HashSet<Integer> candidateEdges) {
		HashMap<Integer, Integer> candEdges = new HashMap<Integer, Integer>();
		for(logDetails entry : userLog) {
			Iterator<Integer> iter = candidateEdges.iterator();
			while(iter.hasNext()) {
				int e = iter.next();
				if(entry.edges.contains(e)) {
					// TODO: check if this is what you want to add in the future too.
					int cnt = entry.specificCount;
					if(candEdges.containsKey(e)) {
						cnt += candEdges.get(e);
					}
					candEdges.put(e, cnt);
				}
			}
		}
		int bestEdge = 0;
		int bestScore = 0;
		Iterator<Integer> iter = candEdges.keySet().iterator();
		while(iter.hasNext()) {
			int e = iter.next();
			if(candEdges.get(e) > bestScore) {
				bestScore = candEdges.get(e);
				bestEdge = e;
			}
		}
		return bestEdge;
	}
	
	/**
	 * This method takes in the edges seen in history, selects a random subset of these edges and returns.
	 * This random subset will be used to count support and select next candidate edge.
	 * @param history
	 * @return
	 */
	private ArrayList<Integer> getRandomSubset(ArrayList<Integer> history) {
		ArrayList<Integer> randomSubset = new ArrayList<Integer>();
		Random rand = new Random();
		for(int i=0; i<history.size(); i++) {
			if(rand.nextDouble() > 0.5) {
				// sample each entry in history uniformly. over several calls to this method, we should have a uniform sample of random subsets.
				randomSubset.add(history.get(i));
			}
			/*if(history.get(i) > 0) {
				if(rand.nextDouble() > 0.1)
					randomSubset.add(history.get(i));
			}
			else if(rand.nextDouble() > 0.5) {
				// sample each entry in history uniformly. over several calls to this method, we should have a uniform sample of random subsets.
				randomSubset.add(history.get(i));
			}*/
		}
		return randomSubset;
	}
	
	/**
	 * Get the user log entries associated with the random subset of edges..
	 * @param randomSubset
	 * @return
	 */
	private int getSubsetUserLog(ArrayList<Integer> randomSubset, ArrayList<logDetails> userLogSubset) {
		int totalDenominatorCount = 0;
		for(logDetails ld : userLog) {
			boolean contains = true;
			for(int i=0; i<randomSubset.size(); i++) {
				if(!ld.edges.contains(randomSubset.get(i))) {
					contains = false;
					break;
				}
			}
			if(contains) {
				userLogSubset.add(ld);
				totalDenominatorCount += ld.specificCount;
			}
		}
		return totalDenominatorCount;
	}
	
	/**
	 * Method to count the support for each candidate in this subset of user log.
	 * @param userLogSubset
	 * @param candidateEdges
	 * @param candidateEdgeSupportCount
	 */
	private void populateCandidateSupport(ArrayList<logDetails> userLogSubset, HashSet<Integer> candidateEdges, 
			HashMap<Integer, ArrayList<EdgeSupport>> candidateEdgeSupportCount, int denominator) {
		HashSet<Integer> seenCands = new HashSet<Integer>();
		for(logDetails ld : userLogSubset) {
			Iterator<Integer> iter = candidateEdges.iterator();
			while(iter.hasNext()) {
				int e = iter.next();
				if(ld.edges.contains(e)) {
					if(seenCands.contains(e)) {
						// this edge was seen in one of the previous log entries of this random subset.
						int ind = candidateEdgeSupportCount.get(e).size() - 1;
						candidateEdgeSupportCount.get(e).get(ind).supportCount += ld.specificCount;
					}
					else {
						EdgeSupport es = new EdgeSupport();
						es.supportCount += ld.specificCount;
						es.subsetUserLogSize = denominator;
						if(!candidateEdgeSupportCount.containsKey(e)) {
							ArrayList<EdgeSupport> esa = new ArrayList<EdgeSupport>();
							esa.add(es);
							candidateEdgeSupportCount.put(e, esa);
						}
						else {
							candidateEdgeSupportCount.get(e).add(es);
						}
						seenCands.add(e);
					}
				}
			}
		}
	}
	
	/**
	 * We have the count of various candidate edges. Compute the weighted average and choose the best option.
	 * @param candidateEdgeSupportCount
	 * @return
	 */
	private int getBestCandidate(HashMap<Integer, ArrayList<EdgeSupport>> candidateEdgeSupportCount) {
		int bestEdge = 0;
		double bestScore = 0.0;
		double alpha = 0.5;
		//RandomForestHelper rfh = new RandomForestHelper();
		Iterator<Integer> iter = candidateEdgeSupportCount.keySet().iterator();
		//System.out.println("--------------------------------");
		while(iter.hasNext()) {
			int edge = iter.next();
			ArrayList<EdgeSupport> randoms = candidateEdgeSupportCount.get(edge);
			double probabilitySum = 0.0;
			//System.out.println("size = " + randoms.size());
			for(EdgeSupport es : randoms) {
				probabilitySum += es.supportCount/es.subsetUserLogSize;
			}
			double score = alpha*getRandomSetSupportWeight(probabilitySum) + 
					(1-alpha)*getNumberOfRandomSetSupportWeight(probabilitySum, randoms.size());
			if(bestScore < score) {
				bestScore = score;
				bestEdge = edge;
			}
		}
		return bestEdge;
	}
	
	private double getRandomSetSupportWeight(double probabilitySum) {
		return probabilitySum/numOfRandomSubsets;
	}

	private double getNumberOfRandomSetSupportWeight(double probabilitySum, int numberOfRandomSubsets) {
		return probabilitySum * (numberOfRandomSubsets/numOfRandomSubsets);
	}
}

final class EdgeSupport {
	// this could be heavy.. if we decide on the exact weighted average, this can be made lighter accordingly.
	int supportCount = 0;
	double subsetUserLogSize;
	double probability;
}

final class logDetails {
	HashSet<Integer> edges;
	int totalCount;
	int specificCount;
}
