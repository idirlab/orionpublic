package viiq.graphQuerySuggestionMain;

import viiq.graphQuerySuggestionMain.GraphQuerySuggestion.ModelToUse;
import viiq.graphQuerySuggestionMain.GraphTypeQuerySuggestion.ModelToUseForType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import viiq.commons.EdgeEnds;
import viiq.commons.EdgeTypeInfo;

import viiq.clientServer.server.LoadData;

public class GraphQuerySuggestionHelper {
//	final Logger logger;
	
	
	public GraphQuerySuggestionHelper(){
//		logger = Logger.getLogger(getClass());
	}
	
	public void loadEdgeTypeInfo(String inputFilePath, HashMap<Integer, EdgeTypeInfo> edgeType,
			HashMap<Integer, HashSet<Integer>> srcTypeEdges, HashMap<Integer, HashSet<Integer>> objTypeEdges) {
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] edgeStrings = line.split(GraphQuerySuggestionConstants.edgeTypeDelimiter);
					EdgeTypeInfo et = new EdgeTypeInfo();
					if(edgeStrings.length == 3) {
						et.source_type = Integer.parseInt(edgeStrings[0].trim());
						int prop = Integer.parseInt(edgeStrings[1].trim());
						et.object_type = Integer.parseInt(edgeStrings[2].trim());
						// add the required info to edgeType DS.
						edgeType.put(prop, et);
						// update the DS holding info about all the edges coming under a given type.
						// first add wrt the source type.
						if(srcTypeEdges.containsKey(et.source_type)) {
							HashSet<Integer> srcTE = srcTypeEdges.get(et.source_type);
							srcTE.add(prop);
						}
						else {
							HashSet<Integer> srcTE = new HashSet<Integer>();
							srcTE.add(prop);
							srcTypeEdges.put(et.source_type, srcTE);
						}
						// now add wrt the object type.
						if(objTypeEdges.containsKey(et.object_type)) {
							HashSet<Integer> objTE = objTypeEdges.get(et.object_type);
							objTE.add(prop);
						}
						else {
							HashSet<Integer> objTE = new HashSet<Integer>();
							objTE.add(prop);
							objTypeEdges.put(et.object_type, objTE);
						}
					}
					else {
//						logger.info("The edge type is not known correctly for: " + line);
					}
				}
			}
			finally
			{
				br.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> readInputQueryFile(String filePath, 
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph, HashSet<Integer> anchorNodes) {
		ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> allPartialGraphs = 
				new ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>>();
		
		System.out.println("Processing input file: " + filePath);
//		logger.info("Processing input file: " + filePath);
		/*
		 * INPUT file format:
		 * 1) TAB separated anchor nodes list (or null if there are no anchor nodes).
		 * 1) +++++
		 * 2) edges/vertices present in partial query graph (TAB separated edge \t src \t obj edges in each line) or (just a vertex ID).
		 * 		edge1 \t src1 \t obj2
		 * 		src3
		 * 3) +++++
		 * 4) edges present in the target query graph. This HAS to be a super graph of the partial graph above and it HAS to be connected.
		 */
		try {
			// have an integer flag to keep track of what we are looking at now.
			/*
			 * graphPart = 0 (what we are reading now is anchor nodes)
			 * graphPart = 1 (what we are reading now is edges/vertices in partial query graph ONLY)
			 * graphPart = 2 (what we are reading now is edges in target graph alone)
			 */
			int graphPart = 0;
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				if(line.contains("null")){
					// there are no anchor nodes!
				}
				else if(line.startsWith("+")) {
					// just read a delimiter line.
					graphPart++;
				}
				else {
					// this line contains graph information.
					String[] linesplit = line.split("\t");
					if(graphPart == 0) {
						// elements of the line are anchor nodes.
						for(String ele : linesplit) {
							anchorNodes.add(Integer.parseInt(ele));
						}
					}
					else if(graphPart == 1) {
						// elements of the line are for partial graph
						addEdgeToInitialGraph(linesplit, partialGraph);
					}
					else {
						// elements of the line are for target graph
						addEdgeToInitialGraph(linesplit, targetGraph);
						HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> pg = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
						addEdgeToInitialGraph(linesplit, pg);
						allPartialGraphs.add(pg);
					}
				}
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return allPartialGraphs;
	}


	public ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> readInputQueryFile(String filePath, 
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph) {
		ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> allPartialGraphs = 
				new ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>>();
		
		System.out.println("Processing input file: " + filePath);
//		logger.info("Processing input file: " + filePath);
		/*
		 * INPUT file format:
		 * 1) edges/vertices present in partial query graph (TAB separated edge \t src \t obj edges in each line) or (just a vertex ID).
		 * 		edge1 \t src1 \t obj2
		 * 		src3
		 * 2) +++++
		 * 3) edges present in the target query graph. This HAS to be a super graph of the partial graph above and it HAS to be connected.
		 */
		try {
			// have an integer flag to keep track of what we are looking at now.
			/*
			 * graphPart = 1 (what we are reading now is edges/vertices in partial query graph ONLY)
			 * graphPart = 2 (what we are reading now is edges in target graph alone)
			 */
			int graphPart = 1;
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				if(line.startsWith("+")) {
					// just read a delimiter line.
					graphPart++;
				}
				else {
					// this line contains graph information.
					String[] linesplit = line.split("\t");
					if(graphPart == 1) {
						// elements of the line are for partial graph
						addEdgeToInitialGraph(linesplit, partialGraph);
					}
					else {
						// elements of the line are for target graph
						addEdgeToInitialGraph(linesplit, targetGraph);
						HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> pg = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
						addEdgeToInitialGraph(linesplit, pg);
						allPartialGraphs.add(pg);
					}
				}
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return allPartialGraphs;
	}
	
	public ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> readInputQueryFileAddGraphNode(String filePath, 
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph) {
		ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>> allPartialGraphs = 
				new ArrayList<HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>>();
		
		System.out.println("Processing input file: " + filePath);
//		logger.info("Processing input file: " + filePath);
		/*
		 * INPUT file format:
		 * 1) edges/vertices present in partial query graph (TAB separated edge \t src \t obj edges in each line) or (just a vertex ID).
		 * 		edge1 \t src1 \t obj2
		 * 		src3
		 * 2) +++++
		 * 3) edges present in the target query graph. This HAS to be a super graph of the partial graph above and it HAS to be connected.
		 */
		try {
			// have an integer flag to keep track of what we are looking at now.
			/*
			 * graphPart = 1 (what we are reading now is edges/vertices in partial query graph ONLY)
			 * graphPart = 2 (what we are reading now is edges in target graph alone)
			 */
			int graphPart = 1;
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;

			HashMap<Integer, Integer> nodeToGraphNode = new HashMap<Integer, Integer>();

			while((line = br.readLine()) != null) {
				if(line.startsWith("+")) {
					// just read a delimiter line.
					graphPart++;
				}
				else {
					// this line contains graph information.
					String[] linesplit = line.split("\t");
					if(graphPart == 1) {
						// elements of the line are for partial graph
						addEdgeToInitialGraphAddGraphNode(linesplit, partialGraph, nodeToGraphNode);
					}
					else {
						// elements of the line are for target graph
						addEdgeToInitialGraphAddGraphNode(linesplit, targetGraph, nodeToGraphNode);
						HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> pg = new HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>();
						addEdgeToInitialGraphAddGraphNode(linesplit, pg, nodeToGraphNode);
						allPartialGraphs.add(pg);
					}
				}
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return allPartialGraphs;
	}

	private void addEdgeToInitialGraph(String[] eles, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> graph) {
		if(eles.length == 1){
			// this contains only a node and not edge-related info.
			graph.put(Integer.parseInt(eles[0]), null);
		}
		else if(eles.length == 3) {
			int prop = Integer.parseInt(eles[0]);
			int src = Integer.parseInt(eles[1]);
			int dest = Integer.parseInt(eles[2]);
			// add the forward edge.
			DestNode dn = new DestNode();
			dn.setDest(dest);
			dn.setForwardEdge(true);
			addEdge(src, prop, dn, graph);
			
			// add the reverse edge.
			DestNode revdn = new DestNode();
			revdn.setDest(src);
			revdn.setForwardEdge(false);
			addEdge(dest, prop, revdn, graph);
		}
		else {
			System.out.println("ERROR: The input information is not compatible with the protocol used.");
//			logger.error("The input information is not compatible with the protocol used.");
		}
	}
	
	private void addEdgeToInitialGraphAddGraphNode(String[] eles, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> graph, HashMap<Integer, Integer> nodeToGraphNode) {
		if(eles.length == 1){
			// this contains only a node and not edge-related info.
			graph.put(Integer.parseInt(eles[0]), null);
		}
		else if(eles.length == 3) {
			int prop = Integer.parseInt(eles[0]);
			int src = Integer.parseInt(eles[1]);
			int dest = Integer.parseInt(eles[2]);

			// add the forward edge.
			DestNode dn = new DestNode();
			dn.setDest(dest);
			dn.setForwardEdge(true);
			if(!nodeToGraphNode.containsKey(dest)) {
				nodeToGraphNode.put(dest, nodeToGraphNode.size()+1);
			}
			dn.setDestGraphNodeID(nodeToGraphNode.get(dest));
			addEdge(src, prop, dn, graph);
			
			// add the reverse edge.
			DestNode revdn = new DestNode();
			revdn.setDest(src);
			revdn.setForwardEdge(false);
			if(!nodeToGraphNode.containsKey(src)) {
				nodeToGraphNode.put(src, nodeToGraphNode.size()+1);
			}
			revdn.setDestGraphNodeID(nodeToGraphNode.get(src));
			addEdge(dest, prop, revdn, graph);
		}
		else {
			System.out.println("ERROR: The input information is not compatible with the protocol used.");
//			logger.error("The input information is not compatible with the protocol used.");
		}
	}
	
	private void addEdge(int src, int prop, DestNode dn, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> graph) {
		HashMap<Integer, ArrayList<DestNode>> propDest;
		ArrayList<DestNode> dns;
		if(graph.containsKey(src)) {
			propDest = graph.get(src);
			if(propDest == null) {
				propDest = new HashMap<Integer, ArrayList<DestNode>>();
				dns = new ArrayList<DestNode>();
				dns.add(dn);
				propDest.put(prop, dns);
				graph.put(src, propDest);
			}
			else {
				if(propDest.containsKey(prop)) {
					dns = propDest.get(prop);
					dns.add(dn);
				}
				else {
					dns = new ArrayList<DestNode>();
					dns.add(dn);
				}
				propDest.put(prop, dns);
			}
		}
		else {
			propDest = new HashMap<Integer, ArrayList<DestNode>>();
			dns = new ArrayList<DestNode>();
			dns.add(dn);
			propDest.put(prop, dns);
			graph.put(src, propDest);
		}
	}
	 
	public ModelToUse getModelToUse(int mod){
		ModelToUse model;
		if(mod == 0)
			model = ModelToUse.DF;
		if(mod == 1)
			model = ModelToUse.RF;
		else if(mod == 2)
			model = ModelToUse.NBC;
		else if(mod == 3)
			model = ModelToUse.RAND;
		else if(mod == 4)
			model = ModelToUse.SVM;
		else if(mod == 5)
			model = ModelToUse.MEMM;
		else {
			// mark MEMM as the default model if the properties file has an unrecognized model.
			System.out.println("The properties file has an unrecognized model. Using Bayesian as default!");
//			logger.warn("The properties file has an unrecognized model. Using Bayesian as default!");
			model = ModelToUse.NBC;
		}
		return model;
	}
	
	public ModelToUseForType getModelToUseForType(int mod){
		//0 = DecisionForest;	1 = Random Forests;	2 = Naive Bayesian model;	3 = Random edge suggestion;	
		//4 = recommendation systems (SVD);	5 = RandomSubsets;	6 = SVM;	7 = MEMM model;	
		//enum ModelToUseForType {DF, RF, NBC, RAND, SVD, RandSubsets, SVM, MEMM};
		ModelToUseForType model;
		if(mod == 0)
			model = ModelToUseForType.DF;
		else if(mod == 1)
			model = ModelToUseForType.RF;
		else if(mod == 2)
			model = ModelToUseForType.NBC;
		else if(mod == 3)
			model = ModelToUseForType.RAND;
		else if(mod == 4)
			model = ModelToUseForType.SVD;
		else if(mod == 5)
			model = ModelToUseForType.RandSubsets;
		else if(mod == 6)
			model = ModelToUseForType.SVM;
		else if(mod == 7)
			model = ModelToUseForType.MEMM;
		else {
			// mark NBC as the default model if the properties file has an unrecognized model.
			System.out.println("The properties file has an unrecognized model. Using Bayesian as default!");
//			logger.warn("The properties file has an unrecognized model. Using Bayesian as default!");
			model = ModelToUseForType.RandSubsets;
		}
		return model;
	}
	
	public void addEdgesToPartialGraph(int prop, ArrayList<EdgeEnds> positiveEdges, 
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph) {
		// add all the tuples present in positiveEdges to partialGraph. Note that, there is only one prop, but there might be
		// multiple instances.
		for(EdgeEnds ee : positiveEdges) {
			int src = ee.source;
			int dest = ee.object;
			int srcgn = ee.graphSource;
			int destgn = ee.graphObject;
			// add the forward edge.
			DestNode dn = new DestNode();
			dn.setDest(dest);
			dn.setForwardEdge(true);
			dn.setDestGraphNodeID(destgn);
			addEdge(src, prop, dn, partialGraph);
			
			// add the reverse edge.
			DestNode revdn = new DestNode();
			revdn.setDest(src);
			revdn.setForwardEdge(false);
			revdn.setDestGraphNodeID(srcgn);
			addEdge(dest, prop, revdn, partialGraph);
		}
	}
	
	public void removeEdge(int src, int prop, int dest, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> graph) {
		HashMap<Integer, ArrayList<DestNode>> propDest = graph.get(src);
		if(propDest != null) {
			ArrayList<DestNode> dns = propDest.get(prop);
			int i;
			for(i=0; i<dns.size(); i++) {
				DestNode dn = dns.get(i);
				if(dn.getDest() == dest)
					break;
			}
			if(i < dns.size())
				dns.remove(i);
			if(dns.isEmpty()) {
				propDest.remove(prop);
			}
		}
		if(propDest.isEmpty()) {
			graph.remove(src);
		}
	}
	
	public void removeEdgesFromTargetGraph(int prop, ArrayList<EdgeEnds> positiveEdges, 
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> targetGraph) {
		// remove edges present in positiveEdges from target graph.
		for(EdgeEnds ee : positiveEdges) {
			int src = ee.source;
			int dest = ee.object;
			removeEdge(src, prop, dest, targetGraph);
			// remove reverse edge
			removeEdge(dest, prop, src, targetGraph);
		}
	}
	
	public void printConstructedGraph(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>  graph, String outputTargetQueryFilePath) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputTargetQueryFilePath));
			Iterator<Integer> iter = graph.keySet().iterator();
			while(iter.hasNext()) {
				int src = iter.next();
				HashMap<Integer, ArrayList<DestNode>> vertices = graph.get(src);
				Iterator<Integer> propiter = vertices.keySet().iterator();
				while(propiter.hasNext()) {
					int prop = propiter.next();
					ArrayList<DestNode> props = vertices.get(prop);
					for(DestNode dn : props) {
						if(dn.isForwardEdge()) {
							bw.write(prop + " , " + src + " : " + dn.getDest() + "\n");
							System.out.println(prop + " , " + src + " : " + dn.getDest());
							//logger.debug(prop + " , " + src + " : " + dn.getDest());
						}
					}
				}
			}
			bw.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}


	public void printConstructedGraphLabel(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>>  graph) {
		LoadData ldlm = new LoadData();
		try {
			Iterator<Integer> iter = graph.keySet().iterator();
			while(iter.hasNext()) {
				int src = iter.next();
				HashMap<Integer, ArrayList<DestNode>> vertices = graph.get(src);
				Iterator<Integer> propiter = vertices.keySet().iterator();
				while(propiter.hasNext()) {
					int prop = propiter.next();
					ArrayList<DestNode> props = vertices.get(prop);
					for(DestNode dn : props) {
						if(dn.isForwardEdge()) {
							System.out.println(ldlm.getEdgeLabelIndex().get(prop) + " , " + ldlm.getNodeLabelIndex().get(src) + " : " + ldlm.getNodeLabelIndex().get(dn.getDest()));
							//logger.debug(prop + " , " + src + " : " + dn.getDest());
						}
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}

/*final class EdgeEnds {
	int source;
	int object;
	int tupleID;
}*/