package viiq.clientServer.server;

import viiq.graphQuerySuggestionMain.GraphQuerySuggestionConstants;
import viiq.utils.PropertyKeys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Random;
import java.lang.OutOfMemoryError;

import viiq.commons.GuiEdgeStringInfo;
import viiq.commons.EdgeTypeInfo;
import viiq.commons.ObjNodeIntProperty;
import viiq.commons.Pair;

public class LoadData
{
	/**
	 * FREEBASE/datagraph specific data structures.
	 */
	static HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	static HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	// store all the types associated with an node/entity/vertex. The types are derived as the union of the type associated with this node
	// on all the edges it is incident on.
	// key = vertex ID, value = set of all types of this vertex
	//TIntObjectHashMap<TIntHashSet> abc = new TIntObjectHashMap<>();
	static HashMap<Integer, HashSet<Integer>> nodeTypes = new HashMap<Integer, HashSet<Integer>>();
	// a node can be of multiple types. but the ends of an edge is of one specific type each. the neighboring edges associated with a node type
	// is not just the edges that are incident on this particular node type, but are those associated with other types that an instance of
	// this node type may have incident on them.
	// for example a node type "Founder" may also have edges that are incident on "Person" too, since every "Founder" will be a "Person" too.
	// key = node type, value = (other node types that instances of the "key" belong to)
	static HashMap<Integer, HashSet<Integer>> invertedNodeTypes = new HashMap<Integer, HashSet<Integer>>();
	// set of nodes in the graph that are intermediate nodes.
	static HashSet<Integer> intermediateNodesList = new HashSet<Integer>();
	// key = concatenation of two edges connecting an intermediate node, value = new edge ID corresponding to the key
	static HashMap<String, Integer> concatenatedStringEdgesToNewEdgeIdMap = new HashMap<String, Integer>();
	HashMap<Integer, HashSet<Integer>> intermediateEdgeToNewEdgeIdMap = new HashMap<Integer, HashSet<Integer>>();

	// store the type of the two ends of an edge.
	// key = edge, value = (source vertex Type, dest vertex Type)
	static HashMap<Integer, EdgeTypeInfo> edgeType = new HashMap<Integer, EdgeTypeInfo>();

	// this maintains all the TYPES associated with a domain.
	// key = domain, value = set of TYPES
//	static HashMap<Integer, ArrayList<Integer>> domainsToTypesIndex = new HashMap<Integer, ArrayList<Integer>>();
	// this maintains all the EdgeTypes associated with a TYPE.
	// key = TYPE, value = EdgeType
//	static HashMap<Integer, ArrayList<Integer>> typeToEdgeTypeIndex = new HashMap<Integer, ArrayList<Integer>>();

	// contains labels of edges. key: edge ID, value: label
	static HashMap<Integer, String> edgeLabelIndex = new HashMap<Integer, String>();
	// contains labels of nodes (domains, type, edgetype, entities). This assumes all IDs are unique!
	// key: node ID, value: node label
	static HashMap<Integer, String> nodeLabelIndex = new HashMap<Integer, String>();

	// key: type, value: edges whose source type is the key. (generated using all the instances of "type" in the data graph)
	static HashMap<Integer, HashSet<Integer>> sourceTypesToEdgesMap = new HashMap<Integer, HashSet<Integer>>();

	// key: type, value: edges whose object type is the key. (generated using all the instances of "type" in the data graph)
	static HashMap<Integer, HashSet<Integer>> objectTypesToEdgesMap = new HashMap<Integer, HashSet<Integer>>();

	static HashMap<Integer, ArrayList<String>> edgeExamples = new HashMap<Integer, ArrayList<String>>();

	// key: type, value: the count of number of instances associated with the type
	static HashMap<Integer, Integer> typeInstancesCount = new HashMap<Integer, Integer>();

	// key: edgetype, value: the count of number of instances associated with the edgetype
	static HashMap<Integer, Integer> edgeInstancesCount = new HashMap<Integer, Integer>();

	// key: type, value: edges whose object type is the key. (generated using all the candidate of "subject" in the end types)
	static HashMap<Integer, HashSet<Integer>> candidateSubject = new HashMap<Integer, HashSet<Integer>>();

	// key: type, value: edges whose object type is the key. (generated using all the candidate of "target" in the end types)
	static HashMap<Integer, HashSet<Integer>> candidateObject = new HashMap<Integer, HashSet<Integer>>();

	// key: type, value: entities belonging to the type
	static HashMap<Integer, LinkedHashSet<Integer>> typetoEntitiesMap = new HashMap<Integer, LinkedHashSet<Integer>>();

