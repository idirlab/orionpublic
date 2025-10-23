package viiq.graphCompletionGuiMain;

import viiq.graphQuerySuggestionMain.Config;
import viiq.graphQuerySuggestionMain.DestNode;
import viiq.utils.BufferedRandomAccessFile;
import viiq.utils.PropertyKeys;
import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import viiq.commons.CandidateEdgeEnds;
import viiq.commons.EdgeEnds;
import viiq.commons.EdgeTypeInfo;
import viiq.commons.GuiEdgeInfo;
import viiq.commons.GuiEdgeStringInfo;
import viiq.commons.ObjNodeIntProperty;
import viiq.commons.IntermediateNodeAndOtherEnd;

import viiq.clientServer.client.BackendResponseObject;
import viiq.clientServer.server.LoadData;

public class GenerateCandidates {
	final Logger logger = Logger.getLogger(getClass());
	boolean isFreebaseDataset = false;
	boolean isDbpediaDataset = false;
	boolean isYagoDataset = false;

	HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();

	// store the type of the two ends of an edge.
	// key = edge, value = (source vertex Type, dest vertex Type)
	HashMap<Integer, EdgeTypeInfo> edgeType = new HashMap<Integer, EdgeTypeInfo>();
	// store all the types associated with an node/entity/vertex. The types are derived as the union of the type associated with this node
	// on all the edges it is incident on.
	// key = vertex ID, value = set of all types of this vertex
	//TIntObjectHashMap<TIntHashSet> abc = new TIntObjectHashMap<>();
	HashMap<Integer, HashSet<Integer>> nodeTypes = new HashMap<Integer, HashSet<Integer>>();
	//HashMap<Integer, HashSet<Integer>> nodeTypes = new HashMap<Integer, HashSet<Integer>>();
	// Edges associated with each node type.
	// when this node type is of source.
	// key = source Type, value = set of all edges that have the key as source type
	HashMap<Integer, HashSet<Integer>> srcTypeEdges = new HashMap<Integer, HashSet<Integer>>(); 
	// when this node type is of object.
	// key = object Type, value = set of all edges that have the key as object type
	HashMap<Integer, HashSet<Integer>> objTypeEdges = new HashMap<Integer, HashSet<Integer>>();
	// a node can be of multiple types. but the ends of an edge is of one specific type each. the neighboring edges associated with a node type
	// is not just the edges that are incident on this particular node type, but are those associated with other types that an instance of
	// this node type may have incident on them.
	// for example a node type "Founder" may also have edges that are incident on "Person" too, since every "Founder" will be a "Person" too.
	// key = node type, value = (other node types that instances of the "key" belong to)
	HashMap<Integer, HashSet<Integer>> invertedNodeTypes = new HashMap<Integer, HashSet<Integer>>();
	// this maintains all the TYPES associated with a domain.
	// key = domain, value = set of TYPES
	//	HashMap<Integer, ArrayList<Integer>> domainsToTypesIndex = new HashMap<Integer, ArrayList<Integer>>();
	// this maintains all the EdgeTypes associated with a TYPE.
	// key = TYPE, value = EdgeType
	//	HashMap<Integer, ArrayList<Integer>> typeToEdgeTypeIndex = new HashMap<Integer, ArrayList<Integer>>();
	// contains labels of edges. key: edge ID, value: label
	HashMap<Integer, String> edgeLabel = new HashMap<Integer, String>();
	// contains labels of nodes (domains, type, edgetype, entities). This assumes all IDs are unique!
	// key: node ID, value: node label
	HashMap<Integer, String> nodeLabel = new HashMap<Integer, String>();

	// set of nodes in the graph that are intermediate nodes.
	HashSet<Integer> intermediateNodesList = new HashSet<Integer>();
	// all types associated with intermediate nodes.
	//	HashSet<Integer> intermediateNodesTypeList = new HashSet<Integer>();
	// key = concatenation of two edges connecting an intermediate node, value = new edge ID corresponding to the key
	HashMap<String, Integer> concatenatedStringEdgesToNewEdgeIdMap = new HashMap<String, Integer>();

	// key: type, value: edges whose source type is the key. (generated using all the instances of "type" in the data graph)
	HashMap<Integer, HashSet<Integer>> sourceTypesToEdgesMap = new HashMap<Integer, HashSet<Integer>>();

	// key: type, value: edges whose object type is the key. (generated using all the instances of "type" in the data graph)
	HashMap<Integer, HashSet<Integer>> objectTypesToEdgesMap = new HashMap<Integer, HashSet<Integer>>();

	BufferedRandomAccessFile sourceDataGraphFileHandler;
	BufferedRandomAccessFile objectDataGraphFileHandler;

	int numOfTotalEdges;

