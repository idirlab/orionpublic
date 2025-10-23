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
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;
//import viiq.clientServer.server.LearnDecisionForest;

import viiq.commons.LogDetails;
import viiq.commons.SubsetInfo;
import viiq.commons.GuiEdgeInfo;
import viiq.commons.CandidateEdgeScore;

import viiq.clientServer.server.LoadData;
import viiq.backendHelper.SpringClientHelper;

public class DecisionForestMain {

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

	//for all decision path, for all candidate edges store the support count
	HashMap<MutableString, HashMap<Integer, EdgeSupport>> candidateEdgeSupportCount = new HashMap<MutableString, HashMap<Integer, EdgeSupport>>();
	HashMap<MutableString, SubsetInfo> seenSubsets = new HashMap<MutableString, SubsetInfo>();

	HashMap<String, HashSet<Integer>> logLines = new HashMap<String, HashSet<Integer>>();

	//will delete later
	HashSet<MutableString> allDecisionPathsTemp= new HashSet<MutableString>();

	//for generating R and R'
	HashSet<MutableString> subsetsJustBelowCoverage = new HashSet<MutableString>();
	HashSet<MutableString> subsetsAboveCoverage = new HashSet<MutableString>();

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
	int topkRules;
	int countCondition;
	int historyUpdate;

	double numeratorPower;


	
	long lineAddTime;
	long antecedentLineAddTime;
	long lineRetainTime;

	//constructor for simulated experiments
	public DecisionForestMain(Config conf) {
		this.conf = conf;
	}

	//constructor for UI
	public DecisionForestMain(LearnDecisionForest df) {
		this.randomSubsetStrategy = df.getRandomSubsetStrategy();
		this.numOfRandomSubsets = df.getNumOfRandomSubsets();
		this.logSizeThreshold = df.getLogSizeThreshold();
		this.weightedAvgFormula = df.getWeightedAvgFormula();
		this.totalFullCnt = df.getTotalFullCnt();
		this.totalSpecificCnt = df.getTotalSpecificCnt();

		this.isWeightedConf = df.getIsWeightedConf();
		this.topkRules = df.getTopkRules();
		this.countCondition = df.getCountCondition();
		this.historyUpdate = df.getHistoryUpdate();

		this.userLog = df.getUserLog();
		this.positiveEdgeInvertedIndex = df.getPositiveEdgeInvertedIndex();
		this.negativeEdgeInvertedIndex = df.getNegativeEdgeInvertedIndex();
		this.userLogEdgeCount = df.getUserLogEdgeCount();
		this.numeratorPower = df.getNumeratorPower();
	}

