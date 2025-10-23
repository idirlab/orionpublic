package viiq.graphCompletionGuiMain;

import viiq.graphQuerySuggestionMain.Config;
import viiq.graphQuerySuggestionMain.DestNode;

import it.unimi.dsi.lang.MutableString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import viiq.utils.PropertyKeys;

import viiq.decisionForest.LearnDecisionForest;
import viiq.clientServer.server.LoadData;
import viiq.decisionForest.DecisionForestMain;


public class NewClient {
	final Logger logger = Logger.getLogger(getClass());
	Config conf = null;
	
	public ArrayList<String> warnings;
	public ArrayList<String> errors;
	String errorFilePath;
	
	public void executeNewClient(String[] args, LoadData dataGraph,	LearnDecisionForest decisionForest) {
		try {
			conf = new Config(args[0]);
		}
		catch(ConfigurationException ce) {
			ce.printStackTrace();
		}
		catch(IOException ce) {
			ce.printStackTrace();
		}
		errors = new ArrayList<String>();
		warnings = new ArrayList<String>();
		// Access the rest of the elements in args[]..
		String pg = args[1];
		String hist = args[2];
		/*
		 * suggestion mode tells us if it is active or passive call.
		 * 0 = active
		 * 1 = passive
		 */
		int suggestionMode = Integer.parseInt(args[3]);
		/*
		 * dataGraphInUse, tells us which data graph is used:
		 * 0 = Freebase
		 * 1 = DBpedia
		 * 2 = YAGO
		 */
/*		int dataGraphInUse = Integer.parseInt(args[4]);
		GenerateCandidates gc = new GenerateCandidates(dataGraph);
		HashSet<MutableString> seenTuples = new HashSet<MutableString>();
		HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph = gc.getPartialGraph(pg, seenTuples);
		ArrayList<Integer> history = gc.getHistory(hist, seenTuples);
		// hashmap which maintains the exact triples associated with each edge.
		// key: edge, value: <source, object> pairs of vertices.
		// At least one of source or object must be part of the partial query graph.
		HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap = new HashMap<Integer, ArrayList<CandidateEdgeEnds>>();
		
		HashSet<Integer> candidateEdges;
		if(dataGraphInUse == 0)
			candidateEdges = gc.getCandidateEdgesFreebase(suggestionMode, partialGraph, dataGraph, seenTuples, edgeToTripletMap);
		else if(dataGraphInUse == 1)
			candidateEdges = gc.getCandidateEdgesDBpedia(suggestionMode, partialGraph, dataGraph, seenTuples, edgeToTripletMap);
		else
			candidateEdges = gc.getCandidateEdgesYago(suggestionMode, partialGraph, dataGraph, seenTuples, edgeToTripletMap);
		
		DecisionForestMain dfm = new DecisionForestMain(decisionForest);
		ArrayList<Integer> rankedCandidateEdges = dfm.rankCandidateEdges(history, candidateEdges);
		
		String rankedCandidateTriples = getCandidateTriples(rankedCandidateEdges, edgeToTripletMap);*/
	}
	
	private JSONObject createJsonForGraph() {
		JSONObject json = new JSONObject();
		
		return json;
	}
	
	private void readJsonObject(JSONObject json) {
		
	}
	
	public static void main(String[] args) {
		NewClient nc = new NewClient();
		JSONObject json = nc.createJsonForGraph();
		nc.readJsonObject(json);
	}
}

/*final class CandidateEdgeEnds {
	int source;
	int object;
}*/
