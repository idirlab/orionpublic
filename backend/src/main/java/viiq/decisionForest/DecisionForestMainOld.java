package viiq.decisionForest;

import viiq.graphQuerySuggestionMain.Config;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;
//import viiq.clientServer.server.LearnDecisionForest;

import viiq.commons.LogDetails;
import viiq.commons.SubsetInfo;
import viiq.commons.CandidateEdgeScore;

import viiq.clientServer.server.LoadData;
import viiq.backendHelper.SpringClientHelper;

public class DecisionForestMainOld {

	Config conf = null;
//	final Logger logger = Logger.getLogger(getClass());
	// the entire log file. The index represents the line number in the log file.
	ArrayList<LogDetails> userLog = new ArrayList<LogDetails>();
	// inverted index for each edge and its occurrence. keep record of both positive and negative log entries for each edge.
	// key: edge ID
	// value: set of line numbers from the user log
	HashMap<Integer, HashSet<Integer>> positiveEdgeInvertedIndex = new HashMap<Integer, HashSet<Integer>>();
	HashMap<Integer, HashSet<Integer>> negativeEdgeInvertedIndex = new HashMap<Integer, HashSet<Integer>>();
	//key: edge ID
	//value: total occurrence an edge in all sessions
	HashMap<Integer, Integer> userLogEdgeCount = new HashMap<Integer, Integer>();

	//for all decision path and candidate edges store the support count
	HashMap<MutableString, HashMap<Integer, EdgeSupport>> candidateEdgeSupportCount = new HashMap<MutableString, HashMap<Integer, EdgeSupport>>();
	HashMap<MutableString, SubsetInfo> seenSubsets = new HashMap<MutableString, SubsetInfo>();

	HashMap<MutableString, HashSet<Integer>> logLines = new HashMap<MutableString, HashSet<Integer>>();

	//will delete later
	HashSet<MutableString> allDecisionPathsTemp= new HashSet<MutableString>();

	LoadData ldlm = new LoadData();

