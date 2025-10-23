package viiq.decisionForest;

import viiq.graphQuerySuggestionMain.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

import viiq.utils.PropertyKeys;

import viiq.commons.LogDetails;

public class LearnDecisionForest {

	/**
	 * This was used for RANDOM FOREST. Not sure if these are applicable parameters for decision forest too. So keeping it here for now.
	 * The various values of randomSubsetStrategy are:
	 *  # 0: Choose "QuerySuggestion.RandomForest.NumberOfRandomSubsets" number of random subsets
		# 1: The number of subsets is 1, the subset is always the entire history.
		# 2: The number of subsets is equal to the size of history, Choose exactly one entry of the history in every iteration
	 */
	static int randomSubsetStrategy;
	public int getRandomSubsetStrategy() {
		return randomSubsetStrategy;
	}

	/*public void setRandomSubsetStrategy(int randomSubsetStrategy) {
		this.randomSubsetStrategy = randomSubsetStrategy;
	}*/

	public double getNumOfRandomSubsets() {
		return numOfRandomSubsets;
	}

	/*public void setNumOfRandomSubsets(double numOfRandomSubsets) {
		this.numOfRandomSubsets = numOfRandomSubsets;
	}*/

	public int getLogSizeThreshold() {
		return logSizeThreshold;
	}

	/*public void setLogSizeThreshold(int logSizeThreshold) {
		this.logSizeThreshold = logSizeThreshold;
	}*/

	public int getWeightedAvgFormula() {
		return weightedAvgFormula;
	}

	/*public void setWeightedAvgFormula(int weightedAvgFormula) {
		this.weightedAvgFormula = weightedAvgFormula;
	}*/

	public int getTotalFullCnt() {
		return totalFullCnt;
	}

	/*public void setTotalFullCnt(int totalFullCnt) {
		this.totalFullCnt = totalFullCnt;
	}*/

	public int getTotalSpecificCnt() {
		return totalSpecificCnt;
	}

	/*public void setTotalSpecificCnt(int totalSpecificCnt) {
		this.totalSpecificCnt = totalSpecificCnt;
	}*/

	public ArrayList<LogDetails> getUserLog() {
		return userLog;
	}

	/*public void setUserLog(ArrayList<LogDetails> userLog) {
		this.userLog = userLog;
	}*/

	public HashMap<Integer, HashSet<Integer>> getPositiveEdgeInvertedIndex() {
		return positiveEdgeInvertedIndex;
	}

	/*public void setPositiveEdgeInvertedIndex(
			HashMap<Integer, HashSet<Integer>> positiveEdgeInvertedIndex) {
		this.positiveEdgeInvertedIndex = positiveEdgeInvertedIndex;
	}*/

	public HashMap<Integer, HashSet<Integer>> getNegativeEdgeInvertedIndex() {
		return negativeEdgeInvertedIndex;
	}

	public HashMap<Integer, Integer> getUserLogEdgeCount() {
		return userLogEdgeCount;
	}

	/*public void setNegativeEdgeInvertedIndex(
			HashMap<Integer, HashSet<Integer>> negativeEdgeInvertedIndex) {
		this.negativeEdgeInvertedIndex = negativeEdgeInvertedIndex;
	}*/

	public boolean getIsWeightedConf() {
		return isWeightedConf;
	}

	public int getTopkRules() {
		return topkRules;
	}

	public int getCountCondition() {
		return countCondition;
	}
	
	public int getHistoryUpdate() {
		return historyUpdate;
	}

	public double getNumeratorPower() {
		return numeratorPower;
	}
	

	static double numOfRandomSubsets;
	static int logSizeThreshold;
	static int weightedAvgFormula;

	//parameters for algorithm framework
	static boolean isWeightedConf;
	static int topkRules;
	static int countCondition;
	static int historyUpdate;
	static double numeratorPower;

	static int totalFullCnt = 0;
	static int totalSpecificCnt = 0;

	// the entire log file. The index represents the line number in the log file.
	static ArrayList<LogDetails> userLog = new ArrayList<LogDetails>();
	// inverted index for each edge and its occurrence. keep record of both positive and negative log entries for each edge.
	// key: edge ID
	// value: set of line numbers from the user log
	static HashMap<Integer, HashSet<Integer>> positiveEdgeInvertedIndex = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, HashSet<Integer>> negativeEdgeInvertedIndex = new HashMap<Integer, HashSet<Integer>>();

	//key: edge ID
	//value: total occurrence an edge in all sessions
	static HashMap<Integer, Integer> userLogEdgeCount = new HashMap<Integer, Integer>();

