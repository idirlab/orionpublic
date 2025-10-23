package viiq.graphCompletionGuiMain;

import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Arrays;

import viiq.backendHelper.SpringClientHelper;
import viiq.clientServer.client.BackendResponseObject;
import viiq.clientServer.server.LoadData;
import viiq.commons.CandidateEdgeEnds;
import viiq.commons.EdgeTypeInfo;
import viiq.commons.GuiEdgeInfo;
import viiq.commons.GuiEdgeStringInfo;
import viiq.commons.IntermediateNodeAndOtherEnd;
import viiq.commons.ObjNodeIntProperty;
import viiq.commons.CandidateEdgeScore;
import viiq.graphQuerySuggestionMain.Config;
import viiq.graphQuerySuggestionMain.DestNode;
import viiq.utils.BufferedRandomAccessFile;
import viiq.utils.PropertyKeys;

public class GenerateCandidatesNew {

	boolean isFreebaseDataset = false;
	boolean isDbpediaDataset = false;
	boolean isYagoDataset = false;

	// set of nodes in the graph that are intermediate nodes.
	HashSet<Integer> intermediateNodesList = new HashSet<Integer>();
	// key = concatenation of two edges connecting an intermediate node, value = new edge ID corresponding to the key
	HashMap<String, Integer> concatenatedStringEdgesToNewEdgeIdMap = new HashMap<String, Integer>();
	// contains labels of edges. key: edge ID, value: label
	HashMap<Integer, String> edgeLabel = new HashMap<Integer, String>();
	// contains labels of nodes (domains, type, edgetype, entities). This assumes all IDs are unique!
	// key: node ID, value: node label
	HashMap<Integer, String> nodeLabel = new HashMap<Integer, String>();
	// key: type, value: edges whose source type is the key. (generated using all the instances of "type" in the data graph)
	HashMap<Integer, HashSet<Integer>> sourceTypesToEdgesMap = new HashMap<Integer, HashSet<Integer>>();
	// key: type, value: edges whose object type is the key. (generated using all the instances of "type" in the data graph)
	HashMap<Integer, HashSet<Integer>> objectTypesToEdgesMap = new HashMap<Integer, HashSet<Integer>>();
	// key = edge, value = (source vertex Type, dest vertex Type)
	HashMap<Integer, EdgeTypeInfo> edgeType = new HashMap<Integer, EdgeTypeInfo>();

	// key: graphNodeID, value: (key: edgeID, value: all the destination nodes with their graphNodeIDs)
	HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialQueryGraph = new HashMap<Integer, HashMap<Integer,ArrayList<DestNode>>>();
	// key: graphNodeID, value: (key: edgeID, value: all the destination nodes with their graphNodeIDs)
	HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> rejectedQueryGraph = new HashMap<Integer, HashMap<Integer,ArrayList<DestNode>>>();
	// key: graphNodeID, value: type/entity ID of the corresponding node.
	HashMap<Integer, Integer> graphNodeToNodeMap = new HashMap<Integer, Integer>();
	// key: type/entity ID of a node in GUI, value: the list of graphNodeIDs that have the particular node label/value.
	HashMap<Integer, HashSet<Integer>> nodeToGraphNodeMap = new HashMap<Integer, HashSet<Integer>>();
	//key: graph node id, value: type values of graph node
	HashMap<Integer, HashSet<Integer>> nodeTypeValues = new HashMap<Integer, HashSet<Integer>>();
	//key: graph node id, value: entity constrains that come from
	HashMap<Integer, HashSet<String>> adjacentEntities = new HashMap<Integer, HashSet<String>>();

	BufferedRandomAccessFile sourceDataGraphFileHandler;
	BufferedRandomAccessFile objectDataGraphFileHandler;
	int numOfTotalEdges;

