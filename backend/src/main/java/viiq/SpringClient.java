package viiq;

import it.unimi.dsi.lang.MutableString;


import java.io.*;
import java.lang.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.net.URLConnection;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import viiq.clientServer.client.BackendResponseObject;
import viiq.clientServer.client.EdgeExamplesResponseObject;
import viiq.clientServer.server.GUIRequestObject;
import viiq.clientServer.server.LoadData;
import viiq.commons.CandidateEdgeEnds;
import viiq.commons.GuiEdgeInfo;
import viiq.commons.GuiEdgeStringInfo;
import viiq.commons.CandidateEdgeScore;
import viiq.commons.QueryResult;
import viiq.decisionForest.DecisionForestMain;
import viiq.decisionForest.LearnDecisionForest;
import viiq.otherClassifiers.baseLineRanker.BaseLineRanker;
import viiq.graphCompletionGuiMain.GenerateCandidatesNew;
import viiq.graphQuerySuggestionMain.Config;
import viiq.graphQuerySuggestionMain.DestNode;
import viiq.utils.PropertyKeys;
import viiq.utils.BufferedRandomAccessFile;
import viiq.backendHelper.SpringClientHelper;
import viiq.backendHelper.NodeLabelID;

@RestController
public class SpringClient {
	final Logger logger = Logger.getLogger(getClass());
	final static String propertiesFilePath = SpringServer.propertiesFilePath;
	Config conf = null;

	//stores the rejected triples as <"graphSource,edge,graphObject", probability>
	public HashMap<String, HashMap<String, Double>> decayFactor = new HashMap<String, HashMap<String, Double>>();
	//stores the edges that were displayed as suggestion in previous iteration
	public HashMap<String, HashSet<String>> lastRecommendedEdges = new HashMap<String, HashSet<String>>();

  //DL,TL,EL of nodes that has been edited
	public HashMap<String, HashMap<Integer, ArrayList<ArrayList<NodeLabelID>>>> nodeEditValuesMap = new HashMap<String, HashMap<Integer, ArrayList<ArrayList<NodeLabelID>>>>();

	public HashMap<String, ArrayList<GuiEdgeInfo>> curPartialGraph = new HashMap<String, ArrayList<GuiEdgeInfo>>();
	//entity values of all nodes in the current partial graph
	public HashMap<String, LinkedHashMap<Integer, HashSet<Integer>>> evaluatedValues = new HashMap<String, LinkedHashMap<Integer, HashSet<Integer>>>();
	//query result as tuples
	public HashMap<String, ArrayList<ArrayList<Integer>>> answerTuples = new HashMap<String, ArrayList<ArrayList<Integer>>>();



	public ArrayList<String> warnings;
	public ArrayList<String> errors;
	String errorFilePath;

	/**
	 * Returns a new query to work on in the GUI. Used only for USER STUDY.
	 * Return value is a pipe separated value with "queryID" | "query String"
	 * @return
	 */
	@RequestMapping(value = "/getnewquery", method = RequestMethod.GET)
	public String getRandomQuery() {
		String query = LoadData.getNextQuery();
		System.out.println("New query to work on is:\n" + query);
		return query;
	}


	/**
	 * This method searches the entity and returns the page number for given
	 * entityName and entityId
	 * @param entityName
	 * @param entityId
	 * @return
	 */
	@RequestMapping(value = "/getentityposition", method = RequestMethod.GET)
	public String getEntityPosition(
			@RequestParam(value="name", required=false, defaultValue="") String entityName,
			@RequestParam(value="id", required=false, defaultValue="-1") int entityId,
			@RequestParam(value="windowsize", required=false, defaultValue="-1") int windowSize) {
			instantiateConfHandler();
			SpringClientHelper scp = new SpringClientHelper(conf);
			long pos = scp.getEntityPosition(entityName, entityId);
			int pageNo = (int)(pos / windowSize);
			return ""+pageNo;
	}



	/**
	 * This method searches the typr and returns the page number for given
	 * typeName and typeId
	 * @param entityName
	 * @param entityId
	 * @return
	 */
	@RequestMapping(value = "/gettypeposition", method = RequestMethod.GET)
	public String gettypePosition(
			@RequestParam(value="name", required=false, defaultValue="") String typeName,
			@RequestParam(value="id", required=false, defaultValue="-1") int typeId,
			@RequestParam(value="windowsize", required=false, defaultValue="-1") int windowSize) {
			instantiateConfHandler();
			SpringClientHelper scp = new SpringClientHelper(conf);
			long pos = scp.getTypePosition(typeName, typeId);
			int pageNo = (int)(pos / windowSize);
			return ""+pageNo;
	}