	public GenerateCandidates(LoadData ld, Config conf) {
		edgeType = ld.getEdgeType();
		nodeTypes = ld.getNodeTypes();
		srcTypeEdges = ld.getSrcTypeEdges();
		objTypeEdges = ld.getObjTypeEdges();
		invertedNodeTypes = ld.getInvertedNodeTypes();
		intermediateNodesList = ld.getIntermediateNodesList();
		srcDataGraph = ld.getSrcDataGraph();
		objDataGraph = ld.getObjDataGraph();
		concatenatedStringEdgesToNewEdgeIdMap = ld.getConcatenatedStringEdgesToNewEdgeIdMap();
		//		domainsToTypesIndex = ld.getDomainsToTypesIndex();
		//		typeToEdgeTypeIndex = ld.getTypeToEdgeTypeIndex();
		edgeLabel = ld.getEdgeLabelIndex();
		nodeLabel = ld.getNodeLabelIndex();
		numOfTotalEdges = ld.getNumOfTotalEdges();
		sourceTypesToEdgesMap = ld.getSourceTypesToEdgesMap();
		objectTypesToEdgesMap = ld.getObjectTypesToEdgesMap();

		try{
			sourceDataGraphFileHandler = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile), "r", numOfTotalEdges, conf);
			objectDataGraphFileHandler = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphObjectAlignedFile), "r", numOfTotalEdges, conf);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	protected void finalize() {
		try {
			sourceDataGraphFileHandler.close();
			objectDataGraphFileHandler.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> getPartialGraph(int suggestionMode, ArrayList<GuiEdgeInfo> pg, 
			ArrayList<Integer> history, HashSet<Integer> historyHash, HashSet<MutableString> seenTuples, 
			CandidateEdgeEnds returnActivePartialGraph, CandidateEdgeEnds activeEdgeEnds) {
		HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph = new HashMap<Integer, HashMap<Integer,ArrayList<DestNode>>>();
		if(pg.size() == 1 && pg.get(0).edge == -1 && pg.get(0).object == 0) {
			// This condition means there is only one node in the partial query graph. This is the case where the passive suggestions is
			// called by the GUI after adding the very first edge!
			/*
			 * The source of pg[0] contains the newly added node ID.
			 */
			int soloVertex = pg.get(0).source;
			partialGraph.put(soloVertex, null);
		}
		else {
			System.out.println("Printing partial query graph");
			for(GuiEdgeInfo gei : pg) {
				MutableString tid = getTupleID(gei.source, gei.edge, gei.object); 
				seenTuples.add(tid);
				System.out.println(tid);
				if(!historyHash.contains(gei.edge)) {
					history.add(gei.edge);
					historyHash.add(gei.edge);
				}
				if(suggestionMode == 1) {
					// Passive event
					System.out.println("adding " + gei.source + " " + gei.edge + " " + gei.object);
					DestNode dn = new DestNode();
					dn.setDest(gei.object);
					dn.setForwardEdge(true);
					addNewEdgeToGraph(gei.source, gei.edge, dn, partialGraph);

					DestNode revdn = new DestNode();
					revdn.setDest(gei.source);
					revdn.setForwardEdge(false);
					addNewEdgeToGraph(gei.object, gei.edge, revdn, partialGraph);
				}
			}
		}
		if(suggestionMode == 0) {
			// Active event
			// We are supposed to have only one edgeEnds here.
			returnActivePartialGraph.source = activeEdgeEnds.source;
			returnActivePartialGraph.object = activeEdgeEnds.object;
		}
		return partialGraph;
	}

	private void addNewEdgeToGraph(int src, int edge, DestNode dn, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph) {
		HashMap<Integer, ArrayList<DestNode>> srcProp;
		if(partialGraph.containsKey(src)) {
			srcProp = partialGraph.get(src);
			if(srcProp.containsKey(edge)) {
				ArrayList<DestNode> dns = srcProp.get(edge);
				dns.add(dn);
			}
			else {
				ArrayList<DestNode> dns = new ArrayList<DestNode>();
				dns.add(dn);
				srcProp.put(edge, dns);
				partialGraph.put(src, srcProp);
			}
		}
		else {
			srcProp = new HashMap<Integer, ArrayList<DestNode>>();
			ArrayList<DestNode> dns = new ArrayList<DestNode>();
			dns.add(dn);
			srcProp.put(edge, dns);
			partialGraph.put(src, srcProp);
		}

	}

	public void getNegativeEdgesHistory(ArrayList<GuiEdgeInfo> rejectedGraph, 
			ArrayList<Integer> history, HashSet<Integer> historyHash, HashSet<MutableString> seenTuples) {
		System.out.println("Printing rejected edges");
		for(GuiEdgeInfo gei : rejectedGraph) {
			MutableString tid = getTupleID(gei.source, gei.edge, gei.object); 
			seenTuples.add(tid);
			System.out.println(tid);
			if(!historyHash.contains(gei.edge*(-1))) {
				history.add(gei.edge*(-1));
				historyHash.add(gei.edge*(-1));
			}
		}
	}

	public BackendResponseObject getCandidateTriples(ArrayList<Integer> rankedCandidateEdges, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, Config conf) {
		BackendResponseObject backObj = new BackendResponseObject();
		HashSet<Integer> seenEdges = new HashSet<Integer>();
		System.out.println("************* REBUILDING CANDIDATES *************** " + rankedCandidateEdges.size());
		BufferedRandomAccessFile braf = null;
		try {
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.instancesIdsortedLangEnAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesIdsortedLangEnNumberOfLines));
			braf = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.instancesIdsortedLangEnPaddedFile), "r", numOfLines, bufferSize);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("Number of edges in ranked list: " + rankedCandidateEdges.size());
		for(int edge : rankedCandidateEdges) {
			//System.out.println(edge);
			ArrayList<CandidateEdgeEnds> cands = edgeToTripletMap.get(edge);
			for(CandidateEdgeEnds cand : cands) {
				String srcLabel = getNodeLabel(cand.source, braf);
				String objLabel = getNodeLabel(cand.object, braf);
				if(srcLabel == null || objLabel == null)
					continue;
				String edgeStr = edge + "|" + edgeLabel.get(edge);
				String sourceStr = cand.source + "|" + srcLabel;
				String objectStr = cand.object + "|" + objLabel;
				GuiEdgeStringInfo gei = new GuiEdgeStringInfo();
				gei.edge = edgeStr;
				gei.source = sourceStr;
				gei.object = objectStr;
				backObj.rankedEdges.add(gei);
				if(!seenEdges.contains(edge)) {
					seenEdges.add(edge);
					GuiEdgeStringInfo geiunique = new GuiEdgeStringInfo();
					geiunique.edge = edgeStr;
					geiunique.source = sourceStr;
					geiunique.object = objectStr;
					backObj.rankedUniqueEdges.add(geiunique);
				}
			}
		}
		System.out.println("******************** ALL FINE ********************");
		return backObj;
	}

	private String getNodeLabel(int node, BufferedRandomAccessFile braf) {
		String label = null;
		if(nodeLabel.containsKey(node))
			label = nodeLabel.get(node);
		else {
			label = braf.getNodeLabel(node);
		}
		return label;
	}

	private HashSet<Integer> getCandidateEdgesYago(int suggestionMode, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			CandidateEdgeEnds activeModePartialGraph, HashSet<MutableString> seenTuples, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		isYagoDataset = true;
		return candidateEdges;
	}

	private HashSet<Integer> getCandidateEdgesDBpedia(int suggestionMode, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			CandidateEdgeEnds activeModePartialGraph, HashSet<MutableString> seenTuples, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		isDbpediaDataset = true;
		return candidateEdges;
	}

	public HashSet<Integer> getCandidateEdges(int suggestionMode, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			CandidateEdgeEnds activeModePartialGraph, HashSet<MutableString> seenTuples, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, int dataGraphInUse) {
		HashSet<Integer> candidateEdges;
		if(dataGraphInUse == 0)
			candidateEdges = getCandidateEdgesFreebase(suggestionMode, partialGraph, activeModePartialGraph, seenTuples, edgeToTripletMap);
		else if(dataGraphInUse == 1)
			candidateEdges = getCandidateEdgesDBpedia(suggestionMode, partialGraph, activeModePartialGraph, seenTuples, edgeToTripletMap);
		else
			candidateEdges = getCandidateEdgesYago(suggestionMode, partialGraph, activeModePartialGraph, seenTuples, edgeToTripletMap);
		// close the random file reader handler.
		try {
			sourceDataGraphFileHandler.close();
			objectDataGraphFileHandler.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return candidateEdges;
	}

	private HashSet<Integer> getCandidateEdgesFreebase(int suggestionMode, HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			CandidateEdgeEnds activeModePartialGraph, HashSet<MutableString> seenTuples, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		HashSet<Integer> candidateEdges = new HashSet<Integer>();
		isFreebaseDataset = true;
		if(suggestionMode == 0) {
			// Active event
			initializeCandidateEdgesActive(activeModePartialGraph, candidateEdges, seenTuples, edgeToTripletMap);
		}
		else {
			// Passive event
			initializeCandidateEdgesPassive(partialGraph, candidateEdges, seenTuples, edgeToTripletMap);
		}
		return candidateEdges;
	}

	/**
	 * 
	 * @param activeModePartialGraph
	 * @param candidateEdges
	 * @param seenTuples
	 * @param edgeToTripletMap
	 */
	private void initializeCandidateEdgesActive(CandidateEdgeEnds activeModePartialGraph, 
			HashSet<Integer> candidateEdges, HashSet<MutableString> seenTuples, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		//TODO: What if vertex 1 or vertex 2 is not assigned any type. Do we allow any one vertex to have a proper value or do we enforce
		// a valid node value for both vertices in active mode?
		int vertex1 = activeModePartialGraph.source;
		int vertex2 = activeModePartialGraph.object;

		HashSet<Integer> vertex1_Edges = getEdgesForVertex(vertex1, seenTuples, null);
		HashSet<Integer> vertex2_Edges = getEdgesForVertex(vertex2, seenTuples, null);
		HashSet<Integer> otherEdgeComparator;
		Iterator<Integer> iter;
		if(vertex1_Edges.size() < vertex2_Edges.size()) {
			iter = vertex1_Edges.iterator();
			otherEdgeComparator = vertex2_Edges;
		}
		else {
			iter = vertex2_Edges.iterator();
			otherEdgeComparator = vertex1_Edges;
		}
		while(iter.hasNext()) {
			int e = iter.next();
			if(otherEdgeComparator.contains(e)) {
				candidateEdges.add(e);
				CandidateEdgeEnds ce = new CandidateEdgeEnds();
				ce.source = vertex1;
				ce.object = vertex2;
				ArrayList<CandidateEdgeEnds> cee;
				if(edgeToTripletMap.containsKey(e)) {
					cee = edgeToTripletMap.get(e);
					cee.add(ce);
				}
				else {
					cee = new ArrayList<CandidateEdgeEnds>();
					cee.add(ce);
					edgeToTripletMap.put(e, cee);
				}
			}
		}
		System.out.println("Number of edges for 1 = " + vertex1_Edges.size());
		System.out.println("Number of edges for 2 = " + vertex2_Edges.size());
		System.out.println("Number of common edges = " + candidateEdges.size());
		//System.out.println("Printing seen tuples");
		//printList(seenTuples);
	}

	/**
	 * vertex contains the node for which we must find candidate incident edges
	 * edgeToTripletMap is null when this method is called from active mode. This method is called in passive mode ONLY when there is a single
	 * node in the partial query graph. It should NOT be called in passive mode otherwise.
	 * @param vertex
	 * @param seenTuples
	 * @param edgeToTripletMap
	 * @return
	 */
	private HashSet<Integer> getEdgesForVertex(int vertex, HashSet<MutableString> seenTuples, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		HashSet<Integer> edges = new HashSet<Integer>();
		System.out.println("Vertex = " + vertex);
		if(vertex == 0) {
			System.out.println("inside case where vertex = 0. We shouldn't be coming here!");
			// Passive mode suggestions on empty canvas is not allowed at the moment. So we should NOT be coming in here at all!
			// this means the value for this vertex was not selected. The entire list of edges will be selected.
			Iterator<Integer> iter = edgeType.keySet().iterator();
			while(iter.hasNext()) {
				edges.add(iter.next());
			}
		}
		/*if(vertex < 0 && vertex > -1000) {
			// Choosing a domain as the node value is NOT allowed at the moment. So we should NOT be coming in here at all!
			// this is a domain
			ArrayList<Integer> types = domainsToTypesIndex.get(vertex);
			if(types != null) {
				for(int type : types) {
					if(typeToEdgeTypeIndex.containsKey(type)) {
						ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(type);
						if(typeToEdgeType == null) 
							continue;
						for(int edgeType : typeToEdgeType) {
							getSrcObjTypeEdges(edgeType, edges);
						}
					}
				}
			}
		}
		else if(typeToEdgeTypeIndex.containsKey(vertex)) {
			// this is a type node
			if(edgeToTripletMap != null) {
				// This condition is when there is a single node in the partial query graph. 
				System.out.println("Passive mode, single node in partial graph, finding edge");
				ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(vertex);
				for(int edgeType : typeToEdgeType) {
					getSrcObjTypeEdges(vertex, edgeType, edges, edgeToTripletMap);
				}
			}
			else {
				System.out.println("Active mode, finding edge");
				ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(vertex);
				for(int edgeType : typeToEdgeType) {
					getSrcObjTypeEdges(edgeType, edges);
				}
			}
		}*/
		else if((srcTypeEdges.containsKey(vertex) || objTypeEdges.containsKey(vertex))) {
			// this is an edgeType node.
			if(edgeToTripletMap != null) {
				// This condition is when there is a single node in the partial query graph. 
				System.out.println("Passive mode, single node in partial graph, finding edge");
				/*HashSet<Integer> types = invertedNodeTypes.get(vertex);
				System.out.println("Types related to vertex " + nodeLabel.get(vertex));
				getSrcObjTypeEdges(vertex, edges);
				for(int edgeType : types) {
					System.out.print(nodeLabel.get(edgeType) + "   ,    ");
					getSrcObjTypeEdges(edgeType, edges, edgeToTripletMap);
				}*/
				getSrcObjTypeEdges(vertex, edges, edgeToTripletMap);
			}
			else {
				System.out.println("Active mode, getting candidates for node " + vertex);
				HashSet<Integer> types = invertedNodeTypes.get(vertex);
				System.out.println("Types related to vertex " + nodeLabel.get(vertex));
				getSrcObjTypeEdges(vertex, edges);
				for(int edgeType : types) {
					System.out.print(nodeLabel.get(edgeType) + "   ,    ");
					getSrcObjTypeEdges(edgeType, edges);
				}
				System.out.println();
			}
		}
		else {
			System.out.println("This is an entity!");
			// this is an entity in the data graph
			// if this vertex is an intermediate node, don't bother adding anything.
			if(edgeToTripletMap != null && !intermediateNodesList.contains(vertex)) {
				// This condition is when there is a single node in the partial query graph.
				// this vertex is an actual node from the data graph.
				// add all those edges where vertex is the source...
				HashSet<MutableString> addedTuples = new HashSet<MutableString>();
				ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
				addEdgeTypeEdge(ons, vertex, addedTuples, seenTuples, edges, edgeToTripletMap, true);
				ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
				addEdgeTypeEdge(revons, vertex, addedTuples, seenTuples, edges, edgeToTripletMap, false);
			}
			else {
				// This condition is when there is an active edge drawn between either two entity nodes or an entity and type node,
				// AND this "vertex" is an entity in the data graph
				ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
				addEdgeTypeEdge(ons, vertex, edges);
				ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
				addEdgeTypeEdge(revons, vertex, edges);
			}
		}
		return edges;
	}

	private void addEdgeTypeEdge(ArrayList<ObjNodeIntProperty> objNodes, int vertex, 
			HashSet<MutableString> addedTuples, HashSet<MutableString> seenTuples, HashSet<Integer> edges,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap, boolean forwardEdge) {
		//ArrayList<ObjNodeIntProperty> ons = dataGraph.get(vertex);
		if(objNodes != null) {
			for(ObjNodeIntProperty on : objNodes) {
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				if(forwardEdge) {
					cee.source = vertex;
					cee.object = on.dest;
				}
				else {
					cee.source = on.dest;
					cee.object = vertex;
				}
				if(edgeToTripletMap.containsKey(on.prop)) {
					edgeToTripletMap.get(on.prop).add(cee);
				}
				else {
					ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
					ce.add(cee);
					edgeToTripletMap.put(on.prop, ce);
				}
				MutableString tid = getTupleID(vertex, on.prop, on.dest);
				if(!addedTuples.contains(tid) && !seenTuples.contains(tid)) {
					addedTuples.add(tid);
					edges.add(on.prop);
				}
			}
		}
	}
	private void addEdgeTypeEdge(ArrayList<ObjNodeIntProperty> objNodes, int vertex, HashSet<Integer> edges) {
		//ArrayList<ObjNodeIntProperty> ons = dataGraph.get(vertex);
		if(objNodes != null) {
			for(ObjNodeIntProperty on : objNodes) {
				edges.add(on.prop);
			}
		}
	}

	private void getSrcObjTypeEdges(int vertex, HashSet<Integer> edges, 
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		// the originalVertex is the source!
		HashSet<Integer> srcType = srcTypeEdges.get(vertex);
		if(srcType != null) {
			int source = vertex;
			Iterator<Integer> iter = srcType.iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				edges.add(edge);
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				cee.source = source;
				cee.object = edgeType.get(edge).object_type;
				if(edgeToTripletMap.containsKey(edge)) {
					edgeToTripletMap.get(edge).add(cee);
				}
				else {
					ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
					ce.add(cee);
					edgeToTripletMap.put(edge, ce);
				}
			}
		}

		// the originalVertex is the object!
		HashSet<Integer> objType = objTypeEdges.get(vertex);
		if(objType != null) {
			int object = vertex;
			Iterator<Integer> iter = objType.iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				edges.add(edge);
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				cee.source = edgeType.get(edge).source_type;
				cee.object = object;
				if(edgeToTripletMap.containsKey(edge)) {
					edgeToTripletMap.get(edge).add(cee);
				}
				else {
					ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
					ce.add(cee);
					edgeToTripletMap.put(edge, ce);
				}
			}
		}
	}

	private void getSrcObjTypeEdges(int vertex, HashSet<Integer> edges) {
		getEdgeTypeEdges(srcTypeEdges, vertex, edges);
		getEdgeTypeEdges(objTypeEdges, vertex, edges);
	}

	private void getEdgeTypeEdges(HashMap<Integer, HashSet<Integer>> typeEdges, int vertex, HashSet<Integer> edges) {
		HashSet<Integer> srcType = typeEdges.get(vertex);
		if(srcType != null) {
			Iterator<Integer> iter = srcType.iterator();
			while(iter.hasNext())
				edges.add(iter.next());
		}
	}

	private void initializeCandidateEdgesPassive(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph, 
			HashSet<Integer> candidateEdges, HashSet<MutableString> seenTuples, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		// for every node present in the initial partial graph, add the neighboring edges (not in the partial graph) to the candidate
		// graph. also add these edges to candidate edges list.
		System.out.println("Printing seen tuples from GUI");
		printList(seenTuples);
		System.out.println("Printing added tuples");
		HashSet<MutableString> addedTuples = new HashSet<MutableString>();
		if(partialGraph.isEmpty()) {
			// The input could be empty. All edges are candidate edges.
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
		}
		else if(partialGraph.size() == 1) {
			/*
			 * Check if the partial query graph has only one entry, it means it only contains a single node in it. This is the case where
			 * the user has just drawn a new node and selected its value and the passive mode kicked in.
			 * 
			 * ADD all the possible edges that are incident on the node in the partial query graph as the input.
			 */
			int vertex = partialGraph.keySet().iterator().next();
			HashSet<Integer> tmp = getEdgesForVertex(vertex, seenTuples, edgeToTripletMap);
			Iterator<Integer> iter = tmp.iterator();
			while(iter.hasNext()) {
				candidateEdges.add(iter.next());
			}
		}
		else {
			Iterator<Integer> iter = partialGraph.keySet().iterator();
			while(iter.hasNext()) {
				int vertex = iter.next();
				logger.debug(" NEW VERTEX = " + vertex);
				System.out.println(" NEW VERTEX = ========================== " + vertex);
				/*if(vertex < 0 && vertex > -1000) {
					// this is a domain node.
					addTypeNodeNeighborEdgesDomain(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, seenTuples, vertex);
					ArrayList<Integer> types = domainsToTypesIndex.get(vertex);
					if(types != null) {
						for(int type : types) {
							ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(type);
							for(int edgeType : typeToEdgeType) {
								addTypeNodeNeighborEdges(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, edgeType);
							}
						}
					}
				}
				else if(typeToEdgeTypeIndex.containsKey(vertex)) {
					logger.debug("Inside type to edge type if condition");
					// this vertex is the actual TYPE node.
					addTypeNodeNeighborEdgesType(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, seenTuples, vertex);
					ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(vertex);
					for(int edgeType : typeToEdgeType) {
						addTypeNodeNeighborEdges(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, edgeType);
					}
				}*/
				if((srcTypeEdges.containsKey(vertex) || objTypeEdges.containsKey(vertex))) {
					// This vertex is an EdgeType node.
					addTypeNodeNeighborEdgesEdgetype(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, seenTuples, vertex);
					//addTypeNodeNeighborEdges(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, vertex);
				}
				else {
					// if this vertex is an intermediate node, don't bother adding anything.
					if(isFreebaseDataset && intermediateNodesList.contains(vertex))
						continue;
					// this vertex is an actual node from the data graph.
					// add all those edges where vertex is the source...
					//ArrayList<ObjNodeIntProperty> ons = srcDataGraph.get(vertex);
					ArrayList<ObjNodeIntProperty> ons = sourceDataGraphFileHandler.getVertexNeighbors(vertex);
					if(ons != null) {
						getCandidateEdge(candidateEdges, seenTuples, addedTuples, vertex, ons, edgeToTripletMap);
					}
					// add all those edges where vertex is the object...
					//ArrayList<ObjNodeIntProperty> revons = objDataGraph.get(vertex);
					ArrayList<ObjNodeIntProperty> revons = objectDataGraphFileHandler.getVertexNeighbors(vertex);
					if(revons != null) {
						getCandidateEdge(candidateEdges, seenTuples, addedTuples, vertex, revons, edgeToTripletMap);
					}
				}
			}
		}

		//printList(addedTuples);
		//printLists(seenTuples, addedTuples);
	}

	private void printLists(HashSet<MutableString> seenTuples, HashSet<MutableString> addedTuples) {
		System.out.println("Printing seen tuples from gui");
		printList(seenTuples);
		System.out.println("Printing added tuples");
		printList(addedTuples);
	}
	private void printList(HashSet<MutableString> l) {
		Iterator<MutableString> iter = l.iterator();
		while(iter.hasNext()) {
			System.out.println(iter.next());
		}
	}

	/*private void addTypeNodeNeighborEdgesDomain(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashSet<Integer> candidateEdges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap,
			HashSet<MutableString> addedTuples, HashSet<MutableString> seenTuples, int domain) {
		ArrayList<Integer> types = domainsToTypesIndex.get(domain);
		if(types != null) {
			for(int type : types) {
				if(typeToEdgeTypeIndex.containsKey(type)) {
					ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(type);
					if(typeToEdgeType == null) 
						continue;
					for(int t : typeToEdgeType) {
						addTypeBasedEdges(addedTuples, seenTuples, domain, t, candidateEdges, partialGraph.get(domain), edgeToTripletMap);
						//addTypeNodeNeighborEdges(partialGraph, candidateEdges, edgeToTripletMap, addedTuples, edgeType);
					}
				}
			}
		}
	}

	private void addTypeNodeNeighborEdgesType(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashSet<Integer> candidateEdges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap,
			HashSet<MutableString> addedTuples, HashSet<MutableString> seenTuples, int type) {
		if(typeToEdgeTypeIndex.containsKey(type)) {
			ArrayList<Integer> typeToEdgeType = typeToEdgeTypeIndex.get(type);
			if(typeToEdgeType != null) {
				for(int edgeType : typeToEdgeType) {
					addTypeBasedEdges(addedTuples, seenTuples, type, edgeType, candidateEdges, partialGraph.get(type), edgeToTripletMap);
				}
			}
		}
	}*/

	private void addTypeNodeNeighborEdgesEdgetype(HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph,
			HashSet<Integer> candidateEdges, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap,
			HashSet<MutableString> addedTuples, HashSet<MutableString> seenTuples, int edgeType) {

		addTypeBasedEdges(addedTuples, seenTuples, edgeType, edgeType, candidateEdges, partialGraph.get(edgeType), edgeToTripletMap);
		HashSet<Integer> types = invertedNodeTypes.get(edgeType);
		if(types != null) {
			System.out.println("Number of instances that nodes of " + edgeType + " belong to = " + types.size());
			Iterator<Integer> typesIter = types.iterator();
			while(typesIter.hasNext()) {
				int type = typesIter.next();
				System.out.print(type + " ");
				addTypeBasedEdges(addedTuples, seenTuples, edgeType, type, candidateEdges, partialGraph.get(edgeType), edgeToTripletMap);
			}
			System.out.println();
		}
	}

	private void addTypeBasedEdges(HashSet<MutableString> addedTuples, HashSet<MutableString> seenTuples, int actualType, int type,
			HashSet<Integer> candidateEdges, HashMap<Integer, ArrayList<DestNode>> actualTypeIncidentEdgeList,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		/*
		 * The problem with both ends of the node being a TYPE and not an instance (actual node in the data graph) is that
		 * the signature in "seenTuple" does not identify something unique. An edge that was suggested early on, but was rejected
		 * because it was not what the user was looking for at that particular partial graph instance, cannot be suggested again
		 * because "seenTuples" will preclude that from happening.
		 * So, we do not check if this type-prop-type signature is in "seenTuples" or not. We add it anyway.
		 */
		// edges incident on this node type being source.
		HashSet<Integer> seenEdgesForThisType = new HashSet<Integer>();
		if(srcTypeEdges.containsKey(type)) {
			HashSet<Integer> edgeList = srcTypeEdges.get(type);
			Iterator<Integer> iter = edgeList.iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				if(edgeLeadsToIntermediateConcatenation(edge, actualTypeIncidentEdgeList))
					continue;
				if(seenEdgesForThisType.contains(edge))
					continue;
				seenEdgesForThisType.add(edge);
				addTypeEdge(addedTuples, seenTuples, candidateEdges, edge, actualType, type, edgeToTripletMap);
			}
		}
		// edges incident on this node type being object.
		if(objTypeEdges.containsKey(type)) {
			HashSet<Integer> edgeList = objTypeEdges.get(type);
			Iterator<Integer> iter = edgeList.iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				if(edgeLeadsToIntermediateConcatenation(edge, actualTypeIncidentEdgeList))
					continue;
				if(seenEdgesForThisType.contains(edge))
					continue;
				seenEdgesForThisType.add(edge);
				addTypeEdge(addedTuples, seenTuples, candidateEdges, edge, actualType, type, edgeToTripletMap);
			}
		}
	}

	private void getCandidateEdge(HashSet<Integer> candidateEdges, HashSet<MutableString> seenTuples,
			HashSet<MutableString> addedTuples, int vertex, ArrayList<ObjNodeIntProperty> ons,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		for(ObjNodeIntProperty on : ons) {
			if(isFreebaseDataset && intermediateNodesList.contains(on.dest)) {
				// get other nodes connected to this intermediate node.
				concatenateEdgesAndAddProp(candidateEdges, seenTuples, addedTuples, vertex, on, edgeToTripletMap);
			}
			else {
				MutableString tid = getTupleID(vertex, on.prop, on.dest);
				if(!addedTuples.contains(tid) && !seenTuples.contains(tid)) {
					//	addedEdges.add(on.prop);
					EdgeEnds ee = new EdgeEnds();
					ee.source = vertex;
					ee.object = on.dest;
					CandidateEdgeEnds cee = new CandidateEdgeEnds();
					cee.source = ee.source;
					cee.object = ee.object;
					if(edgeToTripletMap.containsKey(on.prop)) {
						edgeToTripletMap.get(on.prop).add(cee);
					}
					else {
						ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
						ce.add(cee);
						edgeToTripletMap.put(on.prop, ce);
					}
					addToCandidateGraph(candidateEdges, addedTuples, on.prop, ee, tid);
				}
			}
		}
	}

	private void concatenateEdgesAndAddProp(HashSet<Integer> candidateEdges, HashSet<MutableString> seenTuples,
			HashSet<MutableString> addedTuples, int vertex, ObjNodeIntProperty on,
			HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
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
			MutableString tid = getTupleID(vertex, newEdgeId, interEdge.node);
			if(!addedTuples.contains(tid) && !seenTuples.contains(tid)) {
				EdgeEnds ee = new EdgeEnds();
				if(on.prop < interEdge.prop) {
					ee.source = vertex;
					ee.object = interEdge.node;
				}
				else {
					ee.source = interEdge.node;
					ee.object = vertex;
				}
				CandidateEdgeEnds cee = new CandidateEdgeEnds();
				cee.source = ee.source;
				cee.object = ee.object;
				if(edgeToTripletMap.containsKey(newEdgeId)) {
					edgeToTripletMap.get(newEdgeId).add(cee);
				}
				else {
					ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
					ce.add(cee);
					edgeToTripletMap.put(newEdgeId, ce);
				}
				addToCandidateGraph(candidateEdges, addedTuples, newEdgeId, ee, tid);
			}
		}
	}

	private ArrayList<IntermediateNodeAndOtherEnd> getOtherEdgesOnIntermediateNode(int firstNode, int firstProp, int intermediateNode) {
		ArrayList<IntermediateNodeAndOtherEnd> intermediateEdges = new ArrayList<IntermediateNodeAndOtherEnd>();
		if(srcDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNodeIntProperty> objList = srcDataGraph.get(intermediateNode);
			for(ObjNodeIntProperty on : objList) {
				if(on.prop != firstProp && on.dest != firstNode) {
					IntermediateNodeAndOtherEnd ina = new IntermediateNodeAndOtherEnd();
					ina.node = on.dest;
					ina.prop = on.prop;
					intermediateEdges.add(ina);
				}
			}
		}
		if(objDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNodeIntProperty> objList = objDataGraph.get(intermediateNode);
			for(ObjNodeIntProperty on : objList) {
				if(on.prop != firstProp && on.dest != firstNode) {
					IntermediateNodeAndOtherEnd ina = new IntermediateNodeAndOtherEnd();
					ina.node = on.dest;
					ina.prop = on.prop;
					intermediateEdges.add(ina);
				}
			}
		}
		return intermediateEdges;
	}

	private void addToCandidateGraph(HashSet<Integer> candidateEdges,
			HashSet<MutableString> addedTuples, int prop, EdgeEnds ee, MutableString tid) {
		candidateEdges.add(prop);
		//System.out.println(tid);
		addedTuples.add(tid);
		/*ArrayList<EdgeEnds> edges;
		if(candidateGraph.containsKey(prop)) {
			edges = candidateGraph.get(prop);
			edges.add(ee);
		}
		else {
			edges = new ArrayList<EdgeEnds>();
			edges.add(ee);
		}
		candidateGraph.put(prop, edges);*/
	}

	/**
	 * Look at the existing edges incident on the actualType (this is the node in the partial graph). If any of these edges, when concatenated
	 * with the newly found edge newEdge leads to a concatenated intermediate edge, then we must ignore it.
	 * @param newEdge
	 * @param actualTypeIncidentEdgeList
	 * @return
	 */
	private boolean edgeLeadsToIntermediateConcatenation(int newEdge, HashMap<Integer, ArrayList<DestNode>> actualTypeIncidentEdgeList) {
		boolean formsConcatenatedEdge = false;
		Iterator<Integer> iter = actualTypeIncidentEdgeList.keySet().iterator();
		while(iter.hasNext()) {
			int edge = iter.next();
			String concatEdge;
			if(edge < newEdge)
				concatEdge = edge + "," + newEdge;
			else
				concatEdge = newEdge + "," + edge;
			if(concatenatedStringEdgesToNewEdgeIdMap.containsKey(concatEdge)) {
				formsConcatenatedEdge = true;
				break;
			}
		}
		return formsConcatenatedEdge;
	}

	private void addTypeEdge(HashSet<MutableString> addedTuples, HashSet<MutableString> seenTuples, HashSet<Integer> candidateEdges, 
			int edge, int actualType, int type, HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap) {
		EdgeTypeInfo eti = edgeType.get(edge);    
		int srctype = eti.source_type;
		int objtype = eti.object_type;
		if(srctype == type)
			srctype = actualType;
		else
			objtype = actualType;
		MutableString tid = getTupleID(srctype, edge, objtype);
		if(edge == 30012) {
			System.out.println("\nActual node value = " + actualType + " Node type value = " + type);
			System.out.println("Actual ends of edge " + edge + " = " + eti.source_type + " " + eti.object_type);
			System.out.println("Chosen ends of edge " + edge + " = " + srctype + " " + objtype);
		}
		if(!addedTuples.contains(tid) && !seenTuples.contains(tid)) {
			EdgeEnds ee = new EdgeEnds();
			ee.source = eti.source_type;
			ee.object = eti.object_type;
			CandidateEdgeEnds cee = new CandidateEdgeEnds();
			cee.source = srctype;
			cee.object = objtype;
			if(edgeToTripletMap.containsKey(edge)) {
				edgeToTripletMap.get(edge).add(cee);
			}
			else {
				ArrayList<CandidateEdgeEnds> ce = new ArrayList<CandidateEdgeEnds>();
				ce.add(cee);
				edgeToTripletMap.put(edge, ce);
			}
			addToCandidateGraph(candidateEdges, addedTuples, edge, ee, tid);
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
}