	public GenerateCandidatesNew(Config conf) {
		edgeType = LoadData.getEdgeType();
		intermediateNodesList = LoadData.getIntermediateNodesList();
		concatenatedStringEdgesToNewEdgeIdMap = LoadData.getConcatenatedStringEdgesToNewEdgeIdMap();
		edgeLabel = LoadData.getEdgeLabelIndex();
		nodeLabel = LoadData.getNodeLabelIndex();
		sourceTypesToEdgesMap = LoadData.getSourceTypesToEdgesMap();
		objectTypesToEdgesMap = LoadData.getObjectTypesToEdgesMap();

		try{
			numOfTotalEdges = Integer.parseInt(conf.getProp(PropertyKeys.datagraphNumberOfLines));
			sourceDataGraphFileHandler = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile), "r", numOfTotalEdges, conf);
			objectDataGraphFileHandler = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphObjectAlignedFile), "r", numOfTotalEdges, conf);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private HashSet<Integer> getCandidateEdgesYago(int suggestionMode, CandidateEdgeEnds activeModePartialGraph,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		isYagoDataset = true;
		return candidateEdges;
	}

	private HashSet<Integer> getCandidateEdgesDBpedia(int suggestionMode, CandidateEdgeEnds activeModePartialGraph,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		isDbpediaDataset = true;
		return candidateEdges;
	}

	private HashSet<Integer> getCandidateEdgesFreebase(ArrayList<GuiEdgeInfo> pg, int suggestionMode,CandidateEdgeEnds activeModePartialGraph,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, int selectedGraphNode, LinkedHashMap<Integer, HashSet<Integer>> evaluatedValues, String systemName, Config conf) {
		// for(int node : evaluatedValues.keySet()) {
		// 	System.out.println("nodevalues = "+evaluatedValues.get(node).size());
		// }

		//System.out.println("generating candidate edges for freebase");

		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		isFreebaseDataset = true;
		if(suggestionMode == 0 || suggestionMode == -1) {
			// Active event
			getCandidatesActiveMode(pg, activeModePartialGraph, candidateEdges, edgeToTripletMap, evaluatedValues, systemName, conf);
		}
		else if(suggestionMode == 1) {
			// Passive event
			getCandidatesPassiveMode(pg, candidateEdges, edgeToTripletMap, evaluatedValues, systemName, conf);
		} else if(suggestionMode == 2) {
			// Event where an orange node is selected and Refresh Suggestions button is clicked on!
			getCandidatesPassiveModeForSelectedNode(pg, candidateEdges, edgeToTripletMap, selectedGraphNode, evaluatedValues, systemName, conf);
		}
		return candidateEdges;
	}

	private void addConcatenatedEdgeCandidatesForEntities(int vertex, HashSet<Integer> srcCandidates, HashSet<Integer> objCandidates) {
		if(vertex != 0 && !sourceTypesToEdgesMap.containsKey(vertex) && !objectTypesToEdgesMap.containsKey(vertex)) {
			// do this only if this is an entity node! (NOT type node)
			ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
			addEdgeTypeEdge(ons, vertex, srcCandidates, objCandidates);

			ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
			addEdgeTypeEdge(revons, vertex, srcCandidates, objCandidates);
		}
	}

	private void getCandidatesActiveMode(ArrayList<GuiEdgeInfo> pg, CandidateEdgeEnds activeModePartialGraph,
			HashSet<Integer> candidateEdges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, LinkedHashMap<Integer, HashSet<Integer>> evaluatedValues, String systemName, Config conf) {
		int srcVertex = activeModePartialGraph.source;
		int srcGraphNode = activeModePartialGraph.graphSource;
		int objVertex = activeModePartialGraph.object;
		int objGraphNode = activeModePartialGraph.graphObject;

		SpringClientHelper scp = new SpringClientHelper(conf);


		HashSet<Integer> srcVertexEdgeSource = getEdgesForVertex(pg, srcVertex, srcGraphNode, null, true, evaluatedValues.get(srcGraphNode), systemName, conf);
		HashSet<Integer> srcVertexEdgeObject = getEdgesForVertex(pg, srcVertex, srcGraphNode, null, false, evaluatedValues.get(srcGraphNode), systemName, conf);
		addConcatenatedEdgeCandidatesForEntities(srcVertex, srcVertexEdgeSource, srcVertexEdgeObject);

		HashSet<Integer> objVertexEdgeSource = getEdgesForVertex(pg, objVertex, objGraphNode, null, true, evaluatedValues.get(objGraphNode), systemName, conf);
		HashSet<Integer> objVertexEdgeObject = getEdgesForVertex(pg, objVertex, objGraphNode, null, false, evaluatedValues.get(objGraphNode), systemName, conf);
		addConcatenatedEdgeCandidatesForEntities(objVertex, objVertexEdgeSource, objVertexEdgeObject);
		/**
		 * For an edge to be a valid candidate, it has to be in the intersection of one of the following cases:
		 * 1) srcVertexEdgeSource and objVertexEdgeObject OR
		 * 2) srcVertexEdgeObject and objVertexEdgeSource
		 */
		HashSet<Integer> otherEdgeComparator;
		Iterator<Integer> iter;

		//if(srcVertexEdgeSource.contains(47185552))
		//	System.out.println("src-src");
		//if(srcVertexEdgeObject.contains(47185552))
                //        System.out.println("src-obj");
		//if(objVertexEdgeSource.contains(47185552))
                //        System.out.println("obj-src");
                //if(objVertexEdgeObject.contains(47185552))
                //        System.out.println("obj-obj");

		// System.out.println("srcsrc has edge the edge "+srcVertexEdgeSource.contains(47183530));
		// System.out.println("srcobj has edge the edge "+srcVertexEdgeObject.contains(47183530));
		// System.out.println("objsrc has edge the edge "+objVertexEdgeSource.contains(47183530));
		// System.out.println("objobj has edge the edge "+objVertexEdgeObject.contains(47183530));

		iter = srcVertexEdgeSource.size() < objVertexEdgeObject.size() ? srcVertexEdgeSource.iterator() : objVertexEdgeObject.iterator();
		otherEdgeComparator = srcVertexEdgeSource.size() < objVertexEdgeObject.size() ? objVertexEdgeObject : srcVertexEdgeSource;
		while(iter.hasNext()) {
			int e = iter.next();
			if(otherEdgeComparator.contains(e)) {
				candidateEdges.add(e);
				addToEdgeTripletMap(e, srcVertex, srcGraphNode, objVertex, objGraphNode, edgeToTripletMap);
			}
		}

		iter = srcVertexEdgeObject.size() < objVertexEdgeSource.size() ? srcVertexEdgeObject.iterator() : objVertexEdgeSource.iterator();
		otherEdgeComparator = srcVertexEdgeObject.size() < objVertexEdgeSource.size() ? objVertexEdgeSource : srcVertexEdgeObject;
		while(iter.hasNext()) {
			int e = iter.next();
			if(otherEdgeComparator.contains(e)) {
				if(edgeToTripletMap.containsKey(e)) {
					//this means the  edgetype e can be suggested in two ways, 
					//therefore to disambiguate we will only consider the direction the user drew the edge
					continue; 
				}
				candidateEdges.add(e);
				addToEdgeTripletMap(e, objVertex, objGraphNode, srcVertex, srcGraphNode, edgeToTripletMap);
			}
		}
		//if(candidateEdges.contains(47185552))
                //        System.out.println("found in candidates");
		//else
		//	System.out.println("NOT found in candidates");
		System.out.println("Number of edges for "+srcGraphNode+" = " + Integer.toString(srcVertexEdgeSource.size()+srcVertexEdgeObject.size()));
		System.out.println("Number of edges for "+objGraphNode+" = " + Integer.toString(objVertexEdgeSource.size()+objVertexEdgeObject.size()));
		System.out.println("Number of common edges = " + candidateEdges.size());
	}

	private void addToEdgeTripletMap(int edge, int source, int graphSrc, int object, int graphObj,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		CandidateEdgeEnds ce = new CandidateEdgeEnds();
		ce.graphSource = graphSrc;
		ce.graphObject = graphObj;
		ce.source = source;
		ce.object = object;
		ArrayList<CandidateEdgeEnds> cee;
		if(edgeToTripletMap.containsKey(edge)) {
			cee = edgeToTripletMap.get(edge);
			cee.add(ce);
		}
		else {
			cee = new ArrayList<CandidateEdgeEnds>();
			cee.add(ce);
			edgeToTripletMap.put(edge, cee);
		}
	}

	/**
	 * This method is invoked when an orange node is selected, AND the Refresh Suggestions button is clicked on.
	 * The new candidates are those edges that are incident only on that selected node (graphNode).
	 * @param candidateEdges
	 * @param edgeToTripletMap
	 * @param graphNode
	 */
	private void getCandidatesPassiveModeForSelectedNode(ArrayList<GuiEdgeInfo> pg, HashSet<Integer> candidateEdges,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, int graphNode, LinkedHashMap<Integer, HashSet<Integer>> evaluatedValues, String systemName, Config conf) {
	//	System.out.println("Getting candidates for a selected node, on click of Refresh suggestions");
		HashSet<MutableString> addedTuples = new HashSet<MutableString>();
		if(!graphNodeToNodeMap.containsKey(graphNode)) {
			System.out.println("ERROR " + graphNode + " is not found in our map");
		} else {
			int vertex = graphNodeToNodeMap.get(graphNode);
			System.out.println(" Selected VERTEX = " + vertex + " with graph node id " + graphNode);
			if((sourceTypesToEdgesMap.containsKey(vertex) || objectTypesToEdgesMap.containsKey(vertex))) {
				SpringClientHelper scp = new SpringClientHelper(conf);
				// This vertex is an EdgeType node.
				addTypeNodeNeighborEdgesEdgetype(pg, vertex, graphNode, candidateEdges, edgeToTripletMap, addedTuples, evaluatedValues.get(graphNode), systemName, conf);
				
			}
			else {
				// if this vertex is an intermediate node, don't bother adding anything.
				if(!isFreebaseDataset || !intermediateNodesList.contains(vertex)) {
					// this vertex is an actual entity node from the data graph.
					// add all those edges where vertex is the source...
					ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
					boolean isForwardEdge = true;
					if(ons != null) {
						getCandidateEdgeForEntity(candidateEdges, addedTuples, vertex, graphNode, isForwardEdge, ons, edgeToTripletMap);
					}
					// add all those edges where vertex is the object...
					isForwardEdge = false;
					ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
					if(revons != null) {
						getCandidateEdgeForEntity(candidateEdges, addedTuples, vertex, graphNode, isForwardEdge, revons, edgeToTripletMap);
					}
					
				}
			}
		}
	}

	public void getCandidatesPassiveMode(ArrayList<GuiEdgeInfo> pg, HashSet<Integer> candidateEdges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, LinkedHashMap<Integer, HashSet<Integer>> evaluatedValues, String systemName, Config conf) {
		// for every node present in the initial partial graph, add the neighboring edges (not in the partial graph) to the candidate
		// graph. also add these edges to candidate edges list.

		boolean allSingleNode = true;

		//System.out.println("generating candidate edges for passive node");

		//checking if all nodes in partial graph are single nodes
		for (HashMap.Entry<Integer, HashMap<Integer, ArrayList<DestNode>>> entry : partialQueryGraph.entrySet()) {
			if(entry.getValue() != null) {
				allSingleNode = false;
			}
		}


		HashSet<MutableString> addedTuples = new HashSet<MutableString>();
		if(partialQueryGraph.isEmpty()) {
			// The input could be empty. All edges are candidate edges. This is NOT a valid case at the moment in the GUI
			if(candidateEdges.isEmpty()) {
				// this is the first time we are even coming to this method. just populate all edges into candidate edges, until we
				// hit at least one positive edge suggestion.
				// if this is not the first time we have come in here, then the set of candidate edges is already populated.
				// the just suggested edge was also removed.
				Iterator<Integer> iter = edgeType.keySet().iterator();
				while(iter.hasNext()) {
					candidateEdges.add(iter.next());
				}
			}
		} else if(allSingleNode) {
			/*
			 * Check if the partial query graph has only one entry, it means it only contains a single node in it. This is the case where
			 * the user has just drawn a new node and selected its value and the passive mode kicked in.
			 *
			 * ADD all the possible edges that are incident on the node in the partial query graph as the input.
			 */

			Iterator<Integer> iter = partialQueryGraph.keySet().iterator();

			SpringClientHelper scp = new SpringClientHelper(conf);

			while(iter.hasNext()) {
				int graphNode = iter.next();
				if(!graphNodeToNodeMap.containsKey(graphNode)) {
			//		System.out.println("ERROR " + graphNode + " is not found in our map");
				} else {
					int vertex = graphNodeToNodeMap.get(graphNode);
					// The flag value mentioned in the last parameter does NOT matter since that flag is used only for active mode and NOT Passive mode.
					//System.out.println(evaluatedValues.size()+"---->"+graphNode+"---->"+evaluatedValues.keySet());
					HashSet<Integer> tmp = getEdgesForVertex(pg, vertex, graphNode, edgeToTripletMap, false, evaluatedValues.get(graphNode), systemName, conf);
					Iterator<Integer> tmpIter = tmp.iterator();
					while(tmpIter.hasNext()) {
						candidateEdges.add(tmpIter.next());
					}
				}
		 	}
		} else {
			Iterator<Integer> iter = partialQueryGraph.keySet().iterator();
			SpringClientHelper scp = new SpringClientHelper(conf);
			while(iter.hasNext()) {
				int graphNode = iter.next();
				long start = System.currentTimeMillis();
				if(!graphNodeToNodeMap.containsKey(graphNode)) {
					//System.out.println("ERROR " + graphNode + " is not found in our map");
				} else {
					
					int vertex = graphNodeToNodeMap.get(graphNode);
					//System.out.println(" NEW VERTEX = ========================== " + vertex + " with graph node id " + graphNode);
					if((sourceTypesToEdgesMap.containsKey(vertex) || objectTypesToEdgesMap.containsKey(vertex))) {
						// This vertex is an EdgeType node.
						//System.out.println(vertex+", "+graphNode);
						//System.out.println("Generating suggestions for type node " + vertex + " in connected graph.");
						addTypeNodeNeighborEdgesEdgetype(pg, vertex, graphNode, candidateEdges, edgeToTripletMap, addedTuples, evaluatedValues.get(graphNode), systemName, conf);
						//System.out.println("candidate edges after type node "+graphNode+" = "+candidateEdges.size());
					}
					else {
						// if this vertex is an intermediate node, don't bother adding anything.
						if(isFreebaseDataset && intermediateNodesList.contains(vertex))
							continue;

						// this vertex is an actual entity node from the data graph.
						// add all those edges where vertex is the source...
						//System.out.println("Generating suggestions for entity node " + vertex + " in connected graph.");
						ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
						boolean isForwardEdge = true;
						if(ons != null) {
							getCandidateEdgeForEntity(candidateEdges, addedTuples, vertex, graphNode, isForwardEdge, ons, edgeToTripletMap);
						}
						// add all those edges where vertex is the object...
						isForwardEdge = false;
						ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
						if(revons != null) {
							getCandidateEdgeForEntity(candidateEdges, addedTuples, vertex, graphNode, isForwardEdge, revons, edgeToTripletMap);
						}
						//System.out.println("candidate edges after entity node "+graphNode+" = "+candidateEdges.size());
					}
				}
				//System.out.println("Time elapsed in generating candidate edges for this node = "+ (System.currentTimeMillis()-start));
			}
		}
	}

	public LinkedHashSet<Integer> filterByEdgeConstraint(LinkedHashSet<Integer> entitySet, HashSet<String> neighbors) {
		LinkedHashSet<Integer> entityConstrain = null;
		//System.out.println(neighbors);
		//System.out.println("size before = "+entitySet.size());
		System.out.println(neighbors);
		if(neighbors != null) {
			for (String n: neighbors) {
				int v = Integer.parseInt(n.split(",")[0]);
				int e = Integer.parseInt(n.split(",")[1]);
				boolean isForwardEdge = Boolean.parseBoolean(n.split(",")[2]);
				//System.out.println("isForwardEdge = "+isForwardEdge+","+n.split(",")[2]);
				if(isForwardEdge) {
					entityConstrain = objectDataGraphFileHandler.getVertexNeighborsForSpecificEdge(v, e);
				} else {
					entityConstrain = sourceDataGraphFileHandler.getVertexNeighborsForSpecificEdge(v, e);
				}
				entitySet.retainAll(entityConstrain);
			}
		}
		//System.out.println("size after = "+entitySet.size());

		return entitySet;
	}

	public HashSet<Integer> getCandidateEdgeEnds(int edge, Config conf) {
		BufferedRandomAccessFile braf = null;
		try {
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.datagraphPredicateAlignedLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.datagraphPredicateAlignedNumberOfLines));
			braf = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphPredicateAligned), "r", numOfLines, bufferSize);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		HashSet<Integer> result = braf.getEntitiesForEdge(edge);
		try {
			braf.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	private void addTypeNodeNeighborEdgesEdgetype(ArrayList<GuiEdgeInfo> pg, int edgeType, int graphNode, HashSet<Integer> candidateEdges,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, HashSet<MutableString> addedTuples, HashSet<Integer> nodeValues, String systemName, Config conf) {

		SpringClientHelper scp = new SpringClientHelper(conf);

		if(graphNode==2 && edgeType==1069) System.out.println("graphNode="+graphNode+" edgeType="+edgeType+" has nodeValues="+nodeValues.size());

		//check if the node has only one typeValue
		boolean singleTypeValueNode = false;
		for(GuiEdgeInfo e: pg) {
			if(e.graphSource == graphNode && scp.getListFromString(e.sourceTypeValues).size() == 1) singleTypeValueNode = true;
			else if(e.graphObject == graphNode && scp.getListFromString(e.objectTypeValues).size() == 1) singleTypeValueNode = true;
		}

		HashSet<Integer> srcEdges = singleTypeValueNode ? sourceTypesToEdgesMap.get(edgeType) : getEdgesForEntities(nodeValues, true, systemName);
		HashSet<Integer> objEdges = singleTypeValueNode ? objectTypesToEdgesMap.get(edgeType) : getEdgesForEntities(nodeValues, false, systemName);

		// System.out.println("singleTypeValueNode = "+singleTypeValueNode);
		// System.out.println("type = "+edgeType);
		// System.out.println("srcEdges = "+srcEdges.size());
		// System.out.println("objEdges = "+objEdges.size());

		// System.out.println("candidateEdges before "+ candidateEdges.size());

		getCandidateEdgeForType(edgeType, graphNode, candidateEdges, srcEdges, addedTuples, true, edgeToTripletMap);

		//System.out.println("candidateEdges after src edges "+ candidateEdges.size());
		getCandidateEdgeForType(edgeType, graphNode, candidateEdges, objEdges, addedTuples, false, edgeToTripletMap);
		//System.out.println("candidateEdges after obj edges "+ candidateEdges.size());
		//System.out.println("pg size = "+pg.size()+", candidateEdges = "+candidateEdges.size()+", edgeToTripletMap = "+edgeToTripletMap+", nodeValues = "+nodeValues.size());
	}

	private void getCandidateEdgeForType(int vertex, int graphNode, HashSet<Integer> candidateEdges, HashSet<Integer> edges,
			HashSet<MutableString> addedTuples, boolean isForwardEdge, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		if(edges != null && !edges.isEmpty()) {
			Iterator<Integer> iter = edges.iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				if(!edgeType.containsKey(edge))
					continue;
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				if(isForwardEdge) {
					cee.graphSource = graphNode;
					cee.source = vertex;
					cee.graphObject = -1;
					cee.object = edgeType.get(edge).object_type;
				} else {
					cee.graphObject = graphNode;
					cee.object = vertex;
					cee.graphSource = -1;
					cee.source = edgeType.get(edge).source_type;
				}

				MutableString tid = getTupleID(cee.graphSource, edge, cee.graphObject);

				//if(!addedTuples.contains(tid)) {
				//if(!addedTuples.contains(tid) && isValidCandidateEdge(cee.graphSource, edge, cee.graphObject)) {

				if(edgeToTripletMap.containsKey(edge)) {
					edgeToTripletMap.get(edge).add(cee);
				}
				else {
					ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
					ce.add(cee);
					edgeToTripletMap.put(edge, ce);
				}
				addToCandidateGraph(candidateEdges, addedTuples, edge, tid);
				//}
			}
		}
	}

	private void getCandidateEdgeForEntity(HashSet<Integer> candidateEdges, HashSet<MutableString> addedTuples, int vertex, int graphNode,
			boolean isForwardEdge, ArrayList<ObjNodeIntProperty> ons, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		for(ObjNodeIntProperty on : ons) {
			if(isFreebaseDataset && intermediateNodesList.contains(on.dest)) {
				// get other nodes connected to this intermediate node.
				concatenateEdgesAndAddProp(candidateEdges, addedTuples, vertex, graphNode, on, edgeToTripletMap);
			} else {
				if(!edgeType.containsKey(on.prop))
					continue;
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				//edited by ss
				//cee.source = isForwardEdge == true ? vertex : on.dest;
				cee.source = isForwardEdge == true ? vertex : edgeType.get(on.prop).source_type;
				cee.graphSource = isForwardEdge == true ? graphNode : -1;
				//edited by ss
				//cee.object = isForwardEdge == true ? on.dest : vertex;
				cee.object = isForwardEdge == true ? edgeType.get(on.prop).object_type : vertex;
				cee.graphObject = isForwardEdge == true ? -1 : graphNode;
				MutableString tid = getTupleID(cee.graphSource, on.prop, cee.graphObject);
				/*if(vertex == 19243248) {
					System.out.println("In get candidates for 19243248 earth : ( " + cee.source + ", " + on.prop + ", " + cee.object + " )");
					System.out.print("-- " + cee.graphSource + ", " + on.prop + ", " + cee.graphObject);
				}*/

				if(!addedTuples.contains(tid)) {
				//if(!addedTuples.contains(tid) && isValidCandidateEdge(cee.graphSource, on.prop, cee.graphObject)) {
					/*if(vertex == 19243248) {
						System.out.print(" selected ");
					}*/
					if(edgeToTripletMap.containsKey(on.prop) && !edgeToTripletMap.get(on.prop).contains(cee)) { //avoid duplication in the map
						edgeToTripletMap.get(on.prop).add(cee);
					}
					else {
						ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
						ce.add(cee);
						edgeToTripletMap.put(on.prop, ce);
					}
					addToCandidateGraph(candidateEdges, addedTuples, on.prop, tid);
				}
				/*if(vertex == 19243248) {
					System.out.println();
				}*/
			}
		}
	}

	private void concatenateEdgesAndAddProp(HashSet<Integer> candidateEdges, HashSet<MutableString> addedTuples, int vertex,
			int graphNode, ObjNodeIntProperty on, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		ArrayList<IntermediateNodeAndOtherEnd> intermediateEdges = getOtherEdgesOnIntermediateNode(vertex, on.prop, on.dest);
		for(IntermediateNodeAndOtherEnd interEdge : intermediateEdges) {
			String concatEdge = "";
			if(on.prop < interEdge.prop)
				concatEdge = on.prop + "," + interEdge.prop;
			else
				concatEdge = interEdge.prop + "," + on.prop;
			if(!concatenatedStringEdgesToNewEdgeIdMap.containsKey(concatEdge)) {
				// we have never seen this occurrence in our list. so ignore this.
				continue;
			}
			int newEdgeId = concatenatedStringEdgesToNewEdgeIdMap.get(concatEdge);

			CandidateEdgeEnds cee = new CandidateEdgeEnds();
			if(on.prop < interEdge.prop) {
				cee.source = vertex;
				cee.graphSource = graphNode;
				cee.object = interEdge.node;
				cee.graphObject = -1;
			} else {
				cee.source = interEdge.node;
				cee.graphSource = -1;
				cee.object = vertex;
				cee.graphObject = graphNode;
			}
			MutableString tid = getTupleID(cee.graphSource, newEdgeId, cee.graphObject);
			if(!addedTuples.contains(tid)) {
			//if(!addedTuples.contains(tid) && isValidCandidateEdge(cee.graphSource, newEdgeId, cee.graphObject)) {
				if(edgeToTripletMap.containsKey(newEdgeId)) {
					edgeToTripletMap.get(newEdgeId).add(cee);
				}
				else {
					ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
					ce.add(cee);
					edgeToTripletMap.put(newEdgeId, ce);
				}
				addToCandidateGraph(candidateEdges, addedTuples, newEdgeId, tid);
			}
		}
	}

	private void addToCandidateGraph(HashSet<Integer> candidateEdges,
			HashSet<MutableString> addedTuples, int prop, MutableString tid) {
		candidateEdges.add(prop);
		addedTuples.add(tid);
	}

	private ArrayList<IntermediateNodeAndOtherEnd> getOtherEdgesOnIntermediateNode(int firstNode, int firstProp, int intermediateNode) {
		ArrayList<IntermediateNodeAndOtherEnd> intermediateEdges = new ArrayList<IntermediateNodeAndOtherEnd>();
		ArrayList<ObjNodeIntProperty> intermediateNeighborsSrc = sourceDataGraphFileHandler.getVertexNeighbors(intermediateNode);
		if(intermediateNeighborsSrc != null && !intermediateNeighborsSrc.isEmpty()) {
			for(ObjNodeIntProperty internode : intermediateNeighborsSrc) {
				if(internode.prop != firstProp && internode.dest != firstNode) {
					IntermediateNodeAndOtherEnd ina = new IntermediateNodeAndOtherEnd();
					ina.node = internode.dest;
					ina.prop = internode.prop;
					intermediateEdges.add(ina);
				}
			}
		}

		ArrayList<ObjNodeIntProperty> intermediateNeighborsObj = objectDataGraphFileHandler.getVertexNeighbors(intermediateNode);
		if(intermediateNeighborsObj != null && !intermediateNeighborsObj.isEmpty()) {
			for(ObjNodeIntProperty internode : intermediateNeighborsObj) {
				if(internode.prop != firstProp && internode.dest != firstNode) {
					IntermediateNodeAndOtherEnd ina = new IntermediateNodeAndOtherEnd();
					ina.node = internode.dest;
					ina.prop = internode.prop;
					intermediateEdges.add(ina);
				}
			}
		}
		return intermediateEdges;
	}

	private void updateEdgeToTripletMap(int vertex, int graphNode, boolean isForwardEdge, HashSet<Integer> candidateEdges,
			HashSet<Integer> edges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
				//System.out.println("updateEdgeToTripletMap");
		if(edges != null && !edges.isEmpty()) {
			Iterator<Integer> iter = edges.iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				if(!edgeType.containsKey(edge))
					continue;
				candidateEdges.add(edge);
				CandidateEdgeEnds ce = new CandidateEdgeEnds();
				if(isForwardEdge) {
					ce.graphSource = graphNode;
					ce.source = vertex;
					ce.graphObject = -1;
					ce.object = edgeType.get(edge).object_type;
				} else {
					ce.graphObject = graphNode;
					ce.object = vertex;
					ce.graphSource = -1;
					ce.source = edgeType.get(edge).source_type;
				}
				if(edgeToTripletMap.containsKey(edge)) { //avoiding duplicates
					if(!edgeToTripletMap.get(edge).contains(ce))
						edgeToTripletMap.get(edge).add(ce);
				} else {
					ArrayList<CandidateEdgeEnds> cee = new ArrayList<CandidateEdgeEnds>();
					cee.add(ce);
					edgeToTripletMap.put(edge, cee);
				}
			}
		}
	}

	/**
	 * vertex contains the node for which we must find candidate incident edges
	 * edgeToTripletMap is null when this method is called from active mode. This method is called in passive mode ONLY when there is a single
	 * node in the partial query graph. It should NOT be called in passive mode otherwise.
	 * isSource flag is used only during ACTIVE mode. We only want to send back either source incident edges, or object incident edges.
	 * @param vertex
	 * @param seenTuples
	 * @param edgeToTripletMap
	 * @return
	 */
	private HashSet<Integer> getEdgesForVertex(ArrayList<GuiEdgeInfo>pg, int vertex, int graphNode,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, boolean isForwardEdge, HashSet<Integer> nodeValues, String systemName, Config conf) {
		HashSet<Integer> candidateEdges = new HashSet<Integer>();

		SpringClientHelper scp = new SpringClientHelper(conf);

		//check if the node has only one typeValue
		boolean singleTypeValueNode = false;
		for(GuiEdgeInfo e: pg) {
			if(e.graphSource == graphNode && scp.getListFromString(e.sourceTypeValues).size() == 1) singleTypeValueNode = true;
			else if(e.graphObject == graphNode && scp.getListFromString(e.objectTypeValues).size() == 1) singleTypeValueNode = true;
		}

		//System.out.println("singleTypeValueNode = "+singleTypeValueNode);

		//System.out.println("Vertex = " + vertex);
		if(vertex == 0) {
			System.out.println("inside case where vertex = 0. We shouldn't be coming here!");
			// Passive mode suggestions on empty canvas is not allowed at the moment. So we should NOT be coming in here at all!
			// this means the value for this vertex was not selected. The entire list of edges will be selected.
			Iterator<Integer> iter = edgeType.keySet().iterator();
			while(iter.hasNext()) {
				candidateEdges.add(iter.next());
			}
		} else if(sourceTypesToEdgesMap.containsKey(vertex) || objectTypesToEdgesMap.containsKey(vertex)) {
			// The vertex is a type vertex.
			if(edgeToTripletMap != null) {
				// This condition is when there is a single node in the partial query graph.
				System.out.println("Passive mode, single node in partial graph, finding edge");
				long start = System.currentTimeMillis();
				HashSet<Integer> srcEdges = singleTypeValueNode ? sourceTypesToEdgesMap.get(vertex) : getEdgesForEntities(nodeValues, true, systemName);
				HashSet<Integer> objEdges = singleTypeValueNode ? objectTypesToEdgesMap.get(vertex) : getEdgesForEntities(nodeValues, false, systemName);
							
				System.out.println("Elapsed time = "+(System.currentTimeMillis()-start));
				//System.out.println(srcEdges.size()+" "+objEdges.size());
				// HashSet<Integer> srcEdges = sourceTypesToEdgesMap.get(vertex);
				// HashSet<Integer> objEdges = objectTypesToEdgesMap.get(vertex);
				updateEdgeToTripletMap(vertex, graphNode, true, candidateEdges, srcEdges, edgeToTripletMap);
				updateEdgeToTripletMap(vertex, graphNode, false, candidateEdges, objEdges, edgeToTripletMap);
			} else {
				// This condition is for active mode vertex.
				System.out.println("Active mode, getting candidates for node " + vertex + " with graph node id " + graphNode);
				if(isForwardEdge) {
					//HashSet<Integer> srcEdges = sourceTypesToEdgesMap.get(vertex);
					HashSet<Integer> srcEdges = singleTypeValueNode ? sourceTypesToEdgesMap.get(vertex) : getEdgesForEntities(nodeValues, true, systemName);
					//System.out.println(Integer.toString(vertex)+" "+Integer.toString(srcEdges.size()));
					if(srcEdges != null && !srcEdges.isEmpty()) {
						Iterator<Integer> iter = srcEdges.iterator();
						while(iter.hasNext())
							candidateEdges.add(iter.next());
					}
				} else {
					//HashSet<Integer> objEdges = objectTypesToEdgesMap.get(vertex);
					HashSet<Integer> objEdges = singleTypeValueNode ? objectTypesToEdgesMap.get(vertex) : getEdgesForEntities(nodeValues, false, systemName);
					//System.out.println(Integer.toString(vertex)+" "+Integer.toString(objEdges.size()));
					if(objEdges != null && !objEdges.isEmpty()) {
						Iterator<Integer> iter1 = objEdges.iterator();
						while(iter1.hasNext())
							candidateEdges.add(iter1.next());
					}
				}
			}
		} else {
			System.out.println("This is an entity!");
			// this is an entity in the data graph
			// if this vertex is an intermediate node, don't bother adding anything.
			if(edgeToTripletMap != null && !intermediateNodesList.contains(vertex)) {
				// This condition is when there is a single node in the partial query graph.
				// this vertex is an actual node from the data graph.
				// add all those edges where vertex is the source...

				//getting all types that entity vertex belongs to
				// ArrayList<String> typesForVertex = new ArrayList<String>();
				// try {
				// 	SpringClientHelper scp = new SpringClientHelper(conf);
				// 	scp.getTypesForEntity(vertex, typesForVertex);
				// } catch(IllegalArgumentException iae) {
				// 	iae.printStackTrace();
				// }
				//
				// addEdgeTypeEdge(vertex, graphNode, typesForVertex, candidateEdges, edgeToTripletMap, true);
				// addEdgeTypeEdge(vertex, graphNode, typesForVertex, candidateEdges, edgeToTripletMap, false);
				System.out.println("adding adjacent edges as candidate edges");
				ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
				addEdgeTypeEdge(ons, vertex, graphNode, candidateEdges, edgeToTripletMap, true);
				ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
				addEdgeTypeEdge(revons, vertex, graphNode, candidateEdges, edgeToTripletMap, false);
			} else {
				// This condition is when there is an active edge drawn between either two entity nodes or an entity and type node,
				// AND this "vertex" is an entity in the data graph
				if(isForwardEdge) {
					ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
					addEdgeTypeEdge(ons, vertex, candidateEdges);
				} else {
					ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
					addEdgeTypeEdge(revons, vertex, candidateEdges);
				}
				System.out.println("number of candidates for this entity node = "+candidateEdges.size());

			}
		}
		return candidateEdges;
	}

	/* This method is to be called when there is exactly one node in the partial graph. Finds all candidate edges for the types that an entity vertex belongs to. Than add it to the candidate edges
	*/
	private void addEdgeTypeEdge(int vertex, int graphNode, ArrayList<String> types, HashSet<Integer> edges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, boolean forwardEdge) {

		HashSet<MutableString> addedTuples = new HashSet<MutableString>();
		HashMap<Integer, HashSet<Integer>> typesToEdgesMap = forwardEdge == true ? LoadData.getSourceTypesToEdgesMap() : LoadData.getObjectTypesToEdgesMap();

		for(String type : types) {
			int typeId = Integer.parseInt(type.split(",")[0]);
			if(typeId == 0 || !typesToEdgesMap.containsKey(typeId))
				continue;

			HashSet<Integer> edgesForType = typesToEdgesMap.get(typeId);
			for(Integer edge : edgesForType) {
				if(!edgeType.containsKey(edge))
					continue;
				int dest = forwardEdge == true ? edgeType.get(edge).object_type : edgeType.get(edge).source_type;
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				if(forwardEdge) {
					cee.source = vertex;
					cee.graphSource = graphNode;
					cee.object = dest;
					cee.graphObject = -1;
				}
				else {
					cee.source = dest;
					cee.graphSource = -1;
					cee.object = vertex;
					cee.graphObject = graphNode;
				}
				addToEdgeTripletAndUpdateAddedTuples(graphNode, edges, edgeToTripletMap, addedTuples, cee, edge, dest);
			}
		}
	}

	/**
	 * This method is to be called only when there is exactly one node in the partial query graph, and the candidates are found for
	 * passive mode suggestion (this is the automatic suggestions for the very first node)
	 * @param objNodes
	 * @param vertex
	 * @param edges
	 * @param edgeToTripletMap
	 * @param forwardEdge
	 */
	private void addEdgeTypeEdge(ArrayList<ObjNodeIntProperty> objNodes, int vertex, int graphNode, HashSet<Integer> edges,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, boolean forwardEdge) {
		//ArrayList<ObjNodeIntProperty> ons = dataGraph.get(vertex);
		HashSet<MutableString> addedTuples = new HashSet<MutableString>();
		if(objNodes != null) {
			for(ObjNodeIntProperty on : objNodes) {
				if(isFreebaseDataset && intermediateNodesList.contains(on.dest)) {
					ArrayList<IntermediateNodeAndOtherEnd> intermediateEdges = getOtherEdgesOnIntermediateNode(vertex, on.prop, on.dest);
					for(IntermediateNodeAndOtherEnd interEdge : intermediateEdges) {
						String concatEdge = "";
						if(on.prop < interEdge.prop)
							concatEdge = on.prop + "," + interEdge.prop;
						else
							concatEdge = interEdge.prop + "," + on.prop;
						if(!concatenatedStringEdgesToNewEdgeIdMap.containsKey(concatEdge)) {
							// we have never seen this occurrence in our list. so ignore this.
							continue;
						}
						int newEdgeId = concatenatedStringEdgesToNewEdgeIdMap.get(concatEdge);
						CandidateEdgeEnds cee = new CandidateEdgeEnds();
						if(on.prop < interEdge.prop) {
							// forward concatenated edge for "vertex"
							cee.source = vertex;
							cee.graphSource = graphNode;
							cee.object = interEdge.node;
							cee.graphObject = -1;
						} else {
							// reverse concatenated edge for "vertex"
							cee.source = interEdge.node;
							cee.graphSource = -1;
							cee.object = vertex;
							cee.graphObject = graphNode;
						}
						addToEdgeTripletAndUpdateAddedTuples(graphNode, edges, edgeToTripletMap, addedTuples, cee, newEdgeId, interEdge.node);
					}
				} else {
					if(!edgeType.containsKey(on.prop) || edges.contains(on.prop))
						continue;
					CandidateEdgeEnds cee = new CandidateEdgeEnds();
					if(forwardEdge) {
						cee.source = vertex;
						cee.graphSource = graphNode;
						cee.object = edgeType.get(on.prop).object_type;
						cee.graphObject = -1;
					}
					else {
						cee.source = edgeType.get(on.prop).source_type;
						cee.graphSource = -1;
						cee.object = vertex;
						cee.graphObject = graphNode;
					}
					addToEdgeTripletAndUpdateAddedTuples(graphNode, edges, edgeToTripletMap, addedTuples, cee, on.prop, on.dest);
				}
			}
		}
	}

	private void addToEdgeTripletAndUpdateAddedTuples(int graphNode, HashSet<Integer> edges,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, HashSet<MutableString> addedTuples, CandidateEdgeEnds cee,
			int newEdge, int newDest) {
		if(edgeToTripletMap.containsKey(newEdge)) {
			edgeToTripletMap.get(newEdge).add(cee);
		} else {
			ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
			ce.add(cee);
			edgeToTripletMap.put(newEdge, ce);
		}
		edges.add(newEdge);	
	}

	/**
	 * Takes in nodes that are graphNodeIDs in the GUI graph and makes appropriate checks.
	 * @param graphSource
	 * @param edge
	 * @param graphObject
	 * @return
	 */
	private boolean isValidCandidateEdge(int graphSource, int edge, int graphObject) {
		boolean isValidCandidate = true;
		// if this particular property is incident on either source of object in the partial query graph, return true
		// return true, if this exact edge is already in the rejected query graph (includes the direction, and the edge ends)
		//System.out.println("Checking validity of candidate ======> " + graphSource + " " + edge + " " + graphObject);
		if(partialQueryGraph.containsKey(graphSource)) {
			if(partialQueryGraph.get(graphSource) != null && partialQueryGraph.get(graphSource).containsKey(edge))
				isValidCandidate = false;
		}
		if(isValidCandidate && partialQueryGraph.containsKey(graphObject)) {
			if(partialQueryGraph.get(graphObject) != null && partialQueryGraph.get(graphObject).containsKey(edge))
				isValidCandidate = false;
		}
		if(isValidCandidate && rejectedQueryGraph.containsKey(graphSource)) {
			HashMap<Integer, ArrayList<DestNode>> propDest = rejectedQueryGraph.get(graphSource);
			if(propDest.containsKey(edge)) {
				ArrayList<DestNode> dests = propDest.get(edge);
				for(DestNode dn : dests) {
					if(dn.getDestGraphNodeID() == graphObject && dn.isForwardEdge()) {
						isValidCandidate = false;
						break;
					}
				}
			}
		}
		if(isValidCandidate && rejectedQueryGraph.containsKey(graphObject)) {
			HashMap<Integer, ArrayList<DestNode>> propDest = rejectedQueryGraph.get(graphObject);
			if(propDest.containsKey(edge)) {
				ArrayList<DestNode> dests = propDest.get(edge);
				for(DestNode dn : dests) {
					if(dn.getDestGraphNodeID() == graphSource && !dn.isForwardEdge()) {
						isValidCandidate = false;
						break;
					}
				}
			}
		}
		return isValidCandidate;
	}

	/**
	 * Finding candidates when the vertex is an entity, here we are only trying to find the concatenated edges for intermediate nodes
	 * @param objNodes
	 * @param vertex
	 * @param srcCandidates
	 * @param objCandidates
	 */
	private void addEdgeTypeEdge(ArrayList<ObjNodeIntProperty> objNodes, int vertex, HashSet<Integer> srcCandidates,
			HashSet<Integer> objCandidates) {
		if(objNodes != null) {
			for(ObjNodeIntProperty on : objNodes) {
				if(isFreebaseDataset && intermediateNodesList.contains(on.dest)) {
					ArrayList<IntermediateNodeAndOtherEnd> intermediateEdges = getOtherEdgesOnIntermediateNode(vertex, on.prop, on.dest);
					for(IntermediateNodeAndOtherEnd interEdge : intermediateEdges) {
						String concatEdge = "";
						if(on.prop < interEdge.prop)
							concatEdge = on.prop + "," + interEdge.prop;
						else
							concatEdge = interEdge.prop + "," + on.prop;
						if(!concatenatedStringEdgesToNewEdgeIdMap.containsKey(concatEdge)) {
							// we have never seen this occurrence in our list. so ignore this.
							continue;
						}
						int newEdgeId = concatenatedStringEdgesToNewEdgeIdMap.get(concatEdge);
						if(on.prop < interEdge.prop) {
							srcCandidates.add(newEdgeId);
						} else {
							objCandidates.add(newEdgeId);
						}
					}
				}
			}
		}
	}
	/**
	 * Finding candidates when the vertex is an entity, and only adding regular (non-concatenated) edges to the list
	 * @param objNodes
	 * @param vertex
	 * @param edges
	 */
	private void addEdgeTypeEdge(ArrayList<ObjNodeIntProperty> objNodes, int vertex, HashSet<Integer> edges) {
		//ArrayList<ObjNodeIntProperty> ons = dataGraph.get(vertex);
		if(objNodes != null) {
			for(ObjNodeIntProperty on : objNodes) {
				if(!intermediateNodesList.contains(on.dest))
					edges.add(on.prop);
			}
		}
	}

	public HashSet<Integer> getCandidateEdges(ArrayList<GuiEdgeInfo> pg, int suggestionMode, CandidateEdgeEnds activeModePartialGraph,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, int dataGraphInUse, int selectedGraphNode, LinkedHashMap<Integer, HashSet<Integer>> evaluatedValues, String systemName, Config conf) {

		// for(GuiEdgeInfo e : pg) {
		// 	System.out.println(e.sourceTypeValues+" : "+e.objectTypeValues);
		// }

		//System.out.println("generating candidate edges");

		HashSet<Integer> candidateEdges;
		if(dataGraphInUse == 0)
			candidateEdges = getCandidateEdgesFreebase(pg, suggestionMode, activeModePartialGraph, edgeToTripletMap, selectedGraphNode, evaluatedValues, systemName, conf);
		else if(dataGraphInUse == 1)
			candidateEdges = getCandidateEdgesDBpedia(suggestionMode, activeModePartialGraph, edgeToTripletMap);
		else
			candidateEdges = getCandidateEdgesYago(suggestionMode, activeModePartialGraph, edgeToTripletMap);
		// close the random file reader handler.
		try {
			sourceDataGraphFileHandler.close();
			objectDataGraphFileHandler.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return candidateEdges;
	}

	public void getNegativeEdgesHistory(ArrayList<GuiEdgeInfo> rejectedGraph,
			HashSet<Integer> history) {
	//	System.out.println("Printing rejected edges " + rejectedGraph.size());
		int cnt = 1;
		for(GuiEdgeInfo gei : rejectedGraph) {
			MutableString tid = getTupleID(gei.source, gei.edge, gei.object);
			//System.out.print(cnt + ") " + tid + "     ");
			cnt++;
			// do not edges into negative list if they are already seen as positive edges from partial Query graph.
			if(!history.contains(gei.edge)) {
				history.add(gei.edge*(-1));
			}
			// Now add this to rejected graph.
			/* With rejected graph, most often, one node is a grey node and other is a white node. In such cases, we only need to
			 * add entry from white node to the grey node, and not in the opposite direction.
			 * But in some cases, there may be a grey edge between two white nodes, in such cases, add edge info in both directions.
			 */
			if(gei.graphSource != -1 && gei.graphObject != -1) {
				// This is a grey edge between two white nodes.
				DestNode dn = new DestNode();
				dn.setDest(gei.object);
				dn.setDestGraphNodeID(gei.graphObject);
				boolean isForwardEdge = true;
				dn.setForwardEdge(isForwardEdge);
				addNewEdgeToGraph(gei, isForwardEdge, dn, rejectedQueryGraph);

				DestNode revdn = new DestNode();
				revdn.setDest(gei.source);
				revdn.setDestGraphNodeID(gei.graphSource);
				isForwardEdge = false;
				revdn.setForwardEdge(isForwardEdge);
				addNewEdgeToGraph(gei, isForwardEdge, revdn, rejectedQueryGraph);
			} else {
				int whiteGraphNode = gei.graphSource == -1 ? gei.graphObject : gei.graphSource;
				boolean isForwardEdge = gei.graphSource == -1 ? false : true;
				DestNode dn = new DestNode();
				dn.setDest(gei.graphSource==-1?gei.source : gei.object);
				dn.setDestGraphNodeID(-1);
				dn.setForwardEdge(isForwardEdge);
				//System.out.print("graph node: " + gei.graphSource + ", " + gei.edge + ", " + gei.graphObject);
				addNewEdgeToGraph(whiteGraphNode, gei.edge, dn, rejectedQueryGraph);
			}
		//	System.out.println();
		}
	}

	private void updateGraphNodeIndexes(int node, int graphNode) {
		// If the graphnode id is -1, then this was a grey node that was rejected. don't have to maintain this info for such nodes.
		if(graphNode != -1) {
			graphNodeToNodeMap.put(graphNode, node);
			if(nodeToGraphNodeMap.containsKey(node)) {
				nodeToGraphNodeMap.get(node).add(graphNode);
			} else {
				HashSet<Integer> graphNodes = new HashSet<Integer>();
				graphNodes.add(graphNode);
				nodeToGraphNodeMap.put(node, graphNodes);
			}
		}
	}

	//extract type values from a comma separated string
	private HashSet<Integer> stringToSet(String values) {
		String[] tokens = values.split(",");
		HashSet<Integer> result = new HashSet<Integer>();
		for(String t : tokens) {
			if (!t.equals("")) {
				result.add(Integer.parseInt(t));
			}
		}
		return result;
	}

	//for a type node (a set of type values belonging to it), return all neighboring candidate edges
	private HashSet<Integer> getEdgesForEntities(HashSet<Integer> entitiesForTypes, boolean isForwardEdge, String systemName) {
		HashSet<Integer> result = new HashSet<Integer>();
		ArrayList<ObjNodeIntProperty> ons = null;
		int entityCnt = 0;
		long start = System.currentTimeMillis();
		HashMap<Integer, HashSet<Integer>> edgeToEntities = new HashMap<Integer, HashSet<Integer>>();
		for (Integer entity : entitiesForTypes) {
			if(isForwardEdge) {
				ons = sourceDataGraphFileHandler.getVertexNeighbors(entity);
			} else {
				ons = objectDataGraphFileHandler.getVertexNeighbors(entity);
			}
			if (ons == null) {
				continue;
			}
			for(ObjNodeIntProperty o: ons) {
				//result.add(o.prop);
				if(!edgeToEntities.containsKey(o.prop)) {
					edgeToEntities.put(o.prop, new HashSet<Integer>());
				}
				edgeToEntities.get(o.prop).add(entity);
			}
			// if(edgeCount.containsKey(47182598)) {
			// 	System.out.println(entity+" --> "+edgeCount.get(47182598));
			// }

			entityCnt++;
			//limiting (1) number of entities because for types entity number is very high
			//(2) number of edges because for some entities the degree is very high
			if (entityCnt > 1200 || edgeToEntities.size() > 100) break;
		}

		//System.out.println("entityCnt="+entityCnt+", edgeToEntities.size()="+edgeToEntities.size());
		

		for(int e : edgeToEntities.keySet()) {

			//filtering-out noisy edges ONLY for orion, since this affects RDP
			//if(systemName.equals("orion") && (edgeToEntities.get(e).size()*1.0)/entityCnt < 0.1) continue;

			result.add(e);
		}
	
		//System.out.println("Total time spent in getEdgesForEntities = "+(System.currentTimeMillis()-start)/1000.0);
		return result;
	}

	public void getPartialGraph(int suggestionMode, ArrayList<GuiEdgeInfo> pg, HashSet<Integer> history, 
	CandidateEdgeEnds returnActivePartialGraph, CandidateEdgeEnds activeEdgeEnds, Config conf) {

		SpringClientHelper scp = new SpringClientHelper(conf);
		LoadData ldlm = new LoadData();

		//adding to history => (i) node types from edge ends, (ii) node types assigned to the node, (iii) edge types
		for(GuiEdgeInfo gei : pg) {
			if(gei.edge != -1) {
				history.add(gei.edge);
				history.add(ldlm.getEdgeType().get(gei.edge).source_type);
				history.add(ldlm.getEdgeType().get(gei.edge).object_type);
			}
			history.addAll(scp.getListFromString(gei.sourceTypeValues));
			history.addAll(scp.getListFromString(gei.objectTypeValues));
		}

		ArrayList<Integer> temp = new ArrayList<Integer>(history);
		System.out.println("history = "+temp.toString());

		boolean allSingleNode = true;
		for(int i = 0; i < pg.size(); i++) {
			if(pg.get(i).edge != -1 || pg.get(i).object != 0)
				allSingleNode = false;
		}
		if(allSingleNode) {
			// This condition means there is only one node in the partial query graph. This is the case where the passive suggestions is
			// called by the GUI after adding the very first edge!
			/*
			 * The source of pg[0] contains the newly added node ID.
			 */
			for(GuiEdgeInfo gei : pg) {
				int soloGraphVertex = gei.graphSource;
				int soloVertex = gei.source;
				partialQueryGraph.put(soloGraphVertex, null);
				nodeTypeValues.put(soloGraphVertex, stringToSet(gei.sourceTypeValues));
				adjacentEntities.put(soloGraphVertex, null);
				updateGraphNodeIndexes(soloVertex, soloGraphVertex);
			}
		}
		else {
			//add type values for entities to history
			// try {
			// 	String filePath = conf.getInputFilePath(PropertyKeys.instancesSortedToTypesIndexPaddedFile);
			// 	int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.instancesSortedToTypesIndexAlignmentLength)) + 1;
			// 	int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesSortedToTypesIndexNumberOfLines));
			// 	BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);

			// 	for(int i = 0; i < pg.size(); i++) {
			// 		if(!pg.get(i).sourceEntity.equals("-1")) {
			// 			typeConstrains.addAll(baf.getEntityTypes(pg.get(i).source));
			// 		} else {
			// 			typeConstrains.add(pg.get(i).source);
			// 		}
			// 		if(!pg.get(i).objectEntity.equals("-1")) {
			// 			typeConstrains.addAll(baf.getEntityTypes(pg.get(i).object));
			// 		} else {
			// 			typeConstrains.add(pg.get(i).object);
			// 		}
			// 	}
			// 	baf.close();
			// } catch(IOException ioe) {
			// 	ioe.printStackTrace();
			// }


			//System.out.println("Printing partial query graph");
			for(GuiEdgeInfo gei : pg) {
				MutableString tid = getTupleID(gei.source, gei.edge, gei.object);
				//System.out.println(tid + " -> " + getTupleID(gei.graphSource, gei.edge, gei.graphObject));
				updateGraphNodeIndexes(gei.source, gei.graphSource);
				updateGraphNodeIndexes(gei.object, gei.graphObject);

				if(suggestionMode == 1 || suggestionMode == 2) {
					// Passive event
				//	System.out.println("adding " + gei.graphSource + " " + gei.edge + " " + gei.graphObject);
					DestNode dn = new DestNode();
					dn.setDest(gei.object);
					dn.setDestGraphNodeID(gei.graphObject);
					boolean isForwardEdge = true;
					dn.setForwardEdge(isForwardEdge);
					addNewEdgeToGraph(gei, isForwardEdge, dn, partialQueryGraph);
					nodeTypeValues.put(gei.graphSource, stringToSet(gei.sourceTypeValues));
					if(!gei.objectEntity.equals("-1")) {
						if(adjacentEntities.containsKey(gei.graphSource)) {
							adjacentEntities.get(gei.graphSource).add(gei.objectEntity+","+gei.edge+","+isForwardEdge);
						} else {
							adjacentEntities.put(gei.graphSource, new HashSet<String>(Arrays.asList(gei.objectEntity+","+gei.edge+","+isForwardEdge)));
						}
					}
					//addNewEdgeToGraph(gei.source, gei.edge, dn, partialQueryGraph);

					DestNode revdn = new DestNode();
					revdn.setDest(gei.source);
					revdn.setDestGraphNodeID(gei.graphSource);
					isForwardEdge = false;
					revdn.setForwardEdge(isForwardEdge);
					addNewEdgeToGraph(gei, isForwardEdge, revdn, partialQueryGraph);
					nodeTypeValues.put(gei.graphObject, stringToSet(gei.objectTypeValues));
					if(!gei.sourceEntity.equals("-1")) {
						if(adjacentEntities.containsKey(gei.graphObject)) {
							adjacentEntities.get(gei.graphObject).add(gei.sourceEntity+","+gei.edge+","+isForwardEdge);
						} else {
							adjacentEntities.put(gei.graphObject, new HashSet<String>(Arrays.asList(gei.sourceEntity+","+gei.edge+","+isForwardEdge)));
						}
					}
					//addNewEdgeToGraph(gei.object, gei.edge, revdn, partialQueryGraph);
				}
			}
			//System.out.println(adjacentEntities.keySet());
			// for (Integer key : adjacentEntities.keySet()) {
			// 	System.out.println(adjacentEntities.get(key));
			// }
		}
		if(suggestionMode == 0 || suggestionMode == -1) {
			// Active event
			// We are supposed to have only one edgeEnds here.
			returnActivePartialGraph.source = activeEdgeEnds.source;
			returnActivePartialGraph.graphSource = activeEdgeEnds.graphSource;
			returnActivePartialGraph.object = activeEdgeEnds.object;
			returnActivePartialGraph.graphObject = activeEdgeEnds.graphObject;
			System.out.println("pg size = "+pg.size());
			for(GuiEdgeInfo gei : pg) {
				nodeTypeValues.put(gei.graphSource, stringToSet(gei.sourceTypeValues));
				nodeTypeValues.put(gei.graphObject, stringToSet(gei.objectTypeValues));
			}
		}
	}

	private void addNewEdgeToGraph(GuiEdgeInfo gei, boolean isForwardEdge, DestNode dn,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> graph) {
		HashMap<Integer, ArrayList<DestNode>> srcProp;
		int src;
		int edge = gei.edge;
		if(isForwardEdge) {
			src = gei.graphSource;
		} else {
			src = gei.graphObject;
		}
		if(graph.containsKey(src)) {
			srcProp = graph.get(src);
			if(srcProp.containsKey(edge)) {
				ArrayList<DestNode> dns = srcProp.get(edge);
				dns.add(dn);
			}
			else {
				ArrayList<DestNode> dns = new ArrayList<DestNode>();
				dns.add(dn);
				srcProp.put(edge, dns);
				graph.put(src, srcProp);
			}
		}
		else {
			srcProp = new HashMap<Integer, ArrayList<DestNode>>();
			ArrayList<DestNode> dns = new ArrayList<DestNode>();
			dns.add(dn);
			srcProp.put(edge, dns);
			graph.put(src, srcProp);
		}
	}
	/**
	 * Overloaded method to add the edge only in one direction. This is used for rejected graph, when one of the ends is a grey node.
	 * @param src
	 * @param edge
	 * @param isForwardEdge
	 * @param dn
	 * @param graph
	 */
	private void addNewEdgeToGraph(int src, int edge, DestNode dn,
			HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> graph) {
		HashMap<Integer, ArrayList<DestNode>> srcProp;
		if(graph.containsKey(src)) {
			srcProp = graph.get(src);
			if(srcProp.containsKey(edge)) {
				ArrayList<DestNode> dns = srcProp.get(edge);
				dns.add(dn);
			}
			else {
				ArrayList<DestNode> dns = new ArrayList<DestNode>();
				dns.add(dn);
				srcProp.put(edge, dns);
				graph.put(src, srcProp);
			}
		}
		else {
			srcProp = new HashMap<Integer, ArrayList<DestNode>>();
			ArrayList<DestNode> dns = new ArrayList<DestNode>();
			dns.add(dn);
			srcProp.put(edge, dns);
			graph.put(src, srcProp);
		}
	}

	private MutableString getTupleID(int src, int prop, int dest) {
		MutableString tid = new MutableString();
		if(src < dest) {
			tid = tid.append(src).append(",").append(prop).append(",").append(dest);
		}
		else {
			tid = tid.append(dest).append(",").append(prop).append(",").append(src);
		}
		return tid;
	}

	public ArrayList<CandidateEdgeScore> rankCandidatesByEdgeLabel(HashSet<Integer> candidateEdges) {
		ArrayList<CandidateEdgeScore> rankedCandidates = new ArrayList<CandidateEdgeScore>();
		class EdgeLabel {
			int edge;
			String label;
		}
		ArrayList<EdgeLabel> labeledCandidates = new ArrayList<EdgeLabel>();
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext()) {
			EdgeLabel el = new EdgeLabel();
			el.edge = iter.next();
			if(edgeLabel.containsKey(el.edge)) {
				el.label = edgeLabel.get(el.edge);
				labeledCandidates.add(el);
			}
		}
		Collections.sort(labeledCandidates, new Comparator<EdgeLabel>() {
			public int compare(EdgeLabel l, EdgeLabel r) {
				return l.label.compareToIgnoreCase(r.label);
			}
		});
		int score = 10000000; //assign the first edge a large score
		for(EdgeLabel el : labeledCandidates) {
			CandidateEdgeScore ces = new CandidateEdgeScore();
			ces.edge = el.edge;
			ces.score = score;
			score--;
			rankedCandidates.add(ces);
		}
		return rankedCandidates;
	}