	Config conf;
	public LearnDecisionForest(Config config) {
		this.conf = config;
	}

	/**
	 * This method reads in the entire log and stores it.. This is rudimentary for now. will surely require smarter index in future.
	 */
	//used for Orion UI
	public void learnModel(int n, boolean w, int k, int c, int h) {
		randomSubsetStrategy = Integer.parseInt(conf.getProp(PropertyKeys.randomSubsetStrategy));
		
		logSizeThreshold = Integer.parseInt(conf.getProp(PropertyKeys.userLogSizeThreshold));
		weightedAvgFormula = Integer.parseInt(conf.getProp(PropertyKeys.weightedAverage));
		numeratorPower=Double.parseDouble(conf.getProp(PropertyKeys.numeratorPower));

		// numOfRandomSubsets = Double.parseDouble(conf.getProp(PropertyKeys.numberOfRandomSubsets));
		// isWeightedConf = Boolean.parseBoolean(conf.getProp(PropertyKeys.isWeightedConf));
		// topkRules = Integer.parseInt(conf.getProp(PropertyKeys.topkRules));
		// countCondition = Integer.parseInt(conf.getProp(PropertyKeys.countCondition));

		numOfRandomSubsets = n;
		isWeightedConf = w;
		topkRules = k;
		countCondition = c;
		historyUpdate = h;

		System.out.println("isWeightedConf = "+isWeightedConf);
		System.out.println("topkRules = "+topkRules);
		System.out.println("countCondition = "+countCondition);
		System.out.println("N = "+numOfRandomSubsets);
		System.out.println("historyUpdate = "+historyUpdate);

		String trainingDataFilePath = conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile);
		readUserLogToMemory(trainingDataFilePath);
	}


	private void readUserLogToMemory(String trainingDataFilePath) {
		try{
			FileReader fr = new FileReader(trainingDataFilePath);
			BufferedReader br = new BufferedReader(fr);
			int maxLogLoad = Integer.parseInt(conf.getProp(PropertyKeys.maxLogLoadIntoMemory));
			String line = null;
			int lineNumber = 0;

			// ArrayList<Integer> lineIndices = new ArrayList<Integer>();
			// for(int i = 0; i < Integer.parseInt(conf.getProp(PropertyKeys.trainingDataSize)); i++) {
			// 	lineIndices.add(i);
			// }
			// Collections.shuffle(lineIndices);
			// HashSet<Integer> randomIndices = new HashSet<Integer>();
			// for(int i = 0; i < maxLogLoad; i++) {
			// 	randomIndices.add(lineIndices.get(i));
			// }

			while((line = br.readLine()) != null) {
				
				// if(!randomIndices.contains(lineNumber)) {
				// 	lineNumber++;
				// 	continue;
				// }

				LogDetails ld = new LogDetails();
				HashSet<Integer> edges = new HashSet<Integer>();
				String[] propsCount = line.split(",");
				int newEdgeIndex = propsCount.length - 2 - 1;
				for(int i=0; i<= newEdgeIndex; i++) {
					int e = Integer.parseInt(propsCount[i]);
					edges.add(e);
					if(e > 0) {
						addEdgeToInvertedIndex(positiveEdgeInvertedIndex, e, lineNumber);
					}
					else {
						addEdgeToInvertedIndex(negativeEdgeInvertedIndex, e, lineNumber);
					}
					if(userLogEdgeCount.containsKey(e)) {
						int cnt = userLogEdgeCount.get(e) + Integer.parseInt(propsCount[propsCount.length - 1]);
						userLogEdgeCount.put(e, cnt);
					} else {
						userLogEdgeCount.put(e, 0);
					}
				}
				ld.setEdges(edges);
				ld.setTotalCount(Integer.parseInt(propsCount[propsCount.length - 2]));
				ld.setSpecificCount(Integer.parseInt(propsCount[propsCount.length - 1]));
				totalFullCnt += ld.getTotalCount();
				totalSpecificCnt += ld.getSpecificCount();
				userLog.add(ld);
				lineNumber++;
				if(lineNumber > maxLogLoad) break;
				//if(userLog.size() > maxLogLoad) break;
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

	private void addEdgeToInvertedIndex(HashMap<Integer, HashSet<Integer>> edgeInvertedIndex, int edge, int lineNumber) {
		if(edgeInvertedIndex.containsKey(edge)) {
			edgeInvertedIndex.get(edge).add(lineNumber);
		}
		else {
			HashSet<Integer> lines = new HashSet<Integer>();
			lines.add(lineNumber);
			edgeInvertedIndex.put(edge, lines);
		}
	}
}