	static HashMap<Integer, Integer> typeDomainMap = new HashMap<Integer, Integer>();

	static HashMap<Integer, String> domainLabelMap = new HashMap<Integer, String>();

	static HashMap<String,  HashSet<String>> typesClique = new HashMap<String,  HashSet<String>>();

	public static HashMap<Integer, Integer> getTypeInstancesCount() {
		return typeInstancesCount;
	}

	public static HashMap<Integer, Integer> getEdgeInstancesCount() {
		return edgeInstancesCount;
	}

	public static void setTypeInstancesCount(
			HashMap<Integer, Integer> typeInstancesCount) {
		LoadData.typeInstancesCount = typeInstancesCount;
	}

	public static HashMap<Integer, ArrayList<String>> getEdgeExamples() {
		return edgeExamples;
	}

	public static void setEdgeExamples(HashMap<Integer, ArrayList<String>> edgeExamples) {
		LoadData.edgeExamples = edgeExamples;
	}

	public static HashMap<Integer, HashSet<Integer>> getSourceTypesToEdgesMap() {
		return sourceTypesToEdgesMap;
	}

	public static void setSourceTypesToEdgesMap(
			HashMap<Integer, HashSet<Integer>> sourceTypesToEdgesMap) {
		LoadData.sourceTypesToEdgesMap = sourceTypesToEdgesMap;
	}

	public static HashMap<Integer, HashSet<Integer>> getObjectTypesToEdgesMap() {
		return objectTypesToEdgesMap;
	}

	public static void setObjectTypesToEdgesMap(
			HashMap<Integer, HashSet<Integer>> objectTypesToEdgesMap) {
		LoadData.objectTypesToEdgesMap = objectTypesToEdgesMap;
	}

	static int numOfTotalEdges = 0;

	public static int getNumOfTotalEdges() {
		return numOfTotalEdges;
	}

	public static HashMap<Integer, String> getEdgeLabelIndex() {
		return edgeLabelIndex;
	}

	public static HashMap<Integer, String> getNodeLabelIndex() {
		return nodeLabelIndex;
	}

	public static HashMap<String,  HashSet<String>> getTypesClique() {
		return typesClique;
	}

	/*public HashMap<Integer, ArrayList<Integer>> getDomainsToTypesIndex() {
		return domainsToTypesIndex;
	}

	public HashMap<Integer, ArrayList<Integer>> getTypeToEdgeTypeIndex() {
		return typeToEdgeTypeIndex;
	}*/

	public static HashMap<Integer, ArrayList<ObjNodeIntProperty>> getSrcDataGraph() {
		return srcDataGraph;
	}

	/*public void setSrcDataGraph(
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGr) {
		srcDataGraph = srcDataGr;
	}*/

	public static HashMap<Integer, ArrayList<ObjNodeIntProperty>> getObjDataGraph() {
		return objDataGraph;
	}

	/*public void setObjDataGraph(
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph) {
		this.objDataGraph = objDataGraph;
	}*/

	public static HashMap<Integer, HashSet<Integer>> getNodeTypes() {
		return nodeTypes;
	}

	/*public void setNodeTypes(HashMap<Integer, HashSet<Integer>> nodeTypes) {
		this.nodeTypes = nodeTypes;
	}*/

	public static HashMap<Integer, HashSet<Integer>> getInvertedNodeTypes() {
		return invertedNodeTypes;
	}

	/*public void setInvertedNodeTypes(
			HashMap<Integer, HashSet<Integer>> invertedNodeTypes) {
		this.invertedNodeTypes = invertedNodeTypes;
	}*/

	public static HashSet<Integer> getIntermediateNodesList() {
		return intermediateNodesList;
	}

	/*public void setIntermediateNodesList(HashSet<Integer> intermediateNodesList) {
		this.intermediateNodesList = intermediateNodesList;
	}*/

	public static HashMap<String, Integer> getConcatenatedStringEdgesToNewEdgeIdMap() {
		return concatenatedStringEdgesToNewEdgeIdMap;
	}

	/*public void setConcatenatedEdgesToNewEdgeIdMap(
			HashMap<String, Integer> concatenatedEdgesToNewEdgeIdMap) {
		this.concatenatedEdgesToNewEdgeIdMap = concatenatedEdgesToNewEdgeIdMap;
	}*/

	public static HashMap<Integer, EdgeTypeInfo> getEdgeType() {
		return edgeType;
	}

	/*public void setEdgeType(HashMap<Integer, EdgeTypeInfo> edgeType) {
		this.edgeType = edgeType;
	}*/