public BackendResponseObject getCandidateTriples(ArrayList<GuiEdgeInfo> partialGraph, int topk, int suggestionMode, ArrayList<CandidateEdgeScore> rankedCandidateEdges, 
			HashMap<String, Double>decayFactor, HashSet<String>lastRecommendedEdges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, String systemName, Config conf) {

		//System.out.println("suggestionMode = "+suggestionMode+", lastRecommendedEdges = "+lastRecommendedEdges.size()+", edgeToTripletMap = "+edgeToTripletMap.size());

		BackendResponseObject backObj = new BackendResponseObject();
		ArrayList<GuiEdgeStringInfo> allEdges = new ArrayList<GuiEdgeStringInfo>();
		HashSet<Integer> seenEdges = new HashSet<Integer>();
		HashSet<Integer> seenEdgesBetweenExistingNodes = new HashSet<Integer>();
		HashSet<MutableString> seenTuplesBetweenWhites = new HashSet<MutableString>();
		HashSet<MutableString> seenTuplesNewReverse = new HashSet<MutableString>();
		HashSet<String> seenGraphNodeEnds = new HashSet<String>();
	
		//[UI] System.out.println("************* REBUILDING CANDIDATES *************** " + rankedCandidateEdges.size());
		BufferedRandomAccessFile braf = null;
		try {
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.instancesIdsortedLangEnAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesIdsortedLangEnNumberOfLines));
			braf = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.instancesIdsortedLangEnPaddedFile), "r", numOfLines, bufferSize);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		//System.out.println("System name = " + systemName);

		//for(int i=0; i<rankedCandidateEdges.size() && topk-- > 0; i++) {

		double dfThreshold = Double.parseDouble(conf.getProp(PropertyKeys.decayFactorThreshold));
		double edgeScoreThreshold = systemName.equals("baseline") ? Double.parseDouble(conf.getProp(PropertyKeys.edgeScoreThresholdBLR)) : Double.parseDouble(conf.getProp(PropertyKeys.edgeScoreThresholdRDP));

		//System.out.println("size of rankedCandidateEdges = "+rankedCandidateEdges.size());



		for(int i=0; i<rankedCandidateEdges.size(); i++) {
			
			int edge =  rankedCandidateEdges.get(i).edge;
	
			if(edge == 47183681) //will not display hud_county_place/place
				continue;
			if(!edgeType.containsKey(edge)) {
				//System.out.println(edge);
				continue;
			}
			int actualSrcType = edgeType.get(edge).source_type;
			//System.out.println(actualSrcType);
			int actualObjType = edgeType.get(edge).object_type;
			if(!edgeLabel.containsKey(edge)) {
		//		System.out.println("Cant find label for edge: " + edge);
				continue;
			}


			ArrayList<CandidateEdgeEnds> cands = edgeToTripletMap.get(edge);

			
			// if(edge==47185561) {
			// 	System.out.println("47185561 exists in candidate edges.");
			// 	System.out.println("cand score = "+rankedCandidateEdges.get(i).score);
			// 	System.out.println("cands size = "+cands.size());
			// }
			
			if(cands == null || cands.isEmpty())
				continue;

			//if(edge == 47180290) System.out.println("333");

			for(CandidateEdgeEnds cand : cands) {
				//if(edge==47185561) System.out.println(cand.source+","+cand.graphSource+","+cand.object+","+cand.graphObject+","+cands.size());


				//for baseline system, we need the candidate edge to be attached to a specific graph node
				if(systemName.equals("baseline")) {
					int node = rankedCandidateEdges.get(i).node;
					boolean isForwardEdge = rankedCandidateEdges.get(i).isForwardEdge;
					//System.out.println("%% "+edge+","+node+","+cand.graphSource+","+cand.graphObject);
					if( !((isForwardEdge && node == cand.graphSource) || (!isForwardEdge && node == cand.graphObject)) ) {
						continue;
					}
				}

				NodeLabel srcLabelObj = getNodeLabel(cand.source, braf);
				NodeLabel objLabelObj = getNodeLabel(cand.object, braf);
				if(srcLabelObj.label == null || objLabelObj.label == null)
					continue;
				String edgeStr = edge + "|" + edgeLabel.get(edge);
				String sourceStr = cand.source + "|" + srcLabelObj.label;
				String objectStr = cand.object + "|" + objLabelObj.label;

				GuiEdgeStringInfo gei = new GuiEdgeStringInfo();
				gei.edge = edgeStr;
				gei.actualSourceType = actualSrcType;
				gei.actualObjectType = actualObjType;
				gei.source = sourceStr;
				gei.graphSource = cand.graphSource;
				gei.object = objectStr;
				gei.graphObject = cand.graphObject;
				gei.isSourceType = srcLabelObj.isType;
				gei.isObjectType = objLabelObj.isType;
				String triple = gei.graphSource+","+edge+","+gei.graphObject;

				gei.score = rankedCandidateEdges.get(i).score;
				//gei.score = rankedCandidateEdges.get(i).score * baselineScore.getOrDefault(triple, 1.0);
				
				if(!decayFactor.containsKey(triple)) decayFactor.put(triple, 1.0);
				double df1 = decayFactor.get(triple);

				//if(edge==47185561) System.out.println(gei.graphSource+" --> "+edge+" --> "+gei.graphObject+" --> "+gei.score);
			
				if(df1 > dfThreshold) {
					//if(systemName.equals("orion") && gei.score == 0.0 && df1 < 1.0) continue;
					gei.score *= df1;
					//if(gei.score > 0) 
						allEdges.add(gei);
				
					// if(gei.edge.split("\\|")[0].equals("47185552")) {
					// 	System.out.println(gei.graphSource+","+gei.edge.split("\\|")[0]+","+gei.graphObject+","+gei.score+","+rankedCandidateEdges.get(i).edge);
					// }

					//adding edges between existing nodes
					ArrayList<GuiEdgeStringInfo> candidateEdgeBetweenExistingNodes = new ArrayList<GuiEdgeStringInfo>();
					for(GuiEdgeStringInfo geiComp : allEdges) {
						if(geiComp.edge.equals(gei.edge)) {
							if(geiComp.graphSource != -1 && geiComp.graphObject != -1) continue;
							GuiEdgeStringInfo geiNew = new GuiEdgeStringInfo();
							geiNew.edge = edgeStr;								
							geiNew.isSourceType = srcLabelObj.isType;
							geiNew.isObjectType = objLabelObj.isType;
							geiNew.actualSourceType = actualSrcType;
							geiNew.actualObjectType = actualObjType;
							geiNew.score = Math.max(rankedCandidateEdges.get(i).score, geiComp.score/decayFactor.get(geiComp.graphSource+","+edge+","+geiComp.graphObject));
							boolean isValidEdge = false;
							if(geiComp.graphSource == -1 && gei.graphObject == -1) {
								geiNew.graphSource = gei.graphSource;
								geiNew.graphObject = geiComp.graphObject;
								geiNew.source = gei.source;
								geiNew.object = geiComp.object;
								isValidEdge = true;
							} else if(geiComp.graphObject == -1 && gei.graphSource == -1) {
								geiNew.graphSource = geiComp.graphSource;
								geiNew.graphObject = gei.graphObject;
								geiNew.source = geiComp.source;
								geiNew.object = gei.object;
								isValidEdge = true;
							}
							for(GuiEdgeInfo pge :  partialGraph) {
								if(pge.graphSource == geiNew.graphSource && pge.graphObject == geiNew.graphObject && edge == pge.edge) {
									//do not recommend the same edge between existing nodes if those are already connected by that edge
									isValidEdge=false;
									break;
								}
							}

							if(geiNew.graphSource == geiNew.graphObject) isValidEdge=false;

							if(isValidEdge) {
								triple = geiNew.graphSource+","+edge+","+geiNew.graphObject;
								if(!decayFactor.containsKey(triple)) decayFactor.put(triple, 1.0);
								
								double df2 = decayFactor.get(triple);
								if(df2 > dfThreshold) {
									//if(systemName.equals("orion") && geiNew.score == 0.0 && df2 < 1.0) continue;
									geiNew.score *= df2;
									candidateEdgeBetweenExistingNodes.add(geiNew);
								}
							}
						}
					}
					for(GuiEdgeStringInfo e : candidateEdgeBetweenExistingNodes) {
						//if(e.score > 0) 
							allEdges.add(e);
					}
				}
			}

		}
		try {
			braf.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		//System.out.println("size of allEdges = "+allEdges.size());



		ArrayList<GuiEdgeStringInfo> sortedCandidateEdges = sortCandidateEdgesByScore(allEdges);
		ArrayList<GuiEdgeStringInfo> sortedUniqueCandidateEdges = new ArrayList<GuiEdgeStringInfo>();

		for(int i = 0; i < sortedCandidateEdges.size(); i++) {
			int edge = Integer.parseInt(sortedCandidateEdges.get(i).edge.split("\\|")[0]);
			//System.out.println(sortedCandidateEdges.get(i).graphSource+","+edge+","+sortedCandidateEdges.get(i).graphObject + " : "+ sortedCandidateEdges.get(i).score);
			if(!seenEdges.contains(edge)) {
				seenEdges.add(edge);
				int graphSource = sortedCandidateEdges.get(i).graphSource;
				int graphObject = sortedCandidateEdges.get(i).graphObject;
				if(graphSource != -1 && graphObject != -1 && (suggestionMode == 1 || suggestionMode == 2)) {
					if(seenGraphNodeEnds.contains(graphSource + "," + graphObject))
						continue;
					seenGraphNodeEnds.add(graphSource + "," + graphObject);
					seenGraphNodeEnds.add(graphObject + "," + graphSource);
				}
				String triple = graphSource+","+edge+","+graphObject;
		
				sortedUniqueCandidateEdges.add(sortedCandidateEdges.get(i));

			}
		}

		//System.out.println("size of sortedUniqueCandidateEdges = "+sortedUniqueCandidateEdges.size());

		// for(int  i = 0 ; i < sortedUniqueCandidateEdges.size(); i++) {
		// 	if(sortedUniqueCandidateEdges.get(i).edge.split("\\|")[0].equals("47179852")) {
		// 		System.out.println("FOUND!!! score = "+sortedUniqueCandidateEdges.get(i).score);
		// 		break;
		// 	}
		// 	if(i == (sortedUniqueCandidateEdges.size()-1)) {
		// 		System.out.println("NOT found!!!");
		// 	}
		// }

		lastRecommendedEdges.clear();
		for(int i = 0; i < sortedUniqueCandidateEdges.size() && topk-- > 0; i++) {
			GuiEdgeStringInfo e = sortedUniqueCandidateEdges.get(i);
			String triple = e.graphSource+ "," + e.edge.split("\\|")[0] + "," + e.graphObject;
			//System.out.println(e.graphSource+ "," + e.edge + "," + e.graphObject+","+e.score);
			lastRecommendedEdges.add(triple);
			backObj.rankedUniqueEdges.add(e);
			//if(i == 0) System.out.println("Baseline score for bestEdge = "+baselineScore.get(triple)); //rdp+baseline
		}

		/*
		ArrayList<GuiEdgeStringInfo> sortedCandidateEdges = sortCandidateEdgesByScore(uniqueEdges);
		lastRecommendedEdges.clear();
		for(int i = 0; i < sortedCandidateEdges.size() && topk-- > 0; i++) {
			String triple = Integer.toString(sortedCandidateEdges.get(i).graphSource) + "," + sortedCandidateEdges.get(i).edge.split("\\|")[0] + "," + Integer.toString(sortedCandidateEdges.get(i).graphObject);
			lastRecommendedEdges.add(triple);
			backObj.rankedUniqueEdges.add(sortedCandidateEdges.get(i));
		} */

		//[UI] System.out.println("******************** ALL FINE ********************");
		return backObj;
	}

	private ArrayList<GuiEdgeStringInfo> sortCandidateEdgesByScore(ArrayList<GuiEdgeStringInfo> list) {
		try {
			Collections.sort(list, new Comparator<GuiEdgeStringInfo>(){
				  public int compare(GuiEdgeStringInfo o1, GuiEdgeStringInfo o2) {
					  return Double.compare(o2.score, o1.score);
				  }
				});
		} catch(IllegalArgumentException iae) {
			for(GuiEdgeStringInfo ce : list)
				System.out.println("Comparison error: " + ce.edge + " --> " + ce.score);
			iae.printStackTrace();
		}
		return list;
	}



	/**
	 * This method adds a new grey edge between two existing nodes of the partial query graph, if it can.
	 * @param edge
	 * @param cand
	 * @param rankedEdges
	 */
	private void addCandidateEdgeBetweenExistingNodes(int edge, CandidateEdgeEnds cand, ArrayList<GuiEdgeStringInfo> rankedEdges,
			HashSet<MutableString> seenTuples, BufferedRandomAccessFile braf) {
		int graphNode = -1;
		int otherVertex = 0;
		int src = 0;
		int graphSrc = 0;
		int obj = 0;
		int graphObj = 0;
	//	System.out.println(" Resolving!! " + cand.graphSource + ", " + edge + ", " + cand.graphObject);

		graphNode = cand.graphSource;
		otherVertex = cand.object;

		src = cand.source;
		obj = cand.object;
		graphSrc = cand.graphSource;
		graphObj = cand.graphObject;

		// work with cases where one of the nodes is a new grey node, and other graph nodes in the partial query graph that have the
		// same entity/type as otherVertex are present in the partial query graph
		if(graphNode != -1 && nodeToGraphNodeMap.containsKey(otherVertex)) {
			HashSet<Integer> graphNodesForOtherVertex = nodeToGraphNodeMap.get(otherVertex);
			Iterator<Integer> iter = graphNodesForOtherVertex.iterator();
			while(iter.hasNext()) {
				int otherGraphNode = iter.next();
				if(graphNode == otherGraphNode)
					continue;
			//	System.out.println(graphNode + "    (" + otherVertex + ") , " + otherGraphNode);
				if(canAddEdgeBetweenWhiteNodes(edge, graphNode, otherGraphNode)) {
					//System.out.println(graphNode + "    (" + otherVertex + ") , " + otherGraphNode);
			//		System.out.println("------");
					NodeLabel srcLabelObj = getNodeLabel(src, braf);
					NodeLabel objLabelObj = getNodeLabel(obj, braf);
					if(srcLabelObj.label == null || objLabelObj.label == null)
						continue;
					String edgeStr = edge + "|" + edgeLabel.get(edge);
					String sourceStr = src + "|" + srcLabelObj.label;
					String objectStr = obj + "|" + objLabelObj.label;
					GuiEdgeStringInfo gei = new GuiEdgeStringInfo();
					gei.actualSourceType = edgeType.get(edge).source_type;
					gei.actualObjectType = edgeType.get(edge).object_type;
					gei.edge = edgeStr;
					gei.source = sourceStr;
					gei.isSourceType = srcLabelObj.isType;
					gei.graphSource = graphSrc == -1 ? otherGraphNode : graphSrc;
					gei.object = objectStr;
					gei.isObjectType = objLabelObj.isType;
					gei.graphObject = graphObj == -1 ? otherGraphNode : graphObj;
			//		System.out.println("Edge between existing nodes = " + gei.graphSource + " " + edge + " " + gei.graphObject);
					if(!seenTuples.contains(getTupleID(gei.graphSource, edge, gei.graphObject))) {
						rankedEdges.add(gei);
						seenTuples.add(getTupleID(gei.graphSource, edge, gei.graphObject));
					}
				}
			}
		//	System.out.println("\n ================================== \n");
		}
	}

	/**
	 * check if there is already an edge between graphNode and otherGraphNode in either partial query graph, or rejected graph.
	 * If yes, return false. If not found, return true...
	 * @param edge
	 * @param graphNode
	 * @param otherGraphNode
	 * @return
	 */
	private boolean canAddEdgeBetweenWhiteNodes(int edge, int graphNode, int otherGraphNode) {
		boolean canAdd = true;
		try {

			if(isPresentInGraph(edge, otherGraphNode, partialQueryGraph.get(graphNode))) {
				//System.out.println(graphNode+","+otherGraphNode);
				canAdd = false;
			}
			if(canAdd && rejectedQueryGraph.containsKey(graphNode) && isPresentInGraph(edge, otherGraphNode, rejectedQueryGraph.get(graphNode))) {
				canAdd = false;
			}
		} catch(NullPointerException npe) {
			System.out.println("ERROR during canAddEdgeBetweenWhiteNodes CHECK: " + graphNode);
			canAdd = false;
			/*System.out.println("Printing keys and value sizes in partial query graph");
			Iterator<Integer> iter = partialQueryGraph.keySet().iterator();
			while(iter.hasNext()) {
				int node = iter.next();
				System.out.println(node + " : " + partialQueryGraph.get(node));
			}*/
			npe.printStackTrace();
		}
		return canAdd;
	}
	private boolean isPresentInGraph(int edge, int otherGraphNode, HashMap<Integer, ArrayList<DestNode>> graphNodeEdges) {
		boolean isPresent = false;
    //checks if there's ANY edge between graphNode & otherGraphNode in partial graph. thus parameter edge is not used
		if(graphNodeEdges != null) {
			for (Map.Entry<Integer, ArrayList<DestNode>> entry : graphNodeEdges.entrySet()) {
				ArrayList<DestNode> value = entry.getValue();
				for (int i = 0; i < value.size(); i++) {
					if(value.get(i).getDestGraphNodeID() == otherGraphNode) {
						isPresent = true;
					}
				}
			}
		}
		/*
		if(graphNodeEdges.containsKey(edge)) {
			ArrayList<DestNode> dns = graphNodeEdges.get(edge);
			for(DestNode dn : dns) {
				if(dn.getDestGraphNodeID() == otherGraphNode) {
					isPresent = true;
					break;
				}
			}
		}*/
		return isPresent;
	}

	private NodeLabel getNodeLabel(int node, BufferedRandomAccessFile braf) {
		NodeLabel nl = new NodeLabel();
		if(nodeLabel.containsKey(node)) {
			nl.label = nodeLabel.get(node);
			nl.isType = true;
		}
		else {
			nl.label = braf.getNodeLabel(node);
			nl.isType = false;
		}
		return nl;
	}


}
class NodeLabel {
	String label = null;
	boolean isType = false;
}