	//used for simulated experiments
	public void learnModel(int n, boolean w, int k, int c, int h) {
		randomSubsetStrategy = Integer.parseInt(conf.getProp(PropertyKeys.randomSubsetStrategy));
		
		logSizeThreshold = Integer.parseInt(conf.getProp(PropertyKeys.userLogSizeThreshold));
		weightedAvgFormula = Integer.parseInt(conf.getProp(PropertyKeys.weightedAverage));

		numeratorPower = Double.parseDouble(conf.getProp(PropertyKeys.numeratorPower));

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

		System.out.println("Load data for finding the number of instances associated with each type");
		ldlm.loadInstancesPerTypeCount(conf.getInputFilePath(PropertyKeys.instanceCountForTypes));
		System.out.println("Load data for type to entity mappings");
		ldlm.loadTypetoEntitiesMapping(conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFile));
    	System.out.println("Load end entities of all edgetypes");
		ldlm.loadEdgeEndEntities(conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile));
		System.out.println("Loading type to edge list map, for source types");
		ldlm.loadSourceTypeToEdgesMap(conf.getInputFilePath(PropertyKeys.typeEdgesListSource));
		System.out.println("Loading type to edge list map, for object types");
		ldlm.loadObjectTypeToEdgesMap(conf.getInputFilePath(PropertyKeys.typeEdgesListObject));

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

	public void clearCache() {
		seenSubsets.clear();
		candidateEdgeSupportCount.clear();
		logLines.clear();
	}

	//generating R and R'
	private void initializeSubsets(HashSet<Integer> history, double logdf) {

		subsetsAboveCoverage.clear();
		subsetsJustBelowCoverage.clear();
		
		ArrayList<Integer> historyArray = new ArrayList<Integer>(history);

		for(long state = 1; state < (1<<historyArray.size()); state++) {
			long mask = 1;
			ArrayList<Integer> subsetArray = new ArrayList<Integer>();
			for(int i = 0; i < historyArray.size(); i++) {
				if((state & mask) != 0) {
					subsetArray.add(historyArray.get(i));
				}
				mask <<= 1;
			}
			MutableString curStr = listToString(subsetArray);

			if(!logLines.containsKey(curStr.toString())) {
				if(subsetArray.size() > 1) {
					//System.out.println("size 2 = "+subsetArray.size());
					//look for a previously visited subset only if this subset has more than 1 element,
					//otherwise this is a single element subset, which is the beginning

					int lastEntry = subsetArray.get(subsetArray.size()-1);

					//remove the last array element to reach a previously visited state
					subsetArray.remove(new Integer(lastEntry));
					MutableString prevStr = listToString(subsetArray);

					//loading the log lines, since it was previously visited and was cached
					HashSet<Integer> prevLines = new HashSet<Integer>(logLines.get(prevStr.toString()));

					prevLines.retainAll(positiveEdgeInvertedIndex.getOrDefault(lastEntry, new HashSet<Integer>()));
				
					logLines.put(curStr.toString(), prevLines);
				} else {
					int singleElement = subsetArray.get(0);
					//System.out.println("size 3 = "+positiveEdgeInvertedIndex.getOrDefault(singleElement, new HashSet<Integer>()).size());
					logLines.put(curStr.toString(), positiveEdgeInvertedIndex.getOrDefault(singleElement, new HashSet<Integer>()));
				}
			} else { 
				//System.out.println("size 1 = "+logLines.get(curStr.toString()).size());				
			}

			//System.out.println(curStr + " --> " + logLines.get(curStr.toString()).size() + ", "+ logSizeThreshold);


			if(logLines.get(curStr.toString()).size() >= logSizeThreshold * logdf) {
				//add subset to R'
				//System.out.println("logLines of curStr "+curStr+" = "+logLines.get(curStr).size()+", logSizeThreshold = "+logSizeThreshold);
				subsetsAboveCoverage.add(curStr); 
				subsetsJustBelowCoverage.add(curStr); //a deviation from the pseudocode
			} else if(countCondition == 2) {
				//add subset to R
				//System.out.println("curStr = "+curStr);
				for(int i = 0; i < subsetArray.size(); i++) {
					ArrayList<Integer> tempArray = new ArrayList<Integer>(stringToList(curStr));
					
					//System.out.println("tempArray before removal= "+tempArray.toString());
					//remove one item to see if coverage goes below the threshold
					tempArray.remove(i);

					//System.out.println("tempArray after removal= "+tempArray.toString());
					MutableString tempStr = new MutableString(); 
					//System.out.println("	tempStr = "+tempStr);

					if(!tempArray.isEmpty()) {
						tempStr = listToString(tempArray);
						//System.out.println("logLines of tempStr "+tempStr+" = "+logLines.get(tempStr).size()+", logSizeThreshold = "+logSizeThreshold);
					}

					if(tempArray.isEmpty() || logLines.get(tempStr.toString()).size() >= logSizeThreshold * logdf) {
						subsetsJustBelowCoverage.add(curStr);
					}
				}
			}
		}
	}

	/**
	 * MUST learnModel() before calling this method.
	 * ranks all the candidate edges, given the history.
	 * if there is no history, prior probabilities of each candidate is returned back.
	 * @param history
	 * @param candidateEdges
	 * @return
	 */
	public ArrayList<CandidateEdgeScore> rankCandidateEdges(HashSet<Integer> history, HashSet<Integer> candidateEdges, double logdf) {
		
		ArrayList<CandidateEdgeScore> rankedList = new ArrayList<CandidateEdgeScore>();	

		if(history.isEmpty()) {
			long start = System.currentTimeMillis();
			rankedList = getLargestSupportCandidateFromFullLog(candidateEdges);
			//System.out.println("Time spent getLargestSupportCandidateFromFullLog "+(System.currentTimeMillis()-start)/1000.0 + "seconds");
		}
		else {
			
			long start = System.currentTimeMillis();
			//initializeSubsets(history);
			//System.out.println("Time spent initializeSubsets "+(System.currentTimeMillis()-start)/1000.0 + "seconds");

			start = System.currentTimeMillis();

			rankedList = getCandidateScores(candidateEdges, history, logdf);

			// if(countCondition == 2) {
			// 	System.out.println("subsetsJustBelowCoverage size = "+subsetsJustBelowCoverage.size());
			// 	rankedList = getCandidateScores(subsetsJustBelowCoverage, candidateEdges, history);
			// } else {
			// 	rankedList = getCandidateScores(subsetsAboveCoverage, candidateEdges, history);
			// }
			//System.out.println("Time spent getCandidateScores "+(System.currentTimeMillis()-start)/1000.0 + "seconds");
		}
		return rankedList;
	}

	private void getSubsetScoreForCandidate(int e, MutableString origSubset, MutableString subset, HashSet<Integer> lines, ArrayList<Double> allSubsetScores, HashSet<Integer> history, double logdf) {

		EdgeSupport es = new EdgeSupport();

		if(candidateEdgeSupportCount.containsKey(subset) && candidateEdgeSupportCount.get(subset).containsKey(e)) {
			// `e` was already visited as a candidate of `subset` before
			// if(e==47182598) {
			// 	System.out.println(subset+" --> "+candidateEdgeSupportCount.get(subset).get(e).supportCount+"/"+candidateEdgeSupportCount.get(subset).get(e).subsetUserLogSize);
			// }
			
			es = candidateEdgeSupportCount.get(subset).get(e);
			//if(e==47185820) System.out.println(e+" --> "+subset+" --> "+sc+","+es.subsetUserLogSize+","+score+" --> "+","+logSizeThreshold+","+logdf);
		}
		else {
			if(positiveEdgeInvertedIndex.get(e) == null || (origSubset == null && (!logLines.containsKey(subset.toString()) || logLines.get(subset.toString()).size() == 0 ))) {
				//assigning values in a way that P(e|subset)=0 and there is no division by 0

				// if(e==47183530) {
				// 	if(positiveEdgeInvertedIndex.get(e) == null) System.out.println("cand edge not found in log");
				// 	else System.out.println("subset not found in log");
				// }

				es.supportCount = 0;
				es.subsetUserLogSize = 1;
			} else {

				long start = System.currentTimeMillis();
				lines.clear();
				lines.addAll(positiveEdgeInvertedIndex.get(e));
				//if(e==47183530 ||  e==47186305) System.out.println("lines for candidate edge "+e+" --> "+lines.size());

				lineAddTime += (System.currentTimeMillis() - start);

				//System.out.print("lineAddTime = "+(System.currentTimeMillis() - start) + ", ");


				start = System.currentTimeMillis();
				HashSet<Integer> antecedentLines = new HashSet<Integer>();
				ArrayList<Integer> origEntries = new ArrayList<Integer>();
				ArrayList<Integer> subsetList = stringToList(subset);
				

				if(origSubset != null) {
					ArrayList<Integer> origSubsetList = stringToList(origSubset);
					for(int entry : subsetList) {
						if(origSubsetList.contains(entry)) {
							origEntries.add(entry);
						}

						// if(!origSubsetList.contains(entry)) {
						// 	if(antecedentLines== null) {
						// 		antecedentLines = (HashSet)positiveEdgeInvertedIndex.get(entry).clone();
						// 		//System.out.println("antecedentLines = "+antecedentLines.size()+ ", "+origSubset+ " --> "+subset + ":" + e);
						// 	} else {
						// 		antecedentLines.retainAll(positiveEdgeInvertedIndex.get(entry));
						// 		//System.out.println("antecedentLines = "+antecedentLines.size()+ ", "+origSubset+ " --> "+subset + ":" + e);
						// 	}
						// } else {
						// 	origEntries.add(entry);
						// }
					}
					
					if(!origEntries.isEmpty()) {
						antecedentLines.addAll(logLines.get(listToString(origEntries).toString()));
					} 
					for(int entry : subsetList) {
						if(!origSubsetList.contains(entry)) {
							if(antecedentLines.isEmpty()) {
								antecedentLines.addAll(positiveEdgeInvertedIndex.get(entry));
							} else {
								antecedentLines.retainAll(positiveEdgeInvertedIndex.get(entry));
							}
						}
					}

					//System.out.println("\n");
				}

				//antecedentLineAddTime += (System.currentTimeMillis() - start);

				//System.out.print("antecedentLineAddTime = "+(System.currentTimeMillis() - start) + ", ");


				//set denominator --> count(X)
				//es.subsetUserLogSize = (double)antecedentLines.size();
				

				start = System.currentTimeMillis();

				if(origSubset == null) {
					es.subsetUserLogSize = (double)logLines.get(subset.toString()).size();
					lines.retainAll( logLines.get(subset.toString()) );
				} else {
					es.subsetUserLogSize = (double)antecedentLines.size();
					lines.retainAll(antecedentLines);
				}

				//if(e==47183530 ||  e==47186305) System.out.println("lines after adding consequent "+subset+" --> "+lines.size());

				lineRetainTime += (System.currentTimeMillis() - start);

				//System.out.println("lineRetainTime = "+(System.currentTimeMillis() - start));

				//System.out.println("{"+positiveEdgeInvertedIndex.get(e).size()+","+logLines.get(subset.toString()).size()+","+es.subsetUserLogSize +","+ logLines.keySet().size()+" --> "+subset+"}");

				//set numerator --> count(X U {e})
				//if(e==47186305) System.out.println("number of lines containing {"+ subset + " + "+e +"} = "+lines.size());

				//if(e==47183530 ||  e==47186305) System.out.println(lines.size()+" --> "+logSizeThreshold+" --> "+logdf);
				
				es.supportCount = (double)lines.size();
				
				// if(e==47182598) {
				// 	System.out.println(subset+" --> "+lines.size()+"/"+es.subsetUserLogSize);
				// }
				
			}
		}

		if(!candidateEdgeSupportCount.containsKey(subset)) {
			candidateEdgeSupportCount.put(subset, new HashMap<Integer, EdgeSupport>());
		}
		if(!candidateEdgeSupportCount.get(subset).containsKey(e)) {
			candidateEdgeSupportCount.get(subset).put(e, es);
		}
		
		//checking if supportcount is large enough to meet the current threshold, if not then set to zero

		double sc = es.supportCount;

		if(countCondition == 1 && sc < logSizeThreshold * logdf) {
			sc = 0; 
		} 

		//if(e==47186305) System.out.println(es.supportCount+" -> "+sc);



		double score = (es.subsetUserLogSize == 0.0) ? 0.0 : Math.pow(sc, numeratorPower)/es.subsetUserLogSize;	

		if(isWeightedConf) {
			//finding intersection of history and subset
			ArrayList<Integer> commonElements = new ArrayList<Integer>(history);
			commonElements.retainAll(stringToList(subset));
			double overlap = (double)commonElements.size()/history.size();
			score *= overlap;
		}
		allSubsetScores.add(score);	

		//if(e==47185820) System.out.println(e+" --> "+subset+" --> "+sc+","+es.subsetUserLogSize+","+score+" --> "+","+logSizeThreshold+","+logdf);
	}

	private HashSet<MutableString> getRandomSubsets(HashSet<MutableString> validSubsets) {

		HashSet<MutableString> randomSubsets = new HashSet<MutableString>();

		ArrayList<MutableString> validSubsetsArray = new ArrayList<MutableString>(validSubsets);

		Collections.sort(validSubsetsArray);  // to keep the same order when testing without true randomization

		if(validSubsets.size() <= numOfRandomSubsets || numOfRandomSubsets < 1) {
			randomSubsets = validSubsets;
		} else {
			//random shuffle and then extract first `numOfRandomSubsets` subsets
			
			//Collections.shuffle(validSubsetsArray);
			for(int i = 0; i < numOfRandomSubsets; i++) {
				randomSubsets.add(validSubsetsArray.get(i));
			}
		}

		return randomSubsets;
	}

	private ArrayList<CandidateEdgeScore> getCandidateScores(HashSet<Integer> candidateEdges, HashSet<Integer> history, double logdf) {
		
		long start = System.currentTimeMillis();
		ArrayList<CandidateEdgeScore> candidateScores = new ArrayList<CandidateEdgeScore>();

		LoadData ldlm = new LoadData();

		//System.out.println("size of randomSubsets = "+randomSubsets.size());

		
		//System.out.println("Time spent randomSubsets "+(System.currentTimeMillis()-start)/1000.0 + "seconds");
		
		

		// ArrayList<MutableString> temp = new ArrayList<MutableString>(randomSubsets);
		// System.out.println(temp.toString());
		
		//print rules
		//System.out.println("Antecedents:");

		HashSet<MutableString> validSubsets = null;
		HashSet<MutableString> randomSubsets = null;

		long origsubsetscore = 0;
		long newsubsetscore = 0;
		long newsubsetfind = 0;

		if(historyUpdate == 0 || historyUpdate == 1) {
			initializeSubsets(history, logdf);

			validSubsets = (countCondition == 2) ? subsetsJustBelowCoverage : subsetsAboveCoverage;
			
			if(validSubsets.size() > 0) {
				randomSubsets = getRandomSubsets(validSubsets);
			} else {
				//if valid subset is empty, assign equal highscore to each edge and return
				for(int ce : candidateEdges) {
					CandidateEdgeScore ces = new CandidateEdgeScore();
					ces.edge = ce;
					ces.score = 1.0;
					candidateScores.add(ces);
				}
				return candidateScores;
			}
		}

		long startRanking = System.currentTimeMillis();

		lineAddTime = 0;
		antecedentLineAddTime =  0;
		lineRetainTime = 0;

		//System.out.println("random subset size = "+ randomSubsets.size()+", "+"valid subset size = "+ validSubsets.size());
		// for(MutableString subset : randomSubsets) {
		// 	System.out.println(subset);
		// }

		// if(candidateEdges.contains(47183499)) System.out.println("recipe/author is in candidateEdges"); // 
		// if(candidateEdges.contains(47185820)) System.out.println("olympic_medal_honor/olympics-olympic_medal_honor/medalist is in candidateEdges");
		// if(candidateEdges.contains(47182598)) System.out.println("/people/person/nationality is in candidateEdges");
		

		for(int e : candidateEdges) {
		
			if(historyUpdate == 2) {
				int src = ldlm.getEdgeType().get(e).source_type;
				int obj = ldlm.getEdgeType().get(e).object_type;

				HashSet<Integer> historyNew = new HashSet<Integer>(history);
				historyNew.add(src);
				historyNew.add(obj);

				initializeSubsets(historyNew, logdf);
				validSubsets = (countCondition == 2) ? subsetsJustBelowCoverage : subsetsAboveCoverage;
				
				if(validSubsets.size() > 0) {
					//generate a random subset of size numOfRandomSubsets from validSubsets
					randomSubsets = getRandomSubsets(validSubsets);
				} else {
					//if valid subset is empty, assign equal highscore to edge
					CandidateEdgeScore ces = new CandidateEdgeScore();
					ces.edge = e;
					ces.score = 1.0;
					candidateScores.add(ces);
					continue;
				}
			}
			

			//System.out.println("size of validSubsets = "+validSubsets.size());

			// System.out.print("\t");
			// for(int a : stringToList(subset)) {
			// 	String label = ldlm.getNodeLabelIndex().get(a);
			// 	if(label == null) label = ldlm.getEdgeLabelIndex().get(a);
			// 	System.out.print(label+", ");
			// }
			// System.out.println("");

			HashSet<Integer> lines = new HashSet<Integer>();

			ArrayList<Double> allSubsetScores =  new ArrayList<Double>();

			
			for(MutableString subset : randomSubsets) {

				//start = System.currentTimeMillis();


				getSubsetScoreForCandidate(e, null, subset, lines, allSubsetScores, history, logdf);

				if(historyUpdate == 1) {
					if(!ldlm.getEdgeType().containsKey(e)) continue;
					//origsubsetscore += (System.currentTimeMillis()-start);

					//start = System.currentTimeMillis();

					//replace with edge source
					int src = ldlm.getEdgeType().get(e).source_type;
					ArrayList<Integer> newSubsetList1 = new ArrayList<Integer>();
					for(int entry : stringToList(subset)) {
						if(!ldlm.getSourceTypesToEdgesMap().containsKey(entry) || !ldlm.getSourceTypesToEdgesMap().get(entry).contains(e)) {
							newSubsetList1.add(entry);
						}
					}
					if(!newSubsetList1.contains(src)) newSubsetList1.add(src);

					//newsubsetfind += (System.currentTimeMillis()-start);

					//start = System.currentTimeMillis();

					if(newSubsetList1.size() > 0) {
						MutableString newSubset1 = listToString(newSubsetList1);
						//System.out.print("* ");
						getSubsetScoreForCandidate(e, subset, newSubset1, lines, allSubsetScores, history, logdf);
						//if(e==47185561) System.out.println(subset+" --> "+e+" --> "+newSubset1);
					}

					//newsubsetscore += (System.currentTimeMillis()-start);

					//start = System.currentTimeMillis();

					//replace with edge object
					int obj = ldlm.getEdgeType().get(e).object_type;
					ArrayList<Integer> newSubsetList2 = new ArrayList<Integer>();
					for(int entry : stringToList(subset)) {
						if(!ldlm.getObjectTypesToEdgesMap().containsKey(entry) || !ldlm.getObjectTypesToEdgesMap().get(entry).contains(e)) {
							newSubsetList2.add(entry);
						}
					}
					if(!newSubsetList2.contains(obj)) newSubsetList2.add(obj);

					//newsubsetfind += (System.currentTimeMillis()-start);

					//start = System.currentTimeMillis();

					if(newSubsetList2.size() > 0) {
						MutableString newSubset2 = listToString(newSubsetList2);
						//System.out.print("** ");
						getSubsetScoreForCandidate(e, subset, newSubset2, lines, allSubsetScores, history, logdf);
						//if(e==47185561) System.out.println(subset+" --> "+e+" --> "+newSubset2);
					}

					//newsubsetscore += (System.currentTimeMillis()-start);
				}

			}



			//System.out.println("Scores available for "+allSubsetScores.size()+" subsets");	

			Collections.sort(allSubsetScores);
			Collections.reverse(allSubsetScores);

			int k = (topkRules == -1) ? allSubsetScores.size() : Math.min(allSubsetScores.size(), topkRules);

			double sumScore = 0.0;
			for(int i = 0; i < k; i++) {
				sumScore += allSubsetScores.get(i);
			}

			CandidateEdgeScore ce = new CandidateEdgeScore();
			ce.edge = e;
			ce.score = sumScore/k;
			candidateScores.add(ce);
		}

		// System.out.println("lineAddTime = "+lineAddTime);
		// System.out.println("antecedentLineAddTime = "+antecedentLineAddTime);
		// System.out.println("lineRetainTime = "+lineRetainTime);

		// System.out.println("total time taken by rdp to assign score to all edges = "+(System.currentTimeMillis()-startRanking));



		// System.out.println("origsubsetscore = "+origsubsetscore);
		// System.out.println("newsubsetfind = "+newsubsetfind);
		// System.out.println("newsubsetscore = "+newsubsetscore);

		return candidateScores;
	}

	/**
	 * This method assumes the user log to use is the entire user log. Just count thru the entire user log instead of a subset of it.
	 * @param candidateEdges
	 * @return
	 */
	private ArrayList<CandidateEdgeScore> getLargestSupportCandidateFromFullLog(HashSet<Integer> candidateEdges) {
		ArrayList<CandidateEdgeScore> rankedEdgesScore = new ArrayList<CandidateEdgeScore>();
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext()) {
			int e = iter.next();
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
	}

	private ArrayList<CandidateEdgeScore> sortCandidateEdgesByScore(ArrayList<CandidateEdgeScore> list) {

		ArrayList<Integer> rankedEdges = new ArrayList<Integer>();
		ArrayList<Double> rankedScores = new ArrayList<Double>();
		try {
			Collections.sort(list, new Comparator<CandidateEdgeScore>(){
				  public int compare(CandidateEdgeScore o1, CandidateEdgeScore o2) {
					  return Double.compare(o2.score, o1.score);
				  }
				});
		} catch(IllegalArgumentException iae) {
			for(CandidateEdgeScore ce : list)
				System.out.println("Comparison error: " + ce.edge + " --> " + ce.score);
			iae.printStackTrace();
		}
		return list;
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

	private ArrayList<Integer> stringToList(MutableString str) {
		ArrayList<Integer> lst = new ArrayList<Integer>();
		for(String e : str.toString().split(",")) {
			if(!e.equals("")) lst.add(Integer.parseInt(e));
		}
		return lst;
	} 
}

final class EdgeSupport {
	double supportCount = 0;
	double subsetUserLogSize;
}