	public static HashMap<Integer, HashSet<Integer>> getSrcTypeEdges() {
		return srcTypeEdges;
	}

	/*public void setSrcTypeEdges(HashMap<Integer, HashSet<Integer>> srcTypeEdges) {
		this.srcTypeEdges = srcTypeEdges;
	}*/

	public static HashMap<Integer, HashSet<Integer>> getObjTypeEdges() {
		return objTypeEdges;
	}

	/*public void setObjTypeEdges(HashMap<Integer, HashSet<Integer>> objTypeEdges) {
		this.objTypeEdges = objTypeEdges;
	}*/

	public static HashMap<Integer, String> getFreebaseWikiMap(){
		return freebaseWikiMap;
	}

  public static HashMap<Integer, String> getEntityPreviewMap(){
		return entityPreviewMap;
  }

	public static HashMap<Integer, ArrayList<String>> getEdgePreviewMap(){
		return edgePreviewMap;
	}

	public static HashMap<Integer, HashSet<Integer>> getCandidateSubject(){
		return candidateSubject;
	}

	public static HashMap<Integer, HashSet<Integer>> getCandidateObject(){
		return candidateObject;
	}

	public static HashMap<Integer, LinkedHashSet<Integer>> getEntitiesForType(){
		return typetoEntitiesMap;
	}

	public static HashMap<Integer, Integer> getTypeDomainMap(){
		return typeDomainMap;
	}

	public static HashMap<Integer, String> getDomainLabelMap(){
		return domainLabelMap;
	}

	public static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> getSrcNeighbors(){
		return srcNeighbors;
	}

	public static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> getObjNeighbors(){
		return srcNeighbors;
	}

	public static HashMap<Integer, HashSet<Integer>> getSrcEndEntities(){
		return srcEndEntities;
	}

	public static HashMap<Integer, HashSet<Integer>> getObjEndEntities(){
		return objEndEntities;
	}


	public static HashMap<Integer, HashSet<Integer>> getSrcEdges(){
		return srcEdges;
	}

	public static HashMap<Integer, HashSet<Integer>> getObjEdges(){
		return objEdges;
	}

	public static HashMap<Integer, LinkedHashSet<Pair>> getSrcPropTable(){
		return srcPropTable;
	}

	public static HashMap<Integer, LinkedHashSet<Pair>> getObjPropTable(){
		return objPropTable;
	}

	// Edges associated with each node type.
	// when this node type is of source.
	// key = source Type, value = set of all edges that have the key as source type
	static HashMap<Integer, HashSet<Integer>> srcTypeEdges = new HashMap<Integer, HashSet<Integer>>();
	// when this node type is of object.
	// key = object Type, value = set of all edges that have the key as object type
	static HashMap<Integer, HashSet<Integer>> objTypeEdges = new HashMap<Integer, HashSet<Integer>>();
	//key1: one end, key2: edge, value: other ends
	static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> srcNeighbors = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
	static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> objNeighbors = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
	static HashMap<Integer, HashSet<Integer>> srcEndEntities = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, HashSet<Integer>> objEndEntities = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, HashSet<Integer>> srcEdges = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, HashSet<Integer>> objEdges = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, LinkedHashSet<Pair>> srcPropTable = new HashMap<Integer, LinkedHashSet<Pair>>();
	static HashMap<Integer, LinkedHashSet<Pair>> objPropTable = new HashMap<Integer, LinkedHashSet<Pair>>();
	/************************** END of data graph specific DS *****************************************/


	private static ArrayList<String> queries = new ArrayList<String>();
	private static ArrayList<Integer> randomIndexes = new ArrayList<Integer>();
	private static int randomIndexID = 0;
	private static HashMap<Integer,String> freebaseWikiMap = new HashMap<Integer, String>();
  private static HashMap<Integer,String> entityPreviewMap = new HashMap<Integer, String>();
	private static HashMap<Integer,ArrayList<String>> edgePreviewMap = new HashMap<Integer, ArrayList<String>>();

