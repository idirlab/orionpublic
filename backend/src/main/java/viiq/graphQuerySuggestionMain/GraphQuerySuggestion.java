/**
 * The code in this class works only with the following instances:
 * INPUT query files should ONLY contains entities, and regular edges (no type nodes and concatenated edges)
 * INPUT query log should ONLY contain log of regular edges (<30000 edge ID)
 */
package viiq.graphQuerySuggestionMain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import viiq.otherClassifiers.naiveBayesian.NaiveBayesianMain;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.otherClassifiers.randomEdgeSuggestion.RandomEdgeSuggestor;
import viiq.otherClassifiers.randomForest.RandomForestPredict;
import viiq.otherClassifiers.randomSubsets.RandomSubsetsMain;
import viiq.otherClassifiers.svd.RecommendationSystem;

import viiq.utils.PropertyKeys;

import viiq.commons.DataGraphGenerator;
import viiq.commons.EdgeEnds;
import viiq.commons.ObjNode;
import viiq.commons.ObjNodeIntProperty;
import viiq.decisionForest.DecisionForestMain;

public class GraphQuerySuggestion {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());

	HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
    //0 = DecisionForest;	1 = Random Forests;	2 = Naive Bayesian model;	3 = Random edge suggestion;
	//4 = recommendation systems (SVD);	5 = RandomSubsets;	6 = SVM;	7 = MEMM model;
	enum ModelToUse {DF, RF, NBC, RAND, SVD, RandSubsets, SVM, MEMM};
	ModelToUse model;

	/*
	 * Model objects that can be used!
	 */
	NaiveBayesianMain nbc;
	RandomSubsetsMain rsm;
	DecisionForestMain dfm;
	RandomEdgeSuggestor res;
	RecommendationSystem svd;
	RandomForestPredict rfp;
	//MEMMMain memm;
	//SVM svm;

	GraphQuerySuggestionHelper gqhelper = new GraphQuerySuggestionHelper();

	public static void main(String[] args) {
		GraphQuerySuggestion gqs = new GraphQuerySuggestion();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			gqs.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				gqs.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				gqs.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				gqs.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		DataGraphGenerator dg = new DataGraphGenerator();
		System.out.println("starting to load the data graph");
		String dataGraphFilePath = gqs.conf.getInputFilePath(PropertyKeys.datagraphFile);
		dg.loadDataGraphIntProperty(dataGraphFilePath, gqs.srcDataGraph, gqs.objDataGraph);
		//dg.loadDataGraph(dataGraphFilePath, gqs.srcDataGraph, gqs.objDataGraph);
		System.out.println("Done loading data graph");
		File inputQueryFiles = new File(gqs.conf.getInputFilePath(PropertyKeys.testPartialAndTargetQueryFiles));
		gqs.queryCompletor(inputQueryFiles);
		System.out.println("Done with completing all queries!!!");
	}

	private void queryCompletor(File inputFilesFolder) {
		model = gqhelper.getModelToUse(Integer.parseInt(conf.getProp(PropertyKeys.modelToUse)));
		System.out.println("Model IS: " + model);
		learnModel();
		String outQueryFilesFolder = conf.getOutputFilePath(PropertyKeys.edgeSuggestionOutputFolder);
		File[] listOfInputQueryFiles = inputFilesFolder.listFiles();
		for(int i=0; i<listOfInputQueryFiles.length; i++) {
			// Each file must contain the partial query graph and the target query graph.
			if(listOfInputQueryFiles[i].isFile()) {
				String outFileName = listOfInputQueryFiles[i].getName();
				System.out.println("Starting query completion for file " + outFileName);
				String inputQueryFilePath = listOfInputQueryFiles[i].getAbsolutePath();
				/*
				 * This graph representation stores the input graph and the target graph (which is to be used in prototype). In the actual
				 * system, we are unaware of the targetGraph, only the user knows it.
				 * key = edge ID
				 * value = edge ends (src, obj)
				 *
				 * NOTE: If, a key has "null" as its value, it means the key represents an Vertex ID.
				 * if value = null,
				 * key = vertex ID.
				 *
				 * The null case is not supposed to occur in the target graph. The target graph MUST be connected.
				 */
				/*HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
				HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
				// put in the set of anchor nodes.
				HashSet<Integer> anchorNodes = new HashSet<Integer>();
				ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> allPartialGraphs = gqhelper.readInputQueryFile(inputQueryFilePath, partialGraph, targetGraph, anchorNodes);
				ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> allTargetGraphs = makeDeepCopies(targetGraph, allPartialGraphs.size());
				for(int j=0; j<allPartialGraphs.size(); j++) {
					String outputTargetQueryFilePath = outQueryFilesFolder + outFileName + "-" + j;
					long startTime = java.lang.System.currentTimeMillis();
					makeEdgeSuggestions(allPartialGraphs.get(j), allTargetGraphs.get(j), anchorNodes);
					long endTime = java.lang.System.currentTimeMillis();
					System.out.println("Time taken for completing this graph (in ms) = " + (endTime - startTime));
					gqhelper.printConstructedGraph(allPartialGraphs.get(j), outputTargetQueryFilePath);
					System.out.println("COMPLETED query completion for file " + outFileName);
				}*/
				HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
				HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
				// put in the set of anchor nodes.
				HashSet<Integer> anchorNodes = new HashSet<Integer>();
				gqhelper.readInputQueryFile(inputQueryFilePath, partialGraph, targetGraph, anchorNodes);
				String outputTargetQueryFilePath = outQueryFilesFolder + outFileName;
				long startTime = java.lang.System.currentTimeMillis();
				makeEdgeSuggestions(partialGraph, targetGraph, anchorNodes);
				long endTime = java.lang.System.currentTimeMillis();
				System.out.println("Time taken for completing this graph (in ms) = " + (endTime - startTime));
				gqhelper.printConstructedGraph(partialGraph, outputTargetQueryFilePath);
				System.out.println("COMPLETED query completion for file " + outFileName);
			}
		}
	}

	private ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> makeDeepCopies(
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph, int numOfCopies) {
		ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> allCopies =
				new ArrayList<HashMap<Integer,HashMap<Integer,ArrayList<DestNode>>>>();
		for(int i=0; i<numOfCopies; i++) {
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> tg = new HashMap<Integer, HashMap<Integer,ArrayList<DestNode>>>();
			Iterator<Integer> iter1 = targetGraph.keySet().iterator();
			while(iter1.hasNext()) {
				int vertex = iter1.next();
				HashMap<Integer, ArrayList<DestNode>> propDest = targetGraph.get(vertex);
				HashMap<Integer, ArrayList<DestNode>> newPropDest = new HashMap<Integer, ArrayList<DestNode>>();

				Iterator<Integer> iter2 = propDest.keySet().iterator();
				while(iter2.hasNext()) {
					int edge = iter2.next();
					ArrayList<DestNode> destnodes = propDest.get(edge);
					ArrayList<DestNode> newDestNodes = new ArrayList<DestNode>();

					for(DestNode dn : destnodes) {
						DestNode newDn = new DestNode();
						newDn.setDest(dn.getDest());
						newDn.setForwardEdge(dn.isForwardEdge());
						newDestNodes.add(newDn);
					}
					newPropDest.put(edge, newDestNodes);
				}
				tg.put(vertex, newPropDest);
			}
			allCopies.add(tg);
		}
		return allCopies;
	}

	private void makeEdgeSuggestions(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph, HashSet<Integer> anchorNodes) {
		// contains the candidate graph, which is changed after every edge suggestion. We will delete an edge if is accepted (but added
		// into the partial graph). Edges are only deleted if it is rejected.
		// Note that, all edges of the same label are deleted from the candidate graph (assuming a yes or no is said to all edges
		// that are incident on the partial graph, which is the current instance of the candidate graph.
		// key = edge, value = <src, obj, tupleID>
		HashMap<Integer, ArrayList<EdgeEnds>> candidateGraph = new HashMap<Integer, ArrayList<EdgeEnds>>();
		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		ArrayList<Integer> history = new ArrayList<Integer>();
		// this contains the tuple IDs seen so far. This includes both the ones present in the partial graph and those rejected too.
		HashSet<Integer> seenTuples = new HashSet<Integer>();

		initializeHistorySeenTuplesListAndTargetGraph(history, seenTuples, partialGraph, targetGraph);
		initializeCandidateEdges(candidateGraph, partialGraph, candidateEdges, seenTuples);
		int numOfSuggestionsThreshold = Integer.parseInt(conf.getProp(PropertyKeys.NumberOfSuggestionsThreshold));
		int numOfSuggestions = 0;
		int numOfNegs = 0;
		HashSet<Integer> correctSuggestions = new HashSet<Integer>();
		while(numOfSuggestions < numOfSuggestionsThreshold && !candidateEdges.isEmpty() && !targetGraph.isEmpty()) {
			//logger.debug("Cand edges = " + candidateEdges.size());
			int suggestedEdge = findBestEdge(history, candidateEdges);
	//		System.out.println("candidates = " + candidateEdges.size());

			if(suggestedEdge == 0) {
				//System.out.println("Suggested edge was a 0.. breaking out..");
				logger.warn("There was no edge in candidateEdges??");
				suggestedEdge = RandomEdgeSuggestor.getRandomEdge(candidateEdges);
				//System.out.println(suggestedEdge);
				//break;
			}
			candidateEdges.remove(suggestedEdge);
			int prop = suggestedEdge;
			boolean previouslyAcceptedNowRejectedEdge = false;
			ArrayList<EdgeEnds> positiveEdges = null;
			if((positiveEdges = isPositiveSuggestedEdge(suggestedEdge, partialGraph, targetGraph)) != null) {
				gqhelper.addEdgesToPartialGraph(suggestedEdge, positiveEdges, partialGraph);
				gqhelper.removeEdgesFromTargetGraph(suggestedEdge, positiveEdges, targetGraph);
				correctSuggestions.add(prop);
			}
			else {
				if(correctSuggestions.contains(prop))
					previouslyAcceptedNowRejectedEdge = true;
				prop = prop*(-1);
			}
			if(prop < 0) {
				numOfNegs++;
				logger.debug("---> " + suggestedEdge + " ----- > NEG");
			}
			else {
				logger.debug("cand edges remaining = " + candidateEdges.size() + ", negs suggested = " +
			numOfNegs + " ---> " + suggestedEdge + " ----- > POSITIVEEEEEEE");
				numOfNegs = 0;
			}
		//	logger.debug("---> " + suggestedEdge + " ----- > " + prop);
			// add all the tuple IDs in candidate graph corresponding to a given edge label to seenTupleIDs.
			// remove all the instances of the suggested edge from candidate graph.
			updateSeenTupleIDsListAndCandidateGraph(seenTuples, suggestedEdge, candidateGraph);

			if(prop > 0) {
				// this means that the newly suggested edge was accepted. So we may get to add new candidate edges.
				findNewCandidateEdges(suggestedEdge, candidateGraph, positiveEdges, candidateEdges, seenTuples);
			}
			if(!previouslyAcceptedNowRejectedEdge)
				history.add(prop);
			numOfSuggestions++;
			if(numOfSuggestions == 24)
				System.out.println("lets start");
		}
		System.out.println("NUMBER of SUGGESTIONS MADE : " + numOfSuggestions);
		logger.info("NUMBER of SUGGESTIONS MADE : " + numOfSuggestions);
		if(numOfSuggestions == numOfSuggestionsThreshold) {
			System.out.println("100 suggestions up!!!!!!!!!");
			logger.error("100 suggestions up!!!!!!!!!");
		}
	}

	private void findNewCandidateEdges(int prop, HashMap<Integer, ArrayList<EdgeEnds>> candidateGraph,
			ArrayList<EdgeEnds> positiveEdges, HashSet<Integer> candidateEdges, HashSet<Integer> seenTuples) {
		// in this method, add all new edges that come into candidateGraph due to the addition of positiveEdges into the
		// partial graph.
		// fist construct a pseudo-partial graph that consists of only edges present in partial graph,
		// and input this to initializeCandidateEdges method. that should find all new edges that come in candidateGraph due to the
		// new edges found in positiveEdges.
		HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> pseudoPartialGraph = new HashMap<Integer, HashMap<Integer,ArrayList<DestNode>>>();
		gqhelper.addEdgesToPartialGraph(prop, positiveEdges, pseudoPartialGraph);
		initializeCandidateEdges(candidateGraph, pseudoPartialGraph, candidateEdges, seenTuples);
	}

	private void initializeCandidateEdges(HashMap<Integer, ArrayList<EdgeEnds>> candidateGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashSet<Integer> candidateEdges, HashSet<Integer> seenTuples) {
		// for every node present in the initial partial graph, add the neighboring edges (not in the partial graph) to the candidate
		// graph. also add these edges to candidate edges list.
		HashSet<Integer> addedTuples = new HashSet<Integer>();
		Iterator<Integer> iter = partialGraph.keySet().iterator();
		while(iter.hasNext()) {
			int vertex = iter.next();
			// add all those edges where vertex is the source...
			ArrayList<ObjNodeIntProperty> ons = srcDataGraph.get(vertex);
			if(ons != null) {
				for(ObjNodeIntProperty on : ons) {
					EdgeEnds ee = new EdgeEnds();
					ee.source = vertex;
					ee.object = on.dest;
					if(!addedTuples.contains(on.tupleId) && !seenTuples.contains(on.tupleId))
						addToCandidateGraph(candidateGraph, candidateEdges, seenTuples, addedTuples, on, ee);
				}
			}
			// add all those edges where vertex is the object...
			//logger.debug("neighbors of vertex " + vertex);
			ArrayList<ObjNodeIntProperty> revons = objDataGraph.get(vertex);
			if(revons != null) {
				for(ObjNodeIntProperty on : revons) {
					EdgeEnds ee = new EdgeEnds();
					ee.source = on.dest;
					ee.object = vertex;
					if(!addedTuples.contains(on.tupleId) && !seenTuples.contains(on.tupleId))
						addToCandidateGraph(candidateGraph, candidateEdges, seenTuples, addedTuples, on, ee);
				}
			}
		}
	}

	private void addToCandidateGraph(HashMap<Integer, ArrayList<EdgeEnds>> candidateGraph, HashSet<Integer> candidateEdges,
			HashSet<Integer> seenTuples, HashSet<Integer> addedTuples, ObjNodeIntProperty on, EdgeEnds ee) {
		candidateEdges.add(on.prop);
		addedTuples.add(on.tupleId);
		ArrayList<EdgeEnds> edges;
		if(candidateGraph.containsKey(on.prop)) {
			edges = candidateGraph.get(on.prop);
			edges.add(ee);
		}
		else {
			edges = new ArrayList<EdgeEnds>();
			edges.add(ee);
		}
		candidateGraph.put(on.prop, edges);
	}

	private void initializeHistorySeenTuplesListAndTargetGraph(ArrayList<Integer> history, HashSet<Integer> seenTuples,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph) {
		// look at the initial partial graph and add edges present in it to seenTuples.
		// also add it to the history.
		// if there are no edges in the partial query graph (only nodes), then seentuples and history are empty.
		// the edges found in the partial graph must be removed from the target graph.
		Iterator<Integer> iter = partialGraph.keySet().iterator();
		while(iter.hasNext()) {
			int src = iter.next();
			HashMap<Integer, ArrayList<DestNode>> propDest = partialGraph.get(src);
			if(propDest != null) {
				Iterator<Integer> iter1 = propDest.keySet().iterator();
				int prop = iter1.next();
				history.add(prop);
				ArrayList<DestNode> dns = propDest.get(prop);
				for(DestNode dn : dns) {
					if(dn.isForwardEdge()) {
						int tid = getTupleID(src, prop, dn.getDest());
						if(tid == -1) {
							System.out.println(src + ", " + prop + ", " + dn.getDest());
							System.out.println("Something is wrong. an edge not found in the src data graph!!!");
							logger.error("Something is wrong. an edge not found in the src data graph!!!");
						}
						else {
							seenTuples.add(tid);
						}
						removeInitialEdgesFromTargetGraph(src, prop, dn.getDest(), targetGraph);
					}
				}
			}
		}
	}

	private void removeInitialEdgesFromTargetGraph(int src, int prop, int dest,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph) {
		gqhelper.removeEdge(src, prop, dest, targetGraph);
		gqhelper.removeEdge(dest, prop, src, targetGraph);
	}

	private int getTupleID(int src, int prop, int dest) {
		int tupleID = -1;
		ArrayList<ObjNodeIntProperty> obs = srcDataGraph.get(src);
		for(ObjNodeIntProperty ob : obs) {
			if(ob.prop == prop && ob.dest == dest)
				tupleID = ob.tupleId;
		}
		return tupleID;
	}

	private ArrayList<EdgeEnds> isPositiveSuggestedEdge(int suggestedEdge, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph) {
		// NOTE: the existence of the edge is a little loose here. A vertex can be connected to multiple instances of an edge label.
		// so if there is an edge say (a_1 -> e_1 -> a_2) in the target graph, and we have made the following suggestion (a_1 -> e_1 -> b_2)
		// then I would still say it is valid, and correct the suggested edge to the exact one in the target graph.
		// REASON to do this: If there are multiple instances of an edge associated with the same vertex, instead of making individual
		// suggestion to each of those instances, we can assume that given an edge associated with that vertex, we can expect the user
		// to identify the correct target node.
		ArrayList<EdgeEnds> positiveEdges = null;
		HashSet<Integer> addedTuples = new HashSet<Integer>();
		Iterator<Integer> iter = partialGraph.keySet().iterator();
		while(iter.hasNext()) {
			int v = iter.next();
			if(targetGraph.containsKey(v)) {
				HashMap<Integer, ArrayList<DestNode>> propDest = targetGraph.get(v);
				if(propDest.containsKey(suggestedEdge)) {
					ArrayList<DestNode> dns = propDest.get(suggestedEdge);
					for(DestNode dn : dns) {
						int src;
						int dest;
						if(dn.isForwardEdge()) {
							src = v;
							dest = dn.getDest();
						}
						else {
							src = dn.getDest();
							dest = v;
						}
						int tupleID = getTupleID(src, suggestedEdge, dest);
						if(!addedTuples.contains(tupleID)) {
							addedTuples.add(tupleID);
							if(positiveEdges == null) {
								positiveEdges = new ArrayList<EdgeEnds>();
							}
							EdgeEnds ee = new EdgeEnds();
							ee.source = src;
							ee.object = dest;
							ee.tupleID = tupleID;
							positiveEdges.add(ee);
						}
					}
				}
			}
		}
		return positiveEdges;
	}

	private void updateSeenTupleIDsListAndCandidateGraph(HashSet<Integer> seenTuples, int prop,
			HashMap<Integer, ArrayList<EdgeEnds>> candidateGraph) {
		// add the newly added tuple IDs into seentuples list.
		// remove the entries associated with "prop" from candidateGraph.
		//System.out.println(prop);
		ArrayList<EdgeEnds> props = candidateGraph.get(prop);
		for(EdgeEnds ee : props) {
			seenTuples.add(ee.tupleID);
		}
		candidateGraph.remove(prop);
	}

	private void learnModel() {
	    //0 = DecisionForest;	1 = Random Forests;	2 = Naive Bayesian model;	3 = Random edge suggestion;
		//4 = recommendation systems (SVD);	5 = RandomSubsets;	6 = SVM;	7 = MEMM model;
		//enum ModelToUseForType {DF, RF, NBC, RAND, SVD, RandSubsts, SVM, MEMM};
		switch(model) {
		case DF:
			dfm = new DecisionForestMain(conf);
			System.out.println("Learning model DECISON FOREST");
			logger.info("Learning model DECISON FOREST");
			//dfm.learnModel();
			break;
		case RF:
			rfp = new RandomForestPredict();
			System.out.println("Learning model Random FOREST");
			logger.info("Learning model Random FOREST");
			rfp.learnModel();
			break;
		case NBC:
			nbc = new NaiveBayesianMain(conf);
			System.out.println("Learning model Naive BAYESIAN");
			logger.info("Learning model Naive BAYESIAN");
			nbc.learnModel();
			break;
		case RAND:
			res = new RandomEdgeSuggestor(conf);
			System.out.println("Learning model RANDOM crazy");
			logger.info("Learning model RANDOM crazy");
			res.learnModel();
			break;
			// do things.
		case SVD:
			svd = new RecommendationSystem();
			System.out.println("Learning model SVD based recommendation system");
			logger.info("Learning model SVD based recommendation system");
			svd.learnModel();
			break;
		case RandSubsets:
			rsm = new RandomSubsetsMain(conf);
			System.out.println("Learning model random subsets");
			logger.info("Learning model random subsets");
			rsm.learnModel();
			break;
		case SVM:
			System.out.println("Learning model SVM");
			logger.info("Learning model SVM");
			break;
			// do things.
		case MEMM:
			System.out.println("Learning model RANDOM FOREST");
			logger.info("Learning model RANDOM FOREST");
			break;
			// do things.
		}

	}

	private int findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
	    //0 = DecisionForest;	1 = Random Forests;	2 = Naive Bayesian model;	3 = Random edge suggestion;
		//4 = recommendation systems (SVD);	5 = RandomSubsets;	6 = SVM;	7 = MEMM model;
		//enum ModelToUseForType {DF, RF, NBC, RAND, SVD, RandSubsets, SVM, MEMM};
		int bestEdge = -1;
		switch(model) {
		case DF:
			//bestEdge = dfm.findBestEdge(history, candidateEdges);
			break;
		case RF:
			bestEdge = rfp.findBestEdge(history, candidateEdges);
			break;
		case NBC:
			bestEdge = nbc.findBestEdge(history, candidateEdges);
			break;
		case RAND:
			bestEdge = RandomEdgeSuggestor.getRandomEdge(candidateEdges);
			break;
		case SVD:
			bestEdge = svd.findBestEdge(history, candidateEdges);
			break;
		case RandSubsets:
			bestEdge = rsm.findBestEdge(history, candidateEdges);
			break;
		case SVM:
			// do things.
		case MEMM:
			// do things.
		}
		return bestEdge;
	}
}