	/**
	 * This was used for RANDOM FOREST. Not sure if these are applicable parameters for decision forest too. So keeping it here for now.
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

	//parameters for algorithm framework
	boolean isWeightedConf;
	boolean isRandRules;
	int topkRules;
	int countCondition;

	public DecisionForestMainOld(Config conf) {
		this.conf = conf;
	}

	public DecisionForestMainOld(LearnDecisionForest df) {
		this.randomSubsetStrategy = df.getRandomSubsetStrategy();
		this.numOfRandomSubsets = df.getNumOfRandomSubsets();
		this.logSizeThreshold = df.getLogSizeThreshold();
		this.weightedAvgFormula = df.getWeightedAvgFormula();
		this.totalFullCnt = df.getTotalFullCnt();
		this.totalSpecificCnt = df.getTotalSpecificCnt();

		//this.isRandRules = df.getIsRandRules();
		this.isWeightedConf = df.getIsWeightedConf();
		this.topkRules = df.getTopkRules();
		this.countCondition = df.getCountCondition();

		this.userLog = df.getUserLog();
		this.positiveEdgeInvertedIndex = df.getPositiveEdgeInvertedIndex();
		this.negativeEdgeInvertedIndex = df.getNegativeEdgeInvertedIndex();
		this.userLogEdgeCount = df.getUserLogEdgeCount();
	}

	/**
	 * This method reads in the entire log and stores it.. This is rudimentary for now. will surely require smarter index in future.
	 */
	public void learnModel() {
		randomSubsetStrategy = Integer.parseInt(conf.getProp(PropertyKeys.randomSubsetStrategy));
		numOfRandomSubsets = Double.parseDouble(conf.getProp(PropertyKeys.numberOfRandomSubsets));
		logSizeThreshold = Integer.parseInt(conf.getProp(PropertyKeys.userLogSizeThreshold));
		weightedAvgFormula = Integer.parseInt(conf.getProp(PropertyKeys.weightedAverage));

		isWeightedConf = Boolean.parseBoolean(conf.getProp(PropertyKeys.isWeightedConf));
		isRandRules = Boolean.parseBoolean(conf.getProp(PropertyKeys.isRandRules));
		topkRules = Integer.parseInt(conf.getProp(PropertyKeys.topkRules));
		countCondition = Integer.parseInt(conf.getProp(PropertyKeys.countCondition));

		/******* print execution log *********/
		//only for debugging for experiments
		System.out.println("Load edge labels");
		ldlm.loadEdgeLabels(conf.getInputFilePath(PropertyKeys.propertiesLangEn));
		System.out.println("Load node labels");
		ldlm.loadAllNodeLabels(conf.getInputFilePath(PropertyKeys.domainLangEn), conf.getInputFilePath(PropertyKeys.typeLangEn), null);

		System.out.println("isWeightedConf = "+isWeightedConf+", isRandRules = "+isRandRules+", topkRules = "+topkRules+", countCondition = "+countCondition);
		if(isRandRules) System.out.println("Number of Random Subsets = "+numOfRandomSubsets);

		int isProcessingDataGraphFlag = Integer.parseInt(conf.getProp(PropertyKeys.dataGraphBasedQueryLog));
		String trainingDataFilePath;
		if(isProcessingDataGraphFlag == 1) {
			trainingDataFilePath = conf.getOutputFilePath(PropertyKeys.datagraphTrainigDataWithIDFile);
			System.out.println("Processing data graph based query log: " + trainingDataFilePath);
		} else {
			trainingDataFilePath = conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile);
			System.out.println("Processing WIKI based query log: " + trainingDataFilePath);
		}
		readUserLogToMemory(trainingDataFilePath);
	}

	private void readUserLogToMemory(String trainingDataFilePath) {
		try{
			FileReader fr = new FileReader(trainingDataFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			int lineNumber = 0;
			while((line = br.readLine()) != null) {
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
				if(lineNumber > 100000) break;
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

	public void clearCache() {
		seenSubsets.clear();
		candidateEdgeSupportCount.clear();
		logLines.clear();
	}

	/**
	 * MUST learnModel() before calling this method.
	 * ranks all the candidate edges, given the history.
	 * if there is no history, prior probabilities of each candidate is returned back.
	 * @param history
	 * @param candidateEdges
	 * @return
	 */
	public ArrayList<CandidateEdgeScore> rankCandidateEdges(HashSet<Integer> history, HashSet<Integer> candidateEdges) {
		ArrayList<CandidateEdgeScore> rankedList = new ArrayList<CandidateEdgeScore>();
		// data structure to hold the number of random subsets for which a candidate edge was the best choice in.
		
		/******* print execution log *********/
		ArrayList<String> historyLabel = new ArrayList<String>();
		for(int e: history) historyLabel.add(ldlm.getNodeLabelIndex().get(e) == null ? ldlm.getEdgeLabelIndex().get(e) : ldlm.getNodeLabelIndex().get(e));
		System.out.println("\nCurrent query graph = "+historyLabel);

		if(history.isEmpty()) {
			rankedList = getLargestSupportCandidateFromFullLog(candidateEdges);
		}
		else {
			double numberOfSubsets = 0.0;
			if(isRandRules) {
				numberOfSubsets = getNumberOfRandomSubsets(history);
			} else {
				numberOfSubsets = (1<<history.size())-1;
			}

			//ArrayList<LogDetails> userLogSubset = new ArrayList<LogDetails>();

			ArrayList<MutableString> allDecisionPaths = new ArrayList<MutableString>();

			//System.out.println(history);

			// long total1 = 0;
			// long total2 = 0;

			int maxIterations = 15;
			int iterationCnt = 1;
			System.out.println("Antecedents:");
			while(allDecisionPaths.size() < numberOfSubsets) {
				//long startTime = System.nanoTime();
				if(isRandRules && (iterationCnt > maxIterations)) break;

				ArrayList<Integer> historyCopy = new ArrayList<Integer>(history);
				//HashSet<Integer> logLines = new HashSet<Integer>();
				MutableString decisionPath = null;
				if(isRandRules) {
					long startTime = java.lang.System.currentTimeMillis();
					decisionPath = getRandomDecisionPath(historyCopy);
					//System.out.println("Time took to generate subset#"+i+" = "+(java.lang.System.currentTimeMillis()-startTime));
					//total1 += (java.lang.System.currentTimeMillis()-startTime);
				} else {

					//decisionPath = getNextDecisionPath(iterationCnt, historyCopy);
					
				}

				iterationCnt++;

				if(decisionPath.length() == 0 || allDecisionPaths.contains(decisionPath)) continue;			
				allDecisionPaths.add(decisionPath);
				allDecisionPathsTemp.add(decisionPath); //keeping a global copy for debug purpose will delete later

				/******* print execution log *********/
				String decisionPathLabel = "";
				for(String e: decisionPath.toString().split(",")) {
					if(e.equals("")) continue;
					String label = ldlm.getNodeLabelIndex().get(Integer.parseInt(e));
					if(label == null) label = ldlm.getEdgeLabelIndex().get(Integer.parseInt(e));
					decisionPathLabel += label;
					decisionPathLabel += ", ";
				}

				//generate a random decision path and get its count and line numbers
				//long startTime = java.lang.System.currentTimeMillis();
				//System.out.println(decisionPath);
				getSubsetUserLog(decisionPath, seenSubsets);

				/******* print execution log *********/
				System.out.println("	{"+decisionPathLabel+"}, supportCount = "+seenSubsets.get(decisionPath).denominatorCount);
				
				//System.out.println("Time taken for above getSubsetUserLog = "+(java.lang.System.currentTimeMillis()-startTime));
				populateCandidateSupport(decisionPath, seenSubsets.get(decisionPath).lineIDs, candidateEdges, candidateEdgeSupportCount, seenSubsets.get(decisionPath).denominatorCount);
				// total2 += (java.lang.System.currentTimeMillis()-startTime);
				// System.out.println("Time taken for above decisionpath = "+(java.lang.System.currentTimeMillis()-startTime));
				//	System.out.println("done candidate support");
				
				//System.out.println("decision path = "+decisionPath);
				//System.out.println(candidateEdgeSupportCount.get(decisionPath));
			}
			//System.out.println("Total time for subset generation = "+total1);
			//System.out.println("Total time for support calc = "+total2);
			//System.out.println(allDecisionPaths);
			rankedList = getBestCandidate(candidateEdges, allDecisionPaths, candidateEdgeSupportCount, history);
			//System.out.println("Done ranking edges");
		}
		return rankedList;
	}

	// /**
	//  * MUST learnModel() before calling this method.
	//  * finds the edge with the highest log probability among the candidate edges, given the history.
	//  * if there is no history, prior probabilities of each candidate is returned back.
	//  * @param history
	//  * @param candidateEdges
	//  * @return
	//  */
	// public CandidateEdgeScore findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges, HashMap<Integer, Double> decayFactor) {
	// 	ArrayList<CandidateEdgeScore> rankedList = rankCandidateEdges(history, candidateEdges);
	// 	int bestEdge = -1;
	// 	double maxScore = -1.0;
	// 	System.out.println("Non-zero score candidate edges:");

	// 	double sumDf = 0.0;

	// 	for(CandidateEdgeScore ce : rankedList) {
	// 		if(decayFactor.containsKey(ce.edge)) {
	// 			ce.score *= decayFactor.get(ce.edge);
	// 		} else {
	// 			decayFactor.put(ce.edge, 1.0);
	// 		}

	// 		sumDf += decayFactor.get(ce.edge);

	// 		if(ce.score > maxScore) {
	// 			bestEdge = ce.edge;
	// 			maxScore = ce.score;
	// 		}

	// 		/******* print execution log *********/
	// 		if(ce.score > 0.0) System.out.println("		"+ldlm.getEdgeLabelIndex().get(ce.edge));
	// 	}

	// 	/******* print execution log *********/
	// 	if(maxScore > 0) {
	// 		System.out.format("Best edge = "+ldlm.getEdgeLabelIndex().get(bestEdge)+", score = %.3f, decay factor = %.3f\n", maxScore, decayFactor.get(bestEdge));
	// 		System.out.println("Antecedents of rules having non-zero scores where the best edge is the consequent:");
	// 		for(MutableString decisionPath : candidateEdgeSupportCount.keySet()) {
	// 			HashMap<Integer, EdgeSupport> ces = candidateEdgeSupportCount.get(decisionPath);
	// 			for(int edge : ces.keySet()) {
	// 				double num = ces.get(edge).supportCount;
	// 				double denom = ces.get(edge).subsetUserLogSize;
	// 				if(denom > 0.0 && num > 0.0 && edge==bestEdge && allDecisionPathsTemp.contains(decisionPath)) {
						
	// 					String decisionPathLabel = "";
	// 					for(String e: decisionPath.toString().split(",")) {
	// 						if(e.equals("")) continue;
	// 						String label = ldlm.getNodeLabelIndex().get(Integer.parseInt(e));
	// 						if(label == null) label = ldlm.getEdgeLabelIndex().get(Integer.parseInt(e));
	// 						decisionPathLabel += label;
	// 						decisionPathLabel += ", ";
	// 					}
	// 					System.out.format("	{ "+decisionPathLabel+" }, score = %.3f\n", num/denom);
	// 				}
	// 			}		
	// 		}
	// 	} else {
	// 		System.out.format("Best edge = "+ldlm.getEdgeLabelIndex().get(bestEdge)+", score = %.3f, decay factor = %.3f\n", maxScore, decayFactor.get(bestEdge));
	// 		System.out.println("Non-zero score edge not found! SumDf = "+sumDf);
	// 	}
		

	// 	CandidateEdgeScore ce = new CandidateEdgeScore();
	// 	ce.edge = (sumDf > 0.0) ? bestEdge : -1; //return best edge -1 if all candidate edge has 0.0 decay factor
	// 	ce.score = maxScore;

	// 	allDecisionPathsTemp.clear();
		
	// 	return ce;
	// }




	/**
	 * MUST learnModel() before calling this method.
	 * finds the edge with the highest log probability among the candidate edges, given the history.
	 * if there is no history, prior probabilities of each candidate is returned back.
	 * @param history
	 * @param candidateEdges
	 * @return
	 */

	/*
	public int findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		ArrayList<Integer> rankedList = new ArrayList<Integer>();
		//int bestEdge = 0;
		// data structure to hold the number of random subsets for which a candidate edge was the best choice in.
		HashMap<Integer, ArrayList<EdgeSupport>> candidateEdgeSupportCount;
		if(history.isEmpty()) {
			rankedList = getLargestSupportCandidateFromFullLog(candidateEdges);
		}
		else {
			candidateEdgeSupportCount = new HashMap<Integer, ArrayList<EdgeSupport>>();
			HashSet<MutableString> seenSubsets = new HashSet<MutableString>();
			double numberOfSubsets = getNumberOfRandomSubsets(history);

			ArrayList<Integer> historyCopy = new ArrayList<Integer>(history);
			HashSet<Integer> userLogSubsetLineIDs = new HashSet<Integer>();
			//ArrayList<LogDetails> userLogSubset = new ArrayList<LogDetails>();
			for(int i=0; i<numberOfSubsets; i++) {
				//logger.debug(" ================================================ ");
				//int denominatorCount = getSubsetUserLog(historyCopy, userLogSubset, seenSubsets);
				int denominatorCount = getSubsetUserLog(historyCopy, userLogSubsetLineIDs, seenSubsets);
//				logger.debug("got subset ++++++++++++++++ ");
				//populateCandidateSupport(userLogSubset, candidateEdges, candidateEdgeSupportCount, denominatorCount);
				populateCandidateSupport(userLogSubsetLineIDs, candidateEdges, candidateEdgeSupportCount, denominatorCount);
				//userLogSubset.clear();
				userLogSubsetLineIDs.clear();
//				logger.debug("got support ---------------- ");
//				logger.debug(" ================================================= ");
			}
//			logger.debug("number of subsets = " + numberOfSubsets);
			rankedList = getBestCandidate(candidateEdgeSupportCount);
		}
		if(rankedList.size() >= 1) {
			return rankedList.get(0);
		} else {
			return -1;
		}
	}
	*/


	private double getNumberOfRandomSubsets(HashSet<Integer> history) {
		return numOfRandomSubsets;
		/*double numSubsets = numOfRandomSubsets;
		if(randomSubsetStrategy == 0) {
			numSubsets = numOfRandomSubsets;
		}
		else if(randomSubsetStrategy == 1) {
			numSubsets = 1;
		}
		else if(randomSubsetStrategy == 2) {
			numSubsets = history.size();
		}
		return numSubsets;*/
	}

	/**
	 * This method assumes the user log to use is the entire user log. Just count thru the entire user log instead of a subset of it.
	 * @param candidateEdges
	 * @return
	 */
	private ArrayList<CandidateEdgeScore> getLargestSupportCandidateFromFullLog(HashSet<Integer> candidateEdges) {
		ArrayList<CandidateEdgeScore> rankedEdgesScore = new ArrayList<CandidateEdgeScore>();
    /*
		HashMap<Integer, Integer> candEdges = new HashMap<Integer, Integer>();
		for(LogDetails entry : userLog) {
			Iterator<Integer> iter = candidateEdges.iterator();
			while(iter.hasNext()) {
				int e = iter.next();
				if(entry.getEdges().contains(e)) {
					// TODO: check if this is what you want to add in the future too.
					int cnt = entry.getSpecificCount();
					if(candEdges.containsKey(e)) {
						cnt += candEdges.get(e);
					}
					candEdges.put(e, cnt);
				} else {
					if(!candEdges.containsKey(e)) {
						candEdges.put(e, 0);
					}
				}
			}
		}*/
		/*int bestEdge = 0;
		int bestScore = 0;*/
		//Iterator<Integer> iter = candEdges.keySet().iterator();
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext()) {
			int e = iter.next();
			/*if(candEdges.get(e) > bestScore) {
				bestScore = candEdges.get(e);
				bestEdge = e;
			}*/
			CandidateEdgeScore ce = new CandidateEdgeScore();
			ce.edge = e;
			if(userLogEdgeCount.containsKey(e)) {
				ce.score = userLogEdgeCount.get(e);
			} else {
				ce.score = 0;
			}

			//System.out.println(ce.score);
			rankedEdgesScore.add(ce);
		}
		return rankedEdgesScore;
		//return sortCandidateEdgesByScore(rankedEdgesScore);
	}

	/**
	 * Generates a random number in the range [min, max], i.e., inclusive of min and max.
	 * @param min
	 * @param max
	 * @return
	 */
	private int getRandomNumber(int max) {
		Random rand = new Random();
		return (rand.nextInt(max + 1));
	}

	/**
	 * Get the user log entries associated with a decision path.. Choose one element in history for each node of the decision path.
	 * Stop when the number of entries in the user log subset selected is less than a threshold. That is the user log to consider for
	 * finding the best edge.
	 * @param history
	 * @param userLogSubset
	 * @return
	 */

// 	private int getSubsetUserLog(ArrayList<Integer> history, ArrayList<LogDetails> userLogSubset, HashSet<MutableString> seenSubsets) {
// 		int historyMaxIndex = history.size()-1;
// 	//	System.out.println("history max size = " + historyMaxIndex);
// 	//	boolean subsetThresholdReached = false;

// 		/*for(int i=0; i<history.size(); i++) {
// 			System.out.println(history.get(i));
// 		}*/
// 		HashSet<Integer> logLines = new HashSet<Integer>();
// 		ArrayList<Integer> decisionPathArray = new ArrayList<Integer>();
// 		/*while(historyMaxIndex >= 0) {
// 			int index = 0;
// 			if(historyMaxIndex > 0)
// 				index = getRandomNumber(historyMaxIndex);
// 			long edge = history.get(index);
// 			decisionPathArray.add(edge);
// 			// swap the entry in history[index] with the last max element. This is the Knuth-Fisher-Yates algorithm for shuffling.
// 			//		System.out.println(index + " " + historyMaxIndex);
// 			history.set(index, history.get(historyMaxIndex));
// 			history.set(historyMaxIndex, edge);
// 			historyMaxIndex--;
// 		}*/
// 		MutableString decisionPath = new MutableString();
// //		logger.debug("history max index, before = " + historyMaxIndex);
// 		int remainingLines = 0;
// 		while(historyMaxIndex >= 0) {
// 			int index = 0;
// 			if(historyMaxIndex > 0)
// 				index = getRandomNumber(historyMaxIndex);
// 			int edge = history.get(index);
// 		//	System.out.println("\t" + index + " -- > " + edge);
// 			if(!positiveEdgeInvertedIndex.containsKey(edge) && !negativeEdgeInvertedIndex.containsKey(edge)) {
// //				logger.debug("NO entry was found for this edge in the userlog " + edge);
// 				historyMaxIndex--;
// 				//logLines.clear();
// 				//break;
// 				continue;
// 			}

// 			decisionPathArray.add(edge);
// 			//decisionPath = decisionPath.append(edge).append(",");
// 			// find the subset of lines in the user log that satisfy the conditions of the decision path we have seen so far.
// 			Iterator<Integer> newEdgeIter;
// 			HashSet<Integer> linesFromLogForEdge;

// 			if(edge > 0) {
// 				newEdgeIter = positiveEdgeInvertedIndex.get(edge).iterator();
// 				linesFromLogForEdge = positiveEdgeInvertedIndex.get(edge);
// 			}
// 			else {
// 				newEdgeIter = negativeEdgeInvertedIndex.get(edge).iterator();
// 				linesFromLogForEdge = negativeEdgeInvertedIndex.get(edge);
// 			}
// 			if(logLines.isEmpty()) {
// 				while(newEdgeIter.hasNext()) {
// 					logLines.add(newEdgeIter.next());
// 				}
// 			}
// 			else {
// 				Iterator<Integer> oldIter = logLines.iterator();
// 				ArrayList<Integer> removeLines = new ArrayList<Integer>();
// 				while(oldIter.hasNext()) {
// 					int l = oldIter.next();
// 					if(!linesFromLogForEdge.contains(l)) {
// 						removeLines.add(l);
// 					}
// 				}
// 				remainingLines = logLines.size() - removeLines.size();
// 				if(remainingLines > logSizeThreshold) {
// 					for(int i=0; i<removeLines.size(); i++)
// 						logLines.remove(removeLines.get(i));
// 				}
// 				else {
// //					logger.debug("breaking out! " + logLines.size());
// 					break;
// 				}
// 			}

// 			if(logLines.size() <= logSizeThreshold) {
// 				// the code must not come in here at all since size of log will always be bigger than logSizeThreshold.
// 				// if the execution goes to the previous "else", it will break out anyway. if it cannot go to the previous break in else
// 				// the execution can't come here anyway.
// //				logger.warn("breaking out! Should not be coming here if log is bigger than logsizethreshold" + logLines.size());
// 				//logger.warn("breaking out!" + logLines.size());
// 				break;
// 			}
// 			// swap the entry in history[index] with the last max element. This is the Knuth-Fisher-Yates algorithm for shuffling.
// 	//		System.out.println(index + " " + historyMaxIndex);
// 			history.set(index, history.get(historyMaxIndex));
// 			history.set(historyMaxIndex, edge);
// 			historyMaxIndex--;
// 		}
// //		logger.debug("history max index, AFTER = " + historyMaxIndex);
// 		// now copy the actual user log entries of the leaf node of the decision path.
// 		int totalDenominatorCount = 0;
// 		Collections.sort(decisionPathArray);
// 		for(int i=0; i<decisionPathArray.size(); i++) {
// 			decisionPath = decisionPath.append(decisionPathArray.get(i)).append(",");
// 		}
// 		if(seenSubsets.contains(decisionPath)) {
// //			logger.debug("ignoring seen decision path");
// 		}
// 		else {
// //			logger.debug(" Edges selected = " + decisionPath);
// //			logger.debug("Number of edges selected from history = " + (history.size()-historyMaxIndex) + " loglines = " + logLines.size());

// 			Iterator<Integer> iter = logLines.iterator();
// 			while(iter.hasNext()) {
// 				int line = iter.next();
// 				userLogSubset.add(userLog.get(line));
// 				totalDenominatorCount += userLog.get(line).getSpecificCount();
// 			}
// 			seenSubsets.add(decisionPath);
// 		}
// 		return totalDenominatorCount;
// 	}

	// This function generates the next decision path in the power set of history
// 	private MutableString getNextDecisionPath(int decisionPathNum, ArrayList<Integer> history, HashSet<Integer> logLines) {
// 		ArrayList<Integer> decisionPathArray = new ArrayList<Integer>();
// 		MutableString decisionPath = new MutableString();
// 		int mask = 1;
// 		for(int i=0; i < history.size(); i++) {
// 			int index = 0;
// 			//System.out.println(mask+":"+decisionPathNum);
// 			if((mask & decisionPathNum) != 0)
// 				index = i;
// 			else {
// 				mask <<= 1;
// 				continue;
// 			}
				

// 			int edge = history.get(index);
// 			//System.out.println("picked edge--> "+edge);
// 		//	System.out.println("\t" + index + " -- > " + edge);
// 			if(!positiveEdgeInvertedIndex.containsKey(edge) && !negativeEdgeInvertedIndex.containsKey(edge)) {
// //				logger.debug("NO entry was found for this edge in the userlog " + edge);
// 				continue;
// 			}

// 			decisionPathArray.add(edge);

// 			if(logLines.isEmpty()) {
// 				if(edge > 0) {
// 					logLines.addAll(positiveEdgeInvertedIndex.get(edge));
// 				} else {
// 					logLines.addAll(negativeEdgeInvertedIndex.get(edge));
// 				}
// 			} else {
// 				//HashSet<Integer> remainingLog = new HashSet<Integer>(logLines);
// 				if(edge > 0) {
// 					logLines.retainAll(positiveEdgeInvertedIndex.get(edge));
// 				} else {
// 					logLines.retainAll(negativeEdgeInvertedIndex.get(edge));
// 				}
// 				if(logLines.size() <= logSizeThreshold) {
// 					break;
// 				}
// 			}
// 			mask <<= 1;
// 		}

// 		//System.out.println("logLines size for generated path = "+logLines.size());
// //		logger.debug("history max index, AFTER = " + historyMaxIndex);
// 		// now copy the actual user log entries of the leaf node of the decision path.
// 		int totalDenominatorCount = 0;
// 		Collections.sort(decisionPathArray);
// 		for(int i=0; i<decisionPathArray.size(); i++) {
// 			decisionPath = decisionPath.append(decisionPathArray.get(i)).append(",");
// 		}
// 		return decisionPath;
// 	}


	// This function generates a random decision path using "history". Also retains lines in user log that subsumes that decision path
	private MutableString getRandomDecisionPath(ArrayList<Integer> history) {
		int historyMaxIndex = history.size()-1;
		ArrayList<Integer> decisionPathArray = new ArrayList<Integer>();
		
//		logger.debug("history max index, before = " + historyMaxIndex);
		MutableString decisionPath = new MutableString();
		while(historyMaxIndex >= 0) {
			int index = 0;
			if(historyMaxIndex > 0)
				index = getRandomNumber(historyMaxIndex);
			int edge = history.get(index);
		//	System.out.println("\t" + index + " -- > " + edge);
			if(!positiveEdgeInvertedIndex.containsKey(edge) && !negativeEdgeInvertedIndex.containsKey(edge)) {
//				logger.debug("NO entry was found for this edge in the userlog " + edge);
				historyMaxIndex--;
				continue;
			}

			MutableString prevDecisionPath = decisionPath;
			decisionPathArray.add(edge);
			decisionPath = listToString(decisionPathArray);

			//System.out.println("prevDecisionPath = "+prevDecisionPath+", decisionPath = "+decisionPath);

			if(!logLines.containsKey(decisionPath)) {
				if(logLines.containsKey(prevDecisionPath)) {
					//System.out.println("size of prevDecisionPath before = "+logLines.get(prevDecisionPath).size());
					HashSet<Integer> temp = new HashSet<Integer>(logLines.get(prevDecisionPath));
					temp.retainAll(positiveEdgeInvertedIndex.get(edge));
					//System.out.println("size of prevDecisionPath after = "+logLines.get(prevDecisionPath).size());
					//System.out.println("size of temp = "+temp.size());
					logLines.put(decisionPath, temp);
				} else {
					logLines.put(decisionPath, positiveEdgeInvertedIndex.get(edge));
				}
			}
			if(logLines.get(decisionPath).size() <= logSizeThreshold) break;

			// if(!logLines.contains(decisionPath)) {
			// 	if(edge > 0) {
			// 		logLines.put(decisionPath, positiveEdgeInvertedIndex.get(edge));
			// 	} else {
			// 		logLines.put(decisionPath, negativeEdgeInvertedIndex.get(edge));
			// 	}
			// } else {
			// 	//HashSet<Integer> remainingLog = new HashSet<Integer>(logLines);
			// 	if(edge > 0) {
			// 		logLines.get(decisionPath).retainAll(positiveEdgeInvertedIndex.get(edge));
			// 	} else {
			// 		logLines.retainAll(negativeEdgeInvertedIndex.get(edge));
			// 	}
			// 	if(logLines.size() <= logSizeThreshold) {
			// 		if(countCondition == 1) {
			// 			HashSet<Integer> logLinesPrev = new HashSet<Integer>(logLines);
			// 			logLines.clear();
			// 			for(Integer line : logLinesPrev) logLines.add(new Integer(line));
			// 			//logLines.addAll(logLinesPrev);
			// 			decisionPathArray.remove(decisionPathArray.size()-1);
			// 		}
			// 		break;
			// 	}
			// }


			// swap the entry in history[index] with the last max element. This is the Knuth-Fisher-Yates algorithm for shuffling.
			history.set(index, history.get(historyMaxIndex));
			history.set(historyMaxIndex, edge);
			historyMaxIndex--;
		}

		//System.out.println("logLines size for generated path = "+logLines.size());
//		logger.debug("history max index, AFTER = " + historyMaxIndex);
		// now copy the actual user log entries of the leaf node of the decision path.

		return decisionPath;
	}

	private MutableString listToString(ArrayList<Integer> lst) {
		MutableString str = new MutableString();
		Collections.sort(lst);
		for(int i=0; i<lst.size()-1; i++) {
			str = str.append(lst.get(i)).append(",");
		}
		str = str.append(lst.get(lst.size()-1));
		return str;
	} 

	/**
	 * OVERLOADED Method
	 * Get the user LINE NUMBERS of log entries associated with a decision path.. Choose one element in history for each node of the decision path.
	 * Stop when the number of entries in the user log subset selected is less than a threshold. That is the user log to consider for
	 * finding the best edge.
	 * @param history
	 * @param userLogSubset
	 * @return
	 */

	private void getSubsetUserLog(MutableString decisionPath, HashMap<MutableString, SubsetInfo> seenSubsets) {
		// System.out.println("decision path = "+decisionPath);
		// System.out.println("log lines size = "+logLines.size());

		if(seenSubsets.containsKey(decisionPath)) {
			//System.out.println("This path has already been built!");
			return;
		}

		SubsetInfo si = new SubsetInfo();
		si.denominatorCount = 0;
		si.lineIDs = new HashSet<Integer>();

		Iterator<Integer> iter = logLines.get(decisionPath).iterator();
		//Iterator<Integer> iter = userLogSubsetLineIDs.iterator();
		//System.out.println("userLogSubsetLineIDs size = " + userLogSubsetLineIDs.size());
		while(iter.hasNext()) {
			int line = iter.next();
			si.lineIDs.add(line);
			si.denominatorCount += userLog.get(line).getSpecificCount();
		}

		seenSubsets.put(decisionPath, si);
	}

	/**
	 * Method to count the support for each candidate in this subset of user log.
	 * @param userLogSubset
	 * @param candidateEdges
	 * @param candidateEdgeSupportCount
	 */
	private void populateCandidateSupport(MutableString decisionPath, HashSet<Integer> userLogSubsetLineIDs, HashSet<Integer> candidateEdges,
			HashMap<MutableString, HashMap<Integer, EdgeSupport>> candidateEdgeSupportCount, int denominator) {
		//System.out.println("candidateEdgeSupportCount size = "+candidateEdgeSupportCount.size());
		HashSet<Integer> seenCands = new HashSet<Integer>();
		//System.out.println("user log subset size in populateCandidateSupport = " + userLogSubsetLineIDs.size());

		if(userLogSubsetLineIDs.isEmpty())
			return;

		Iterator<Integer> iter = candidateEdges.iterator();
		// long total = 0;
		// long maxTime = -1;
		// int maxEdge = 0;
		HashSet<Integer>candidateLines = new HashSet<Integer>();
		while(iter.hasNext()) {
			int candidate = iter.next();
			if(Arrays.asList(decisionPath.toString().split(",")).contains(Integer.toString(candidate))) continue; // avoid a candidate edge if it is already in the query session
			if(candidateEdgeSupportCount.containsKey(decisionPath) && candidateEdgeSupportCount.get(decisionPath).containsKey(candidate)) {
				//System.out.println("This candidate has already been visited for this path!");
				continue;
			}

			EdgeSupport es = new EdgeSupport();

			//System.out.println(candidate);
			Iterator<Integer> candidateIter = null;
			if(candidate < 0) {
				//System.out.println("This is an error. candidate has to be positive " + candidate);
				continue;
			}
      //System.out.println("positiveEdgeInvertedIndex size = "+positiveEdgeInvertedIndex.size());
			if(!positiveEdgeInvertedIndex.containsKey(candidate)) {
				//System.out.println("candidate edge not in query log!");
				// this edge is not seen in the user log at all! Give it a zero support instead of ignoring it.
				//System.out.println("Not in query log: " + candidate);
				es.supportCount = 0;
				es.subsetUserLogSize = denominator;
			} else {
				//long start = java.lang.System.currentTimeMillis();
				candidateLines.clear();
				candidateLines.addAll(positiveEdgeInvertedIndex.get(candidate));
				//long t = (java.lang.System.currentTimeMillis()-start);
				// total += t;
				// if(t > maxTime) {
				// 	maxTime = t;
				// 	maxEdge = candidate;
				// }
				//System.out.println("candidateLines size before = "+candidateLines.size());
				candidateLines.retainAll(userLogSubsetLineIDs);
				//System.out.println("candidateLines size after = "+candidateLines.size());
				
				

				if(candidateLines.isEmpty()) {
					//System.out.println("candidate edge "+candidate+" not in the sessions that subsumes the decision path!");
					//candidate doesn't appear in userLogSubset
					es.supportCount = 0;
					es.subsetUserLogSize = denominator;
				} else {
					//System.out.println("candidate edge found in the sessions that subsumes the decision path :)");
					candidateIter = candidateLines.iterator();
					//System.out.println(candidateEdges.size() + ", " + candidateLines.size());

					es.supportCount = 0;
					es.subsetUserLogSize = denominator;

					while(candidateIter.hasNext()) {
						int lineNum = candidateIter.next();
						es.supportCount += userLog.get(lineNum).getSpecificCount();
					}
				}
			}

			if(!candidateEdgeSupportCount.containsKey(decisionPath)) {
				HashMap<Integer, EdgeSupport> candidateSupport = new HashMap<Integer, EdgeSupport>();
				candidateSupport.put(candidate, es);
				candidateEdgeSupportCount.put(decisionPath, candidateSupport);
			} else {
				candidateEdgeSupportCount.get(decisionPath).put(candidate, es);
			}
 		}
		//System.out.println("	time took to initialize = "+total+", maxTime = "+maxTime+", maxEdge = "+maxEdge+", positiveEdgeInvertedIndex for maxEdge = "+positiveEdgeInvertedIndex.get(maxEdge).size());
	}

	/**
	 * Method to count the support for each candidate in this subset of user log.
	 * @param userLogSubset
	 * @param candidateEdges
	 * @param candidateEdgeSupportCount
	 */
	private void populateCandidateSupport(ArrayList<LogDetails> userLogSubset, HashSet<Integer> candidateEdges,
			HashMap<Integer, ArrayList<EdgeSupport>> candidateEdgeSupportCount, int denominator) {
		HashSet<Integer> seenCands = new HashSet<Integer>();
	//	System.out.println("user log subset size = " + userLogSubset.size());
		for(int i=0; i<userLogSubset.size(); i++) {
			//Iterator<Long> iter = ld.edges.iterator();
			Iterator<Integer> iter = candidateEdges.iterator();
			while(iter.hasNext()) {
				int e = iter.next();
				//if(e > 0 && candidateEdges.contains(e)) {
				if(userLogSubset.get(i).getEdges().contains(e)) {
					if(seenCands.contains(e)) {
						// this edge was seen in one of the previous log entries of this random subset.
						int ind = candidateEdgeSupportCount.get(e).size() - 1;
						candidateEdgeSupportCount.get(e).get(ind).supportCount += userLogSubset.get(i).getSpecificCount();
					}
					else {
						EdgeSupport es = new EdgeSupport();
						es.supportCount += userLogSubset.get(i).getSpecificCount();
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
		/*for(logDetails ld : userLogSubset) {
			//Iterator<Long> iter = ld.edges.iterator();
			Iterator<Integer> iter = candidateEdges.iterator();
			while(iter.hasNext()) {
				int e = iter.next();
				//if(e > 0 && candidateEdges.contains(e)) {
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
		}*/
	}

	/**
	 * We have the count of various candidate edges. Compute the weighted average and choose the best option.
	 * @param candidateEdgeSupportCount
	 * @return
	 */
	private ArrayList<CandidateEdgeScore> getBestCandidate(HashSet<Integer> candidateEdges, ArrayList<MutableString> allDecisionPaths, HashMap<MutableString, HashMap<Integer, EdgeSupport>> candidateEdgeSupportCount, HashSet<Integer> history) {
		ArrayList<CandidateEdgeScore> rankedEdgesScore = new ArrayList<CandidateEdgeScore>();

		for(int edge : candidateEdges) {
			ArrayList<Double> scores = new ArrayList<Double>();
			for(MutableString decisionPath : allDecisionPaths) {
				double sc = 0.0;
				//System.out.println("check dp = "+decisionPath);
				if(candidateEdgeSupportCount.containsKey(decisionPath) && candidateEdgeSupportCount.get(decisionPath).containsKey(edge)) {
					EdgeSupport es = candidateEdgeSupportCount.get(decisionPath).get(edge);
					// if(ce.score == 0)
					// 	System.out.println(edge+" = "+es.supportCount+","+es.subsetUserLogSize);

					if(es.subsetUserLogSize == 0)
						sc = 0.0;
					else {
						if(isWeightedConf) {
							//weighted score by overlap
							String[] decisionPathEdges = decisionPath.toString().split(",");
							int commonCnt = 0;
							for(String e : decisionPathEdges) {
								if(history.contains(Integer.parseInt(e))) commonCnt += 1;
							}
							double overlap = (commonCnt * 1.0)/history.size();
							sc = (overlap*es.supportCount)/es.subsetUserLogSize;
						} else {
							sc = es.supportCount/es.subsetUserLogSize;
						}
					}
				}
				scores.add(sc);
			}


			CandidateEdgeScore ce = new CandidateEdgeScore();
			ce.edge = edge;
			ce.score = 0;

			

			int k = topkRules;
			if(k == -1) k = allDecisionPaths.size();
			Collections.sort(scores);
			Collections.reverse(scores);

			//System.out.println(scores.size()+","+topkRules+","+allDecisionPaths.size());
			//System.out.println(Math.min(scores.size(), k));

			for(int i = 0; i < Math.min(scores.size(), k); i++) {
				ce.score += scores.get(i);
			}

			if(Double.isNaN(ce.score))
				ce.score = 0.0;

			rankedEdgesScore.add(ce);
		}
		return rankedEdgesScore;
	//
	//
	// //	int bestEdge = 0;
	// //	double bestScore = 0.0;
	// 	double alpha = 0.5;
	// 	Iterator<Integer> iter = candidateEdgeSupportCount.keySet().iterator();
	// 	//System.out.println("--------------------------------");
	// 	//System.out.println(candidateEdgeSupportCount.get(47185552).size());
	// 	while(iter.hasNext()) {
	// 		int edge = iter.next();
	// 		ArrayList<EdgeSupport> randoms = candidateEdgeSupportCount.get(edge);
	// 		double probabilitySum = 0.0;
	// 		//System.out.println("size = " + randoms.size());
	// 		for(EdgeSupport es : randoms) {
  //                               //if(edge == 47185552)
  //                               //       	System.out.println("Candidate " + edge + " has subsetUserLogSize = " + es.subsetUserLogSize);
	// 			if(es.subsetUserLogSize == 0)
	// 				probabilitySum += 0.0;
	// 			else {
	// 				probabilitySum += es.supportCount/es.subsetUserLogSize;
  //                                       //System.out.println("For edge " + edge + " supportCount = " + es.supportCount + ", subsetUserLogSize = " + es.subsetUserLogSize);
  //                               }
	// 		}
  //     //System.out.println("Probability sum for edge " + edge + " = " + probabilitySum);
	// 		double score = alpha*getRandomSetSupportWeight(probabilitySum) +
	// 				(1-alpha)*getNumberOfRandomSetSupportWeight(probabilitySum, randoms.size());
	// 		CandidateEdgeScore ce = new CandidateEdgeScore();
	// 		ce.edge = edge;
	// 		if(Double.isNaN(score))
	// 			ce.score = 0.0;
	// 		else
	// 			ce.score = score;
	// 		rankedEdgesScore.add(ce);
	// 	}
	// 	return rankedEdgesScore;
	// 	//return sortCandidateEdgesByScore(rankedEdgesScore);

	}

  //private ArrayList<Integer> sortCandidateEdgesByScore(ArrayList<CandidateEdgeScore> list) {
	private ArrayList<CandidateEdgeScore> sortCandidateEdgesByScore(ArrayList<CandidateEdgeScore> list) {

		ArrayList<Integer> rankedEdges = new ArrayList<Integer>();
		ArrayList<Double> rankedScores = new ArrayList<Double>();
		try {
			Collections.sort(list, new Comparator<CandidateEdgeScore>(){
				  public int compare(CandidateEdgeScore o1, CandidateEdgeScore o2) {
					  return Double.compare(o2.score, o1.score);
					 /* if(o1.score <= o2.score)
						  return 1;
					  else
						  return -1;*/
				  }
				});

			/*for(CandidateEdgeScore ce : list) {
				rankedEdges.add(ce.edge);
				rankedScores.add(ce.score);
			}*/
		} catch(IllegalArgumentException iae) {
			for(CandidateEdgeScore ce : list)
				System.out.println("Comparison error: " + ce.edge + " --> " + ce.score);
			iae.printStackTrace();
		}
		/*
                for(int i = 0; i < rankedEdges.size(); i++) {
                        System.out.println(rankedEdges.get(i) + " --> " + rankedScores.get(i));
                } */
		/*for(int i=0; i<rankedScores.size() && i<10; i++) {
			System.out.println("score for edge: " + rankedEdges.get(i) + " = " + rankedScores.get(i));
		}*/
		//return rankedEdges;
		return list;
	}

	private double getRandomSetSupportWeight(double probabilitySum) {
		//return probabilitySum/numOfRandomSubsets;
		return probabilitySum;
	}

	private double getNumberOfRandomSetSupportWeight(double probabilitySum, int numberOfRandomSubsets) {
                //System.out.println(numberOfRandomSubsets + " xxxxxxxxxxx " + numOfRandomSubsets);
		return probabilitySum * (numberOfRandomSubsets/numOfRandomSubsets);
		//return probabilitySum * numberOfRandomSubsets;
	}

}

/*
final class CandidateEdgeScore {
	int edge;
	double score;
}*/


class BareMinimumLogSubset {
	int lineNumber;
	int specificCount;
	int totalCount;
}