	public void populateQueriesForUserStudy() {
		/*queries.add("Oneeeeeeeeeeeee\n1111111111");
		queries.add("Twooooooooooo\n22222222222222");
		queries.add("Threeeeeeeeeeee\n33333333333");
		queries.add("Fourrrrrrrr\n4444444444444444");
		queries.add("fiveeeeeeeeeeeeeeee\n5555555555555555");
		queries.add("sixxxxxx\n666666666666666");
		queries.add("sevennnnnnnnnn\n777777777777777");
		queries.add("eighttttttttttt\n888888888888888");
		queries.add("nineeeeeeeeeeeeeeee\n99999999999999");
		queries.add("tennnnnnnnnnn\n0000000000000000000");*/
		queries.add("Find the DIRECTOR-ACTOR-MOVIE triplet such that the actor is also the writer of the movie, and the movie is directed by an academy award winning director.");
		queries.add("Find all the FILMS which stars an ACTOR who was born in Israel and studied in Harvard University.");
		queries.add("Find the COUNTRY and its official LANGUAGE such that the country is a major TOURIST ATTRACTION and the language is spoken by people of Indian ethnicity, and belongs to the Indo-European family of languages.");
		queries.add("Find FILM and PRODUCER pairs such that the producer has also composed the music for the film, and both the film and producer have won the Academy Award.");
		queries.add("Find an American born ACTOR who has acted in both award winning FILM and TV PROGRAM.");
		queries.add("Find all BOOKS on the French revolution written by award winning AUTHORS.");
		populateRandomQueryIDs();
		/*for(int i=0; i<20; i++) {
			System.out.print(randomIndexes.get(i) + ", ");
		}
		System.out.println();*/
	}
	private void populateRandomQueryIDs() {
		int low = 0;
		int high = queries.size();
		Random rand = new Random();
		for(int i=0; i<1000; i++) {
			randomIndexes.add(rand.nextInt(high-low)+low);
		}
	}

	public static synchronized String getNextQuery() {
		String q = "";
		if(randomIndexID == randomIndexes.size()) {
			randomIndexID = 0;
		}
		q += randomIndexes.get(randomIndexID) + "|";
		q += queries.get(randomIndexes.get(randomIndexID++));
		/*if(randomIndexID < randomIndexes.size()) {
			q += randomIndexes.get(randomIndexID) + "|";
			q += queries.get(randomIndexes.get(randomIndexID++));
		} else {
			q = "-1|No more queries to work on!";
		}*/
		return q;
	}