	/**
	 * This gets entities and their values, subject to various filters.
	 * @param domain
	 * @param type
	 * @param keyword
	 * @param windowNumber
	 * @param windowSize
	 * @return
	 */
	@RequestMapping(value = "/getentities", method = RequestMethod.GET)
	public ArrayList<String> getEntities(
			@RequestParam(value="domain", required=false, defaultValue="-1") int domain,
			@RequestParam(value="type", required=false, defaultValue="-1") int type,
			@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
			@RequestParam(value="windownum",  required=false, defaultValue="0") int windowNumber,
			@RequestParam(value="windowsize",  required=false, defaultValue="1500") int windowSize) {
		System.out.println("Input parameters for GET request /getentities :");
		System.out.println("domain = " + domain);
		System.out.println("type = " + type);
		System.out.println("keyword = " + keyword);
		System.out.println("windowNumber = " + windowNumber);
		System.out.println("windowSize = " + windowSize);
		instantiateConfHandler();
		ArrayList<String> entities = new ArrayList<String>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);


			scp.getEntities(domain, type, keyword, windowNumber, windowSize, entities);

			entities = scp.reOrderIdenticalEntities(entities);

			for(int i=0;i<entities.size();i++){
				String[] vals = entities.get(i).split(",");
				if(scp.getWikiSummary(Integer.parseInt(vals[0]))==""){
					entities.set(i, entities.get(i)+",nopreview");

				}
				else{
					entities.set(i, entities.get(i)+",preview");
				}
			}
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done " + entities.size());
		return entities;
	}

	/**
	 * This returns DL, TL, EL based on the current DTL.
	 * @param domain
	 * @param type
	 * @param keyword
	 * @param windowNumber
	 * @param windowSize
	 * @param nodeTypeValues
	 * @return
	 */
	@RequestMapping(value = "/getdomainstypesentities", method = RequestMethod.POST)
	public ArrayList<ArrayList<String>> updateDomainsTypesEntities(
			@RequestParam(value="domain", required=false, defaultValue="-1") int domain,
			@RequestParam(value="type", required=false, defaultValue="-1") int type,
			@RequestParam(value="typeKeyword", required=false, defaultValue="") String typeKeyword,
			@RequestParam(value="entityKeyword", required=false, defaultValue="") String entityKeyword,
			@RequestParam(value="typewindownum",  required=false, defaultValue="0") int typeWindowNumber,
			@RequestParam(value="entityewindownum",  required=false, defaultValue="0") int entityWindowNumber,
			@RequestParam(value="windowsize",  required=false, defaultValue="1500") int windowSize,
			@RequestParam(value="nodetypevalues",  required=false, defaultValue="") String nodeTypeValues,
			@RequestParam(value="entityid",  required=false, defaultValue="-1") int entityId,
			@RequestParam(value="nodetobeedited",  required=false, defaultValue="-1") int nodeToBeEdited,
			@RequestParam(value="sessionid",  required=false, defaultValue="") String sessionId,
			@RequestBody GUIRequestObject obj) {
		System.out.println("Input parameters for GET request /getentities :");
		System.out.println("domain = " + domain);
		System.out.println("type = " + type);
		System.out.println("entityKeyword = " + entityKeyword);
		System.out.println("typeWindowNumber = " + typeWindowNumber);
		System.out.println("entityWindowNumber = " + entityWindowNumber);
		System.out.println("windowSize = " + windowSize);
		System.out.println("nodetypevalues = " + nodeTypeValues);
		System.out.println("nodetobeedited = " + nodeToBeEdited);
		instantiateConfHandler();
		ArrayList<ArrayList<String>> returnValues = new ArrayList<ArrayList<String>>();
		//ArrayList<ArrayList<NodeLabelID>> allValues = new ArrayList<ArrayList<NodeLabelID>>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			initializeSession(sessionId);

			//partial graph has been modified, thus need to evaluate the new graph
			if(!scp.equalGraphs(obj.partialGraph, curPartialGraph.get(sessionId))) {
				System.out.println("evaluating a new partial graph!!");
				initializeCurrentGraph(obj.partialGraph, sessionId);
			} else {
				System.out.println("no change in graph!!");
			}

			ArrayList<ArrayList<NodeLabelID>> vals = new ArrayList<ArrayList<NodeLabelID>>();
			if(nodeEditValuesMap.get(sessionId).containsKey(nodeToBeEdited)) {
				System.out.println("this node already explored, using previously calculated result!");
				vals = nodeEditValuesMap.get(sessionId).get(nodeToBeEdited);
			} else {
				System.out.println("exploring this node for the first time!");
				vals = scp.nodeEditValues(nodeTypeValues, entityId, evaluatedValues.get(sessionId), nodeToBeEdited);
				nodeEditValuesMap.get(sessionId).put(nodeToBeEdited, vals);
			}
			returnValues = scp.filterValues(vals, domain, type, typeKeyword, entityKeyword, typeWindowNumber, entityWindowNumber, entityId, nodeTypeValues, windowSize);
			returnValues.set(2, scp.reOrderIdenticalEntities(returnValues.get(2)));
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}

		return returnValues;
	}

	/**
	 * This returns union of entities for a given set of types
	 * @param domain
	 * @param type
	 * @param keyword
	 * @param windowNumber
	 * @param windowSize
	 * @param nodeTypeValues
	 * @return
	 */
	@RequestMapping(value = "/getunionofentitiesfortypes", method = RequestMethod.GET)
	public ArrayList<String> getUnionOfEntitiesForTypes(
			@RequestParam(value="domain", required=false, defaultValue="-1") int domain,
			@RequestParam(value="type", required=false, defaultValue="-1") int type,
			@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
			@RequestParam(value="typewindownum",  required=false, defaultValue="0") int typeWindowNumber,
			@RequestParam(value="entitywindownum",  required=false, defaultValue="0") int entityWindowNumber,
			@RequestParam(value="windowsize",  required=false, defaultValue="1500") int windowSize,
			@RequestParam(value="nodetypevalues",  required=false, defaultValue="") String nodeTypeValues) {
		System.out.println("Input parameters for GET request /getentities :");
		System.out.println("domain = " + domain);
		System.out.println("type = " + type);
		System.out.println("keyword = " + keyword);
		System.out.println("typeWindowNumber = " + typeWindowNumber);
		System.out.println("entityWindowNumber = " + entityWindowNumber);
		System.out.println("windowSize = " + windowSize);
		System.out.println("nodetypevalues = " + nodeTypeValues);
		instantiateConfHandler();
		ArrayList<String> returnValues = new ArrayList<String>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			HashSet<Integer> typesOfThisNode = new HashSet<Integer>();
			String[] types = nodeTypeValues.split(",");
			for(String t : types) {
				typesOfThisNode.add(Integer.parseInt(t));
			}
			returnValues = scp.getInstancesUnion(typesOfThisNode, windowSize, entityWindowNumber);

		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		return returnValues;
	}

	/**
	 * This gets types and their values, subject to variosu filters
	 * @param domain
	 * @param keyword
	 * @param windowNumber
	 * @param windowSize
	 * @return
	 */
	@RequestMapping(value = "/gettypes", method = RequestMethod.GET)
	public ArrayList<String> getTypess(
			@RequestParam(value="domain", required=false, defaultValue="-1") int domain,
			@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
			@RequestParam(value="windownum",  required=false, defaultValue="0") int windowNumber,
			@RequestParam(value="windowsize",  required=false, defaultValue="1500") int windowSize) {
		System.out.println("Input parameters for GET request /gettypes :");
		System.out.println("domain = " + domain);
		System.out.println("keyword = " + keyword);
		System.out.println("windowNumber = " + windowNumber);
		System.out.println("windowSize = " + windowSize);
		instantiateConfHandler();
		ArrayList<String> types = new ArrayList<String>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			scp.getTypes(domain, keyword, windowNumber, windowSize, types);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done " + types.size());
		return types;
	}

	/**
	 * This gets wikipedia summary subject to node id
	 * @param nodeID
	 * @return
	 */
	@RequestMapping(value = "/getwikisummary", method = RequestMethod.GET)
	public String getWikiSummary(
			@RequestParam(value="nodeid", required=false, defaultValue="-1") int nodeid){
		System.out.println("Input parameters for GET request /gettypes :");
		System.out.println("nodeid = " + nodeid);
		//HashMap<Integer, String> freeWikiMap = LoadData.getFreebaseWikiMap();
		SpringClientHelper scp = new SpringClientHelper(conf);
		String wikiSummary = scp.getWikiSummary(nodeid);

                /*
		try {
			if(!freeWikiMap.containsKey(nodeid)){
				return "";
			}
			String wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/"+freeWikiMap.get(nodeid);
      String wikiNUrl = wikiUrl.replace(" ","_");
			URL url = new URL(wikiNUrl);
    	URLConnection request = url.openConnection();
    	request.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) request.getContent()));
      StringBuffer sb = new StringBuffer();
      String str;
      while((str = reader.readLine())!= null){
         sb.append(str);
      }
			wikiSummary = sb.toString();
			reader.close();

		} catch(Exception e) {
			System.out.println("Error in getwikisummary");
			e.printStackTrace();
		}*/

		return wikiSummary;
	}

		/**
	 * This gets result of the query as tuples
	 * @param windowSize
	 * @param windowNum
	   @param keyword
	 * @return
	 */
	@RequestMapping(value = "/getqueryresult", method = RequestMethod.GET)
	public ArrayList<ArrayList<String>> getQueryResult(
			@RequestParam(value="querywindowsize", required=false, defaultValue="-1") int windowSize,
			@RequestParam(value="querywindownum", required=false, defaultValue="1") int windowNum,
			@RequestParam(value="querykeyword", required=false, defaultValue="-1") String keyword,
			@RequestParam(value="sessionid", required=false, defaultValue="") String sessionId){
		System.out.println("Input parameters for GET request /gettypes :");
		System.out.println("windowsize = " + windowSize);
		System.out.println("windownum = " + windowNum);
		System.out.println("keyword = " + keyword);

		initializeSession(sessionId);
		
		LoadData ldlm = new LoadData();
		ArrayList<ArrayList<String>> queryResult = new ArrayList<ArrayList<String>>();

		//adding graphnode ids as column headers
		ArrayList<String> graphNodes = new ArrayList<String>();
		System.out.println("evaluatedValues keys = "+evaluatedValues.get(sessionId).keySet());
		for(int node : evaluatedValues.get(sessionId).keySet()) {
			graphNodes.add(Integer.toString(node));
		}
		queryResult.add(graphNodes);
		//increasing windowSize by 1 for column headers
		//windowSize++;
		
		int ncol = answerTuples.get(sessionId).isEmpty() ? 0 : answerTuples.get(sessionId).get(0).size();
		int tupleCount = 0;
		for(int i = 0; i < answerTuples.get(sessionId).size(); i++) {
			ArrayList<String> tuple = new ArrayList<String>();
			boolean keywordMatch = false;
			boolean noEmptyLabel = true;
			for(int j = 0; j < ncol; j++) {
				int entityId = answerTuples.get(sessionId).get(i).get(j);
				String entityLabel = ldlm.getNodeLabelIndex().getOrDefault(entityId, "");
				//System.out.println(entityId+" --> "+entityLabel);
				if(entityLabel.equals(""))
					noEmptyLabel = false;
				tuple.add(entityLabel);
				if(keyword.equals("-1") || entityLabel.toLowerCase().contains(keyword.toLowerCase())) {
					keywordMatch = true;
				}
			}
			if(keywordMatch && noEmptyLabel) {
				tupleCount++;
				if(tupleCount > (windowNum-1)*windowSize)
					queryResult.add(tuple);
			}
			if(queryResult.size() == windowSize+2) {
				// two additional rows for column headers and one row from next page
				break;
			}
		}
		return queryResult;
	}



	/**
	 * This gets edge preview
	 * @param edgeID
	 * @return
	 */
	@RequestMapping(value = "/getedgepreview", method = RequestMethod.GET)
	public ArrayList<String> getEdgePreview(
			@RequestParam(value="edgeid", required=false, defaultValue="-1") int edgeid){
		System.out.println("Input parameters for GET request /gettypes :");
		System.out.println("edgeid = " + edgeid);
		//HashMap<Integer, String> freeWikiMap = LoadData.getFreebaseWikiMap();
		SpringClientHelper scp = new SpringClientHelper(conf);
		ArrayList<String> edgeInfo = scp.getEdgePreview(edgeid);
		return edgeInfo;
	}


	/**
	 * OLD: DEPRECATED
	 * This get request seeks all types associated with domainID. This is called when a domain is chosen in the node suggestion domain-combo box.
	 * @param domainID
	 * @return
	 */
	@RequestMapping(value = "/gettypesss", method = RequestMethod.GET)
	public ArrayList<String> getTypesss(@RequestParam(value="name", required=false, defaultValue="25") int domainID) {
		instantiateConfHandler();
		String filePath = conf.getInputFilePath(PropertyKeys.domainTypesFolder) + domainID;
		ArrayList<String> types = new ArrayList<String>();
		try {
			String currLine;
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			while ((currLine = br.readLine()) != null){
				currLine = currLine.replace("\t",",");
				types.add(currLine);
			}
			br.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return types;
	}

	/**
	 * This method takes a node ID as input when the user clicks on it to edit its value, and sends back potential new values for the node.
	 * The edges incident on the node to be edited is sent. The format of the edge details is:
	 * // the property is sent in the following format:
	 * 28113,0|28114,1 etc.
	 * The first id is the edge ID, 0 or 1 indicates if this node is the source of object of this edge respectively.
	 * @param nodeID
	 * @param windowNumber
	 * @param windowSize
	 * @param edges
	 * @return
	 */
	// @RequestMapping(value = "/geteditnode", method = RequestMethod.GET)
	// public ArrayList<String> getNodeEditValues(
	// 		@RequestParam(value="node", required=true, defaultValue="25") int nodeID,
	// 		@RequestParam(value="windownum", required=false, defaultValue="0") int windowNumber,
	// 		@RequestParam(value="windowsize", required=false, defaultValue="100") int windowSize,
	// 		@RequestParam(value="edges", required=true, defaultValue="") String edges,
	// 		@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
	// 		@RequestParam(value="domain", required=false, defaultValue="-1") int domain) {
	// 	System.out.println("Input node = " + nodeID);
	// 	System.out.println("keyword = " + keyword);
	// 	System.out.println("edges seen = " + edges);
	// 	System.out.println("window num = " + windowNumber);
	// 	System.out.println("window size = " + windowSize);
	// 	System.out.println("domain = " + domain);
	// 	instantiateConfHandler();
	// 	ArrayList<String> values = new ArrayList<String>();
	// 	try {
	// 		SpringClientHelper scp = new SpringClientHelper(conf);
	// 		boolean isType = LoadData.isTypeNode(nodeID);
	// 		values = scp.nodeEditValues(nodeID, edges, keyword, windowNumber, windowSize, domain);
	// 		if(isType) {
	// 			//System.out.println("double clicked node is Type");
	// 			for(int i=0;i<values.size();i++){
	// 				String[] vals = values.get(i).split(",");
	// 				if(scp.getWikiSummary(Integer.parseInt(vals[0]))==""){
	// 					values.set(i, values.get(i)+",nopreview");
	// 				}
	// 				else{
	// 					values.set(i, values.get(i)+",preview");
	// 				}
	// 			}
	// 		}
	// 	} catch(IllegalArgumentException iae) {
	// 		iae.printStackTrace();
	// 	}
	// 	System.out.println("done " + values.size());
	// 	return values;
	// }


	@RequestMapping(value = "/geteditentity", method = RequestMethod.GET)
	public ArrayList<String> getEntityEditValues(
			@RequestParam(value="typeid", required=true, defaultValue="25") int typeId,
			@RequestParam(value="entityid", required=true, defaultValue="25") int entityId,
			@RequestParam(value="entityname", required=true, defaultValue="") String entityName,
			@RequestParam(value="windownum", required=false, defaultValue="0") int windowNumber,
			@RequestParam(value="windowsize", required=false, defaultValue="100") int windowSize,
			@RequestParam(value="edges", required=true, defaultValue="") String edges,
			@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
			@RequestParam(value="domain", required=false, defaultValue="-1") int domain) {
		System.out.println("Input node = " + typeId);
		System.out.println("keyword = " + keyword);
		System.out.println("edges seen = " + edges);
		System.out.println("window num = " + windowNumber);
		System.out.println("window size = " + windowSize);
		System.out.println("domain = " + domain);
		instantiateConfHandler();
		ArrayList<String> values = new ArrayList<String>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			boolean isType = LoadData.isTypeNode(typeId);
			values = scp.entityEditValues(typeId, entityId, entityName, edges, keyword, windowNumber, windowSize, domain);
			if(isType) {
				//System.out.println("double clicked node is Type");
				for(int i=0;i<values.size();i++){
					String[] vals = values.get(i).split(",");


						if(!vals[0].equals("windowNum") && scp.getWikiSummary(Integer.parseInt(vals[0]))==""){
							values.set(i, values.get(i)+",nopreview");
						}
						else{
							values.set(i, values.get(i)+",preview");
						}


				}
			}
		} catch(IllegalArgumentException iae) {
			System.out.println("1");
			iae.printStackTrace();
		}
		System.out.println("done " + values.size());
		return values;
	}


	@RequestMapping(value = "/getnodetypevalues", method = RequestMethod.GET)
	public ArrayList<String> getNodeTypeValues(
			@RequestParam(value="node", required=true, defaultValue="25") int nodeID,
			@RequestParam(value="edges", required=true, defaultValue="") String edges) {
		System.out.println("Input node = " + nodeID);
		System.out.println("edges seen = " + edges);
		instantiateConfHandler();
		ArrayList<String> values = new ArrayList<String>();
		long start = System.currentTimeMillis();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			values = scp.nodeTypeValues(nodeID, edges);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done " + values.size());
		System.out.println("finding type values for node took " + (System.currentTimeMillis()-start)/1000.0 + " seconds");
		return values;
	}

	@RequestMapping(value = "/getnodetypecandidate", method = RequestMethod.GET)
	public ArrayList<String> getNodeTypeCandidate(
			@RequestParam(value="domain", required=true, defaultValue="-1") int domain,
			@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
			@RequestParam(value="edges", required=true, defaultValue="") String edges) {
		System.out.println("/getnodetypecandidate edges seen = " + edges);
		instantiateConfHandler();
		ArrayList<String> values = new ArrayList<String>();
		if(edges.length()==0){
			return values;
		}
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			values = scp.nodeTypeCandidate(edges, domain, keyword);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done " + values.size());
		return values;
	}

	//returns domains for a set of types
	@RequestMapping(value = "/getdomainsfortypes", method = RequestMethod.GET)
	public ArrayList<String> getDomainsForTypes(
			@RequestParam(value="types", required=true, defaultValue="") String types) {
		System.out.println("/getdomainsfortypes types = " + types);
		instantiateConfHandler();
		ArrayList<String> values = new ArrayList<String>();
		if(types.length()==0){
			return values;
		}
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			values = scp.domainForTypes(types);
		}
		catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done " + values.size());
		return values;
	}

	//returns domains for a set of types
	@RequestMapping(value = "/getendtypes", method = RequestMethod.GET)
	public String getEndTypesForEdge(
			@RequestParam(value="edge", required=true, defaultValue="") int edge) {
				SpringClientHelper scp = new SpringClientHelper(conf);
				return scp.getEndTypesForEdge(edge);
	}


	/**
	 * This GET request seeks all types associated with an entity. This is called when an entity is chosen in the node suggestion entity-combo box.
	 * @param entityID
	 * @return
	 */
	@RequestMapping(value = "/gettypesforentity", method = RequestMethod.GET)
	public ArrayList<String> getTypesForEntity(@RequestParam(value="name", required=false, defaultValue="27") int entityID) {
		System.out.println("Input entity = " + entityID);
		instantiateConfHandler();
		ArrayList<String> types = new ArrayList<String>();
		try {
			//int entity = Integer.parseInt(entityID);
			SpringClientHelper scp = new SpringClientHelper(conf);
			scp.getTypesForEntity(entityID, types);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done " + types.size());
		return types;
	}

	/**
	 * This request gets all possible values that an already existing node can be edited to.
	 * case 1: If node is an entity, the only possible edit is to change it to a type.
	 * case 2: If node is a type, the only possible edit is to change it to an instance of the type.
	 * @param nodeID
	 * @return
	 */
/*	@RequestMapping(value = "/geteditnode", method = RequestMethod.GET)
	public ArrayList<String> getEditNodeValues(@RequestParam(value="name", required=false, defaultValue="World") String nodeID) {
		instantiateConfHandler();
		ArrayList<String> possibleNodeValues = new ArrayList<String>();
		try {
			int node = Integer.parseInt(nodeID);
			SpringClientHelper scp = new SpringClientHelper(conf);
			possibleNodeValues = scp.(node);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		return possibleNodeValues;
	}*/

	/**
	 * This request gets all entity string values that start with "keyword"..
	 * @param keyword
	 * @return
	 */
	/*@RequestMapping(value = "/getentitykeywordcompletion", method = RequestMethod.GET,
			params = {"name"})
	public ArrayList<String> getEntityKeywordCompletion(@RequestParam(value = "name") String keyword) {
		System.out.println("Input keyword from httpservletrequest = " + keyword);
		instantiateConfHandler();
		ArrayList<String> completedKeywords = new ArrayList<String>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			completedKeywords = scp.getEntityKeywordCompletion(keyword);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done! " + completedKeywords.size());
		return completedKeywords;
	}*/

	/**
	 * This request gets all type string values that start with "keyword"..
	 * @param keyword
	 * @return
	 */
	/*@RequestMapping(value = "/gettypekeywordcompletion", method = RequestMethod.GET)
	public ArrayList<String> getTypeKeywordCompletion(@RequestParam(value="name", required=false, defaultValue="World") String keyword) {
		System.out.println("Input keyword = " + keyword);
		instantiateConfHandler();
		ArrayList<String> completedKeywords = new ArrayList<String>();
		try {
			SpringClientHelper scp = new SpringClientHelper(conf);
			completedKeywords = scp.getTypeKeywordCompletion(keyword);
		} catch(IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		System.out.println("done! " + completedKeywords.size());
		return completedKeywords;
	}*/

	/**
	 * This request seeks all the domains that are exposed to the combo box. This is called on page load.
	 * @return
	 */
	@RequestMapping(value = "/greeting", method = RequestMethod.GET)
	public ArrayList<String> get(){



		ArrayList<String> domains = new ArrayList<String>();
		instantiateConfHandler();
		try {
			String currLine;
			BufferedReader br = new BufferedReader(new FileReader(conf.getInputFilePath(PropertyKeys.domainLangEn)));
			while ((currLine = br.readLine()) != null){
				currLine = currLine.replace("\t",",");
				domains.add(currLine);
			}
			br.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		return domains;
	}

	@RequestMapping(value = "/getexamples", method = RequestMethod.GET)
	public EdgeExamplesResponseObject getExamplesGet(
			@RequestParam(value="edge") int edge,
			@RequestParam(value="source") int source,
			@RequestParam(value="object") int object) {
		instantiateConfHandler();
		System.out.println("Fetching examples for edge " + edge);
		SpringClientHelper scp = new SpringClientHelper(conf);
		EdgeExamplesResponseObject eer = scp.getExamples(source, edge, object);
		return eer;
	}

	/*
	* suggestion mode tells us if it is active or passive call.
	* -1 = active, but for naive GUI (which requires sorted display of edges)
	* 0 = active, but for fancy Orion GUI
	* 1 = passive
	* 2 = select an orange node and hit Refresh (this is a mix of passive and active)
	*/

	/*
	* dataGraphInUse, tells us which data graph is used:
	* 0 = Freebase
	* 1 = DBpedia
	* 2 = YAGO
	*/

	@RequestMapping(value = "/greeting", method = RequestMethod.POST)
	// @Consumes("application/json")
	public ResponseEntity<BackendResponseObject> post(@RequestBody GUIRequestObject obj) {
		//System.out.println("number of rejected edges = " + decayFactor.size());
		//System.out.println("number of lastRecommendedEdges = " + lastRecommendedEdges.size());

		long start = System.currentTimeMillis();

		instantiateConfHandler();
		initializeSession(obj.sessionId);

		// Access the rest of the elements in args[]..
		ArrayList<GuiEdgeInfo> pg = obj.partialGraph;

		int suggestionMode = obj.mode;
		String systemName = obj.systemName;

		int dataGraphInUse = obj.dataGraphInUse;
		//	LoadData dataGraph = new LoadData();
		GenerateCandidatesNew gc = new GenerateCandidatesNew(conf);
		HashSet<Integer> history = new HashSet<Integer>();
		CandidateEdgeEnds activeModePartialGraph = new CandidateEdgeEnds();
		System.out.println("************************* Invoked the backend ****************************");
		System.out.println("Suggestion mode = " + suggestionMode);
		System.out.println("Number of iterations = " + obj.noOfIteration);
		/*
		// IMPORTANT NOTE: We must load more files to memory if this is used. Uncomment accordingly in SpringServer.java too!!!
		GenerateCandidates gc = new GenerateCandidates(dataGraph, conf);
		HashMap<Integer, HashMap<Integer, ArrayList<DestNode>>> partialGraph = null;
		HashSet<MutableString> seenTuples = new HashSet<MutableString>();
		partialGraph = gc.getPartialGraph(suggestionMode, pg, history, historyHash, seenTuples, activeModePartialGraph, obj.activeEdgeEnds);
		gc.getNegativeEdgesHistory(rejectedGraph, history, historyHash, seenTuples);*/
		long start1 = System.currentTimeMillis();
		gc.getPartialGraph(suggestionMode, pg, history, activeModePartialGraph, obj.activeEdgeEnds, conf);
		System.out.println("Partial graph generation took "+(System.currentTimeMillis()-start1)/1000.0+" seconds");

		// hashmap which maintains the exact triples associated with each edge.
		// key: edge, value: <source, object> pairs of vertices.
		// At least one of source or object must be part of the partial query graph.
		HashMap<Integer, ArrayList<CandidateEdgeEnds>> edgeToTripletMap = new HashMap<Integer, ArrayList<CandidateEdgeEnds>>();

		//long start = System.currentTimeMillis();
		start1 = System.currentTimeMillis();

		//partial graph has been modified, thus need to evaluate the new graph
		SpringClientHelper scp = new SpringClientHelper(conf);
		if(!scp.equalGraphs(obj.partialGraph, curPartialGraph.get(obj.sessionId))) {
			System.out.println("Initializing graph for candidate generation");
			initializeCurrentGraph(obj.partialGraph, obj.sessionId);
		} else {
			//applying decay factor 
			double df = Double.parseDouble(conf.getProp(PropertyKeys.decayFactorMultiplier));
			for(String triple : lastRecommendedEdges.get(obj.sessionId)) {
				if(decayFactor.get(obj.sessionId).containsKey(triple)) {
					decayFactor.get(obj.sessionId).put(triple, decayFactor.get(obj.sessionId).get(triple)*df);
		 		} else {
		 			decayFactor.get(obj.sessionId).put(triple, df);
		 		}
			}
		}
		System.out.println("Graph prep took "+(System.currentTimeMillis()-start1)/1000.0+" seconds");

		//print full pg
		// for(GuiEdgeInfo e : pg) {
		// 	if(e.graphSource == -1 || e.graphObject == -1) continue;
		// 	System.out.println(e.source+" : "+e.graphSource+" : "+evaluatedValues.get(obj.sessionId).get(e.graphSource).size()+" : "+e.sourceTypeValues+" : "+e.sourceEntity+" : "+e.edge+" : "+e.object+" : "+e.graphObject+" : "+evaluatedValues.get(obj.sessionId).get(e.graphObject).size()+" : "+e.objectTypeValues+" : "+e.objectEntity);
		// }

	    start1 = System.currentTimeMillis();
		HashSet<Integer> candidateEdges = gc.getCandidateEdges(pg, suggestionMode, activeModePartialGraph, edgeToTripletMap, dataGraphInUse, obj.refreshGraphNode, evaluatedValues.get(obj.sessionId), systemName, conf);
		System.out.println("Candidate edge size "+candidateEdges.size()+" and took "+(System.currentTimeMillis()-start1)/1000.0+" seconds to generate");

		ArrayList<Integer> candidateEdgesArray = new ArrayList<Integer>(candidateEdges);
		//System.out.println(candidateEdgesArray.toString());

		start1 = System.currentTimeMillis();
		
		ArrayList<CandidateEdgeScore> rankedCandidateEdges = new ArrayList<CandidateEdgeScore>();
		int topk = obj.topk;

		if(systemName.equals("baseline") || (systemName.equals("hybrid") && (obj.noOfIteration & 1) == 0)) {
			//ranking for baseline method
			BaseLineRanker blr = new BaseLineRanker(conf);
			QueryResult qr = new QueryResult();
			qr.values = evaluatedValues.get(obj.sessionId);
			qr.tuples = answerTuples.get(obj.sessionId);
			rankedCandidateEdges = blr.rankCandidateEdges(candidateEdges, qr);
		} else if(systemName.equals("orion") || (systemName.equals("hybrid") && (obj.noOfIteration & 1) == 1)) {
			double logdfm = Double.parseDouble(conf.getProp(PropertyKeys.userLogSizeThresholdDecayFactorMultiplier));
			// This is for the fancy Orion GUI, which has to be ranked.
			LearnDecisionForest decisionForest = new LearnDecisionForest(conf);
			DecisionForestMain dfm = new DecisionForestMain(decisionForest);
			System.out.println("count = "+decisionForest.getTotalSpecificCnt());
			double logdf = systemName.equals("orion") ? Math.pow(logdfm, (double) obj.noOfIteration) : Math.pow(0.3, 0.5 * (obj.noOfIteration - 1)); //reduce logdf at each iteration for RDP, but at alternate iteration for Hybrid
			System.out.println("tau decay factor = "+ logdf);
			rankedCandidateEdges = dfm.rankCandidateEdges(history, candidateEdges, logdf);
		} else { // this is naive
			if(suggestionMode == 0) {
				// This is for the naive GUI, so all candidates have to be just sorted alphabetically.
				rankedCandidateEdges = gc.rankCandidatesByEdgeLabel(candidateEdges);
			} else {
				topk = 0;
			}		
		}

		System.out.println("Candidate edge ranking took "+(System.currentTimeMillis()-start1)/1000.0+" seconds");

		if(suggestionMode == 0 || suggestionMode == -1) {
			topk = rankedCandidateEdges.size();
		}


		BackendResponseObject returnObject = gc.getCandidateTriples(pg, topk, suggestionMode, rankedCandidateEdges, decayFactor.get(obj.sessionId), lastRecommendedEdges.get(obj.sessionId), edgeToTripletMap, systemName, conf);
		System.out.println("Number of unique suggestions back = " + returnObject.rankedUniqueEdges.size());
		System.out.println("Number of all suggestions back = " + returnObject.rankedEdges.size());
		System.out.println("Done with backend call!!! returning now!");
		ArrayList<GuiEdgeStringInfo> r = returnObject.rankedUniqueEdges;
		for(int i = 0; i < r.size(); i++) {
			System.out.println(r.get(i).graphSource+","+r.get(i).edge+","+r.get(i).graphObject+","+r.get(i).score);
		}
		System.out.println("Candidate triples generation took "+(System.currentTimeMillis()-start1)/1000.0+" seconds");
		System.out.println("TOTAL time spent in candidate generation "+(System.currentTimeMillis()-start)/1000.0+" seconds");

		return new ResponseEntity<BackendResponseObject>(returnObject, HttpStatus.OK);
	}

	@RequestMapping(value = "/clearcanvas", method = RequestMethod.GET)
	public void clearCanvas(
			@RequestParam(value="sessionid", required=false, defaultValue="") String sessionId) {
		decayFactor.remove(sessionId);
		lastRecommendedEdges.remove(sessionId);
		nodeEditValuesMap.remove(sessionId);
		evaluatedValues.remove(sessionId);
		answerTuples.remove(sessionId);
		curPartialGraph.remove(sessionId);
	}

	private void initializeSession(String sessionId) {
		decayFactor.putIfAbsent(sessionId, new HashMap<String, Double>());
		lastRecommendedEdges.putIfAbsent(sessionId, new HashSet<String>());
		nodeEditValuesMap.putIfAbsent(sessionId, new HashMap<Integer, ArrayList<ArrayList<NodeLabelID>>>());
		evaluatedValues.putIfAbsent(sessionId, new LinkedHashMap<Integer, HashSet<Integer>>());
		answerTuples.putIfAbsent(sessionId, new ArrayList<ArrayList<Integer>>());
		curPartialGraph.putIfAbsent(sessionId, new ArrayList<GuiEdgeInfo>());
	}

	private void printGUIInput(int suggestionMode, ArrayList<Integer> history) {
		System.out.println("************************* Invoked the backend ****************************");
		System.out.println(suggestionMode);
		for(int edge : history) {
			System.out.print(edge + " ");
		}
		System.out.println("**************************************************************************");
	}

	private void initializeCurrentGraph(ArrayList<GuiEdgeInfo> graph, String sessionId) {
			//deep copy of new graph to current graph
			curPartialGraph.get(sessionId).clear();
			for(GuiEdgeInfo e : graph) {
				GuiEdgeInfo gei = new GuiEdgeInfo();
				gei.source = e.source;
				gei.graphSource = e.graphSource;
				gei.edge = e.edge;
				gei.object = e.object;
				gei.graphObject = e.graphObject;
				gei.sourceTypeValues = e.sourceTypeValues;
				gei.objectTypeValues = e.objectTypeValues;
				gei.sourceEntity = e.sourceEntity;
				gei.objectEntity = e.objectEntity;
				curPartialGraph.get(sessionId).add(gei);
			}


			SpringClientHelper scp = new SpringClientHelper(conf);
			
			long start = System.currentTimeMillis();
			QueryResult qr = scp.evaluateQueryGraph(curPartialGraph.get(sessionId));
			System.out.println("Query evaluation took "+ (System.currentTimeMillis()-start)/1000.0 + " seconds");

			evaluatedValues.put(sessionId, qr.values);
			answerTuples.put(sessionId, qr.tuples);

			nodeEditValuesMap.get(sessionId).clear();
	}

	private void instantiateConfHandler() {
		if(conf == null) {
			try {
				conf = new Config(propertiesFilePath);
			} catch(ConfigurationException ce) {
				ce.printStackTrace();
			} catch(IOException ce) {
				ce.printStackTrace();
			}
		}
	}
}