	private void loadtypeToEdgesMap(String filepath, HashMap<Integer, HashSet<Integer>> typesToEdgesMap) {
		try {
			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] typeedges = line.split(":");
				if(typeedges.length != 2)
					continue;
				int type = Integer.parseInt(typeedges[0]);
				String[] edges = typeedges[1].split(",");
				HashSet<Integer> edgeList = new HashSet<Integer>();
				for(String edge : edges) {
					if(!edge.isEmpty())
						edgeList.add(Integer.parseInt(edge));
				}
				typesToEdgesMap.put(type, edgeList);
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void loadSourceTypeToEdgesMap(String filepath) {
		loadtypeToEdgesMap(filepath, sourceTypesToEdgesMap);
	}

	public void loadObjectTypeToEdgesMap(String filepath) {
		loadtypeToEdgesMap(filepath, objectTypesToEdgesMap);
	}

	public static boolean isTypeNode(int node) {
		if(sourceTypesToEdgesMap.containsKey(node) || objectTypesToEdgesMap.containsKey(node))
			return true;
		return false;
	}

	public void loadEdgeLabels(String inputFilePath) {
		// creating a separate method even though it does the same thing as loadNodeLabel just in case we need to handle more things for edge.
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] idLabel = line.split("\t");
					edgeLabelIndex.put(Integer.parseInt(idLabel[0]), idLabel[1].trim());
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void loadAllNodeLabels(String domainLabelPath, String typeLabelPath, String entityLabelPath) {
		loadNodeLabel(domainLabelPath, true, false);
		loadNodeLabel(typeLabelPath, false, false);
		loadNodeLabel(entityLabelPath, false, true);
	//	loadNodeLabel(edgeTypeLabelPath);
	}

	/**
	 * Input file is freebase to wiki mappinf file path:
	 * @param filebaseWikiPath
	 */
	public void loadAllfreebaseWiki(String freebaseWikiPath){
		try {
			BufferedReader br = new BufferedReader(new FileReader(freebaseWikiPath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] freeWiki = line.split("\t");
					freebaseWikiMap.put(Integer.parseInt(freeWiki[0]), freeWiki[1]);
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

 //load adjacent edges and end adjacent nodes for each node in datagraph
	public void loadEdgeEndEntities(String filepath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] tokens = line.split(",");
					int source = Integer.parseInt(tokens[1]);
					int edge = Integer.parseInt(tokens[2]);
					int target = Integer.parseInt(tokens[3].trim());

					if(!srcEndEntities.containsKey(edge)) {
						srcEndEntities.put(edge, new HashSet<Integer>());
					}
					if(!objEndEntities.containsKey(edge)) {
						objEndEntities.put(edge, new HashSet<Integer>());
					}
					srcEndEntities.get(edge).add(source);
					objEndEntities.get(edge).add(target);

					// if(!srcEdges.containsKey(source)) {
					// 	srcEdges.put(source, new HashSet<Integer>());
					// }
					// srcEdges.get(source).add(edge);
					
					// if(!objEdges.containsKey(target)) {
					// 	objEdges.put(target, new HashSet<Integer>());
					// }
					// objEdges.get(target).add(edge);

					// //for source node
					// if(!srcNeighbors.containsKey(source)) {
					// 	srcNeighbors.put(source, new HashMap<Integer, HashSet<Integer>>());
					// }
					// if(!srcNeighbors.get(source).containsKey(edge)) {
					// 	srcNeighbors.get(source).put(edge, new HashSet<Integer>());
					// }
					// srcNeighbors.get(source).get(edge).add(target);
					// //for object node
					// if(!objNeighbors.containsKey(target)) {
					// 	objNeighbors.put(target, new HashMap<Integer, HashSet<Integer>>());
					// }
					// if(!objNeighbors.get(target).containsKey(edge)) {
					// 	objNeighbors.get(target).put(edge, new HashSet<Integer>());
					// }
					// objNeighbors.get(target).get(edge).add(source);
				}
			}
			finally {
				br.close();
			}
		}
		catch(OutOfMemoryError oome) {
			System.out.println(srcEdges.size()+","+objEdges.size());
			oome.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void loadPropertyTable(String filepath, boolean isSourceAligned) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] tokens = line.split(",");
					int entity1 = Integer.parseInt(tokens[1]);
					int edge = Integer.parseInt(tokens[2]);
					int entity2 = Integer.parseInt(tokens[3].trim());

					if(isSourceAligned) {
						if(srcPropTable.containsKey(edge)) {
							srcPropTable.get(edge).add(new Pair(entity1, entity2));
						} else {
							srcPropTable.put(edge, new LinkedHashSet<Pair>());
						}
					} else {
						if(objPropTable.containsKey(edge)) {
							objPropTable.get(edge).add(new Pair(entity1, entity2));
						} else {
							objPropTable.put(edge, new LinkedHashSet<Pair>());
						}
					}
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void loadTypesClique(String typesCliqueFile, String typesPairCliqueFile) {
		try {
			BufferedReader br1 = new BufferedReader(new FileReader(typesCliqueFile));
			BufferedReader br2 = new BufferedReader(new FileReader(typesPairCliqueFile));
			try {
				String line = null;
				while((line = br1.readLine()) != null) {
					String[] tokens = line.split(",");
					if(typesClique.containsKey(tokens[0])) {
						typesClique.get(tokens[0]).add(tokens[1]);
					} else {
						typesClique.put(tokens[0], new HashSet<String>());
						typesClique.get(tokens[0]).add(tokens[1]);
					}
				}
				while((line = br2.readLine()) != null) {
					String[] tokens = line.split(",");
					if(typesClique.containsKey(tokens[0]+","+tokens[1])) {
						typesClique.get(tokens[0]+","+tokens[1]).add(tokens[2]);
					} else {
						typesClique.put(tokens[0]+","+tokens[1], new HashSet<String>());
						typesClique.get(tokens[0]+","+tokens[1]).add(tokens[2]);
					}
				}
			}
			finally {
				br1.close();
				br2.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

  /**
  ** Input file is freebase entity to preview mapping file path:
  ** @param filePath
  **/
  public void loadWikipediaPreview(String filePath){
          try {
                  BufferedReader br = new BufferedReader(new FileReader(filePath));
                  try {
                          String line = null;
                          while((line = br.readLine()) != null) {
                                  String[] preview = line.split("\t", 2);
                                  entityPreviewMap.put(Integer.parseInt(preview[0]), preview[1]);
                          }
                  }
                  finally {
                          br.close();
                  }
          }
          catch(Exception e) {
                  e.printStackTrace();
          }
  }


	/**
  ** Input file is candidate subject end type:
  ** @param filePath
  **/
  public void loadCandidateSubjectEndType(String filePath){
          try {
                  BufferedReader br = new BufferedReader(new FileReader(filePath));
                  try {
                          String line = null;
                          while((line = br.readLine()) != null) {
                                  String[] preview = line.split(",", 2);
																	if(candidateSubject.containsKey(Integer.parseInt(preview[0]))){
																		candidateSubject.get(Integer.parseInt(preview[0])).add(Integer.parseInt(preview[1]));
																	}
																	else{
																		candidateSubject.put(Integer.parseInt(preview[0]),new HashSet<Integer>());
																		candidateSubject.get(Integer.parseInt(preview[0])).add(Integer.parseInt(preview[1]));
																	}
                          }

                  }
                  finally {
                          br.close();
                  }
          }
          catch(Exception e) {
                  e.printStackTrace();
          }
  }


	/**
  ** Input file is domain to type list map:
  ** @param filePath
  **/
  public void loadTypeDomainMap(String filePath){
          try {
                  BufferedReader br = new BufferedReader(new FileReader(filePath));
                  try {
                          String line = null;
                          while((line = br.readLine()) != null) {
                                  String[] data = line.split(",");
																	int domainId = Integer.parseInt(data[0]);
																	int typeId = Integer.parseInt(data[1]);
																	typeDomainMap.put(typeId, domainId);
                          }

                  }
                  finally {
                          br.close();
                  }
          }
          catch(Exception e) {
                  e.printStackTrace();
          }

  }


	/**
  ** Input file is domain label map:
  ** @param filePath
  **/
  public void loadDomainLabelMap(String filePath){
          try {
                  BufferedReader br = new BufferedReader(new FileReader(filePath));
                  try {
                          String line = null;
                          while((line = br.readLine()) != null) {
                                  String[] data = line.split("\\t");
																	domainLabelMap.put(Integer.parseInt(data[0]),data[1]);
                          }

                  }
                  finally {
                          br.close();
                  }
          }
          catch(Exception e) {
                  e.printStackTrace();
          }
  }



	/**
  ** Input file is candidate target end type:
  ** @param filePath
  **/
  public void loadCandidateObjectEndType(String filePath){
          try {
                  BufferedReader br = new BufferedReader(new FileReader(filePath));
                  try {
                          String line = null;
                          while((line = br.readLine()) != null) {
                                  String[] preview = line.split(",", 2);
																	if(candidateObject.containsKey(Integer.parseInt(preview[0]))){
																		candidateObject.get(Integer.parseInt(preview[0])).add(Integer.parseInt(preview[1]));
																	}
																	else{
																		candidateObject.put(Integer.parseInt(preview[0]),new HashSet<Integer>());
																		candidateObject.get(Integer.parseInt(preview[0])).add(Integer.parseInt(preview[1]));
																	}
                          }


                  }
                  finally {
                          br.close();
                  }
          }
          catch(Exception e) {
                  e.printStackTrace();
          }
  }

	/**
	** Input file type to entities mapping
	** @param filePath
	**/
	public void loadTypetoEntitiesMapping(String filePath){
					try {
									BufferedReader br = new BufferedReader(new FileReader(filePath));
									try {
													String line = null;
													while((line = br.readLine()) != null) {
																	String[] text = line.split(",", 3);
																	int type = Integer.parseInt(text[0]);
																	int entity = Integer.parseInt(text[1]);
																	if(typetoEntitiesMap.containsKey(type)){
																		typetoEntitiesMap.get(type).add(entity);
																	}
																	else{
																		typetoEntitiesMap.put(type,new LinkedHashSet<Integer>());
																		typetoEntitiesMap.get(type).add(entity);
																	}
													}
									}
									finally {
													br.close();
									}
					}
					catch(Exception e) {
									e.printStackTrace();
					}
	}

	/**
	** Input file is freebase edge to preview mapping file path:
	** @param filePath
	**/
	public void loadEdgePreview(String filePath){
          try {
                  BufferedReader br = new BufferedReader(new FileReader(filePath));
                  try {
                          String line = null;
                          while((line = br.readLine()) != null) {
                                  String[] preview = line.split("@");
																	ArrayList<String> edgeInfo = new ArrayList<String>();
																	edgeInfo.add(preview[0]);
																	edgeInfo.add(preview[1]);
																	edgeInfo.add(preview[2]);
                                  edgePreviewMap.put(Integer.parseInt(edgeInfo.get(1)), edgeInfo);
                          }
                  }
                  finally {
                          br.close();
                  }
          }
          catch(Exception e) {
                  e.printStackTrace();
          }
  }

	private void loadNodeLabel(String inputFilePath, boolean isTabDelimiter, boolean isEntity) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] idLabel = line.split(isTabDelimiter ? "\t" : ",", 2);
					nodeLabelIndex.put(Integer.parseInt(idLabel[0]), idLabel[1].trim());
				}
			}
			finally {
				br.close();
			}
		}
		catch(NullPointerException e) {
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void loadInstancesPerTypeCount(String inputFilePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				// The format of the file is:
				// Count, TypeID
				String line = null;
				while((line = br.readLine()) != null) {
					String[] spl = line.split(",");
					typeInstancesCount.put(Integer.parseInt(spl[1].trim()), Integer.parseInt(spl[0].trim()));
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

	public void loadInstancesPerEdgeCount(String inputFilePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				// The format of the file is:
				// Count, EdgeID
				String line = null;
				while((line = br.readLine()) != null) {
					String[] spl = line.split(",");
					edgeInstancesCount.put(Integer.parseInt(spl[1].trim()), Integer.parseInt(spl[0].trim()));
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void loadEdgeTypeInfo(String inputFilePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
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
						System.out.println("The edge type is not known correctly for: " + line);
					}
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Input file is DBpedia data graph:
	 * tupleID, src, prop, dest
	 * @param filePath
	 */
	public void loadDataGraphIntPropertyDbpedia(String filePath) {
		int numOfLines = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					numOfLines++;
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraphIntProperty(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
				}
			}
			finally {
				br.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		numOfTotalEdges = numOfLines;
	}

	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public void loadDataGraphIntProperty(String filePath, boolean loadDataGraphToMemory)
	{
		int numOfLines = 0;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					numOfLines++;
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					if(loadDataGraphToMemory)
						addEdgeToDataGraphIntProperty(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
					if(edgeType.containsKey(prop)) {
						EdgeTypeInfo eti = edgeType.get(prop);
						// add the types associated with the source.
						if(nodeTypes.containsKey(src)) {
							HashSet<Integer> nt = nodeTypes.get(src);
							nt.add(eti.source_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.source_type);
							nodeTypes.put(src, nt);
						}
						// add the types associated with the object.
						if(nodeTypes.containsKey(dest)) {
							HashSet<Integer> nt = nodeTypes.get(dest);
							nt.add(eti.object_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.object_type);
							nodeTypes.put(dest, nt);
						}
					} else if(intermediateEdgeToNewEdgeIdMap.containsKey(prop)) {
						HashSet<Integer> newPropIds = intermediateEdgeToNewEdgeIdMap.get(prop);
						Iterator<Integer> iter = newPropIds.iterator();
						int node = src;
						boolean isSrc = true;
						if(intermediateNodesList.contains(src) && !intermediateNodesList.contains(dest)) {
							node = dest;
							isSrc = false;
						}
						while(iter.hasNext()) {
							int newProp = iter.next();
							if(edgeType.containsKey(newProp)) {
								EdgeTypeInfo eti = edgeType.get(newProp);
								// add the types associated with the source.
								int type = eti.source_type;
								if(!isSrc)
									type = eti.object_type;
								if(nodeTypes.containsKey(node)) {
									HashSet<Integer> nt = nodeTypes.get(node);
									nt.add(type);
								}
								else {
									HashSet<Integer> nt = new HashSet<Integer>();
									nt.add(type);
									nodeTypes.put(node, nt);
								}
							}
						}
						//System.out.println("This property is not present in edge type info! -> " + prop);
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
		numOfTotalEdges = numOfLines;
	}

	/**
	 * For every line in the data graph file, an entry into data structures that are supposed to hold the data graph in memory are
	 * populated. An entry for src->obj is made, and its corresponding back entry is made to objDataGraph.
	 * @param tupleId
	 * @param src
	 * @param prop
	 * @param obj
	 */
	private void addEdgeToDataGraphIntProperty(int tupleId, int src, int prop, int obj,
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph, HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph)
	{
		ObjNodeIntProperty son = new ObjNodeIntProperty();
		son.tupleId = tupleId;
		son.dest = obj;
		son.prop = prop;
		if(srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNodeIntProperty> tuple = srcDataGraph.get(src);
			tuple.add(son);
		}
		else
		{
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(son);
			srcDataGraph.put(src, tuple);
		}

		// Add this same edge in the opposite direction..
		ObjNodeIntProperty oon = new ObjNodeIntProperty();
		oon.tupleId = tupleId;
		oon.dest = src;
		oon.prop = prop;
		if(objDataGraph.containsKey(obj))
		{
			ArrayList<ObjNodeIntProperty> tuple = objDataGraph.get(obj);
			tuple.add(oon);
		}
		else
		{
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(oon);
			objDataGraph.put(obj, tuple);
		}
	}

	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public void loadDataGraphIntProperty(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraph(tupleId, src, prop, dest);
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * For every line in the data graph file, an entry into data structures that are supposed to hold the data graph in memory are
	 * populated. An entry for src->obj is made, and its corresponding back entry is made to objDataGraph.
	 * @param tupleId
	 * @param src
	 * @param prop
	 * @param obj
	 */
	private void addEdgeToDataGraph(int tupleId, int src, int prop, int obj) {
		ObjNodeIntProperty son = new ObjNodeIntProperty();
		son.tupleId = tupleId;
		son.dest = obj;
		son.prop = prop;
		if(srcDataGraph.containsKey(src)) {
			ArrayList<ObjNodeIntProperty> tuple = srcDataGraph.get(src);
			tuple.add(son);
		}
		else {
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(son);
			srcDataGraph.put(src, tuple);
		}

		// Add this same edge in the opposite direction..
		ObjNodeIntProperty oon = new ObjNodeIntProperty();
		oon.tupleId = tupleId;
		oon.dest = src;
		oon.prop = prop;
		if(objDataGraph.containsKey(obj)) {
			ArrayList<ObjNodeIntProperty> tuple = objDataGraph.get(obj);
			tuple.add(oon);
		}
		else {
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(oon);
			objDataGraph.put(obj, tuple);
		}
	}

	public void populateInvertedNodeType() {
		Iterator<Integer> iter = nodeTypes.keySet().iterator();
		while(iter.hasNext()) {
			HashSet<Integer> types = nodeTypes.get(iter.next());
			// add every node-type in "types" as relevant type to every other node.
			int len = types.size();
			int[] typesarr = new int[len];
			Iterator<Integer> it = types.iterator();
			int i = 0;
			while(it.hasNext()) {
				typesarr[i] = it.next();
				i++;
			}
			for(int j=0; j<len-1; j++) {
				int t1 = typesarr[j];
				HashSet<Integer> t1type;
				if(invertedNodeTypes.containsKey(t1)) {
					t1type = invertedNodeTypes.get(t1);
				}
				else {
					t1type = new HashSet<Integer>();
				}
				for(int k=j+1; k<len; k++) {
					int t2 = typesarr[k];
					t1type.add(t2);

					// add t1 as a type relevant to type t2.
					HashSet<Integer> t2type;
					if(invertedNodeTypes.containsKey(t2)) {
						t2type = invertedNodeTypes.get(t2);
						t2type.add(t1);
					}
					else {
						t2type = new HashSet<Integer>();
						t2type.add(t1);
						invertedNodeTypes.put(t2, t2type);
					}
				}
				if(!invertedNodeTypes.containsKey(t1))
					invertedNodeTypes.put(t1, t1type);
			}
		}
	}

	public void loadConcatedPropertiesList(String inputFilePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] split = line.split("\t");
					int newPropID = Integer.parseInt(split[1]);
					concatenatedStringEdgesToNewEdgeIdMap.put(split[0].trim(), newPropID);
					String[] interEdges = split[0].split(",");

					int interEdge1 = Integer.parseInt(interEdges[0].trim());
					if(intermediateEdgeToNewEdgeIdMap.containsKey(interEdge1)) {
						intermediateEdgeToNewEdgeIdMap.get(interEdge1).add(interEdge1);
					} else {
						HashSet<Integer> ids = new HashSet<Integer>();
						ids.add(interEdge1);
						intermediateEdgeToNewEdgeIdMap.put(interEdge1, ids);
					}

					int interEdge2 = Integer.parseInt(interEdges[1].trim());
					if(intermediateEdgeToNewEdgeIdMap.containsKey(interEdge2)) {
						intermediateEdgeToNewEdgeIdMap.get(interEdge2).add(interEdge2);
					} else {
						HashSet<Integer> ids = new HashSet<Integer>();
						ids.add(interEdge2);
						intermediateEdgeToNewEdgeIdMap.put(interEdge2, ids);
					}
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the list of intermediate nodes present in the dataset.
	 */
	public void loadIntermediateNodesFromFile(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					intermediateNodesList.add(Integer.parseInt(line.trim()));
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/*public void loadDomainToTypeIndex(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] splitline = line.split(":");
					int domain = Integer.parseInt(splitline[0].trim());
					String[] types = splitline[1].split(",");
					ArrayList<Integer> typesList = new ArrayList<Integer>();
					for(String type : types) {
						typesList.add(Integer.parseInt(type.trim()));
					}
					domainsToTypesIndex.put(domain, typesList);
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/

	/*public void loadTypeToEdgeTypeIndex(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					String[] splitline = line.split(":");
					int type = Integer.parseInt(splitline[0].trim());
					String[] edgeTypes = splitline[1].split(",");
					ArrayList<Integer> edgeTypesList = new ArrayList<Integer>();
					for(String edgeType : edgeTypes) {
						edgeTypesList.add(Integer.parseInt(edgeType.trim()));
					}
					typeToEdgeTypeIndex.put(type, edgeTypesList);
				}
			}
			finally {
				br.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}*/
}
