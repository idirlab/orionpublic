package viiq.prepareTrainingData;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.log4j.Logger;

public class CreateFreebaseEdgeTypeToType {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	// key: type ID, value: domain ID
	HashMap<Integer, Integer> typeDomainMap = new HashMap<Integer, Integer>();
	// key: edge ID, value: domain ID
	HashMap<Integer, Integer> edgeDomainMap = new HashMap<Integer, Integer>();
	// key: edge ID, value: the edge types of this edge
	HashMap<Integer, RelationEnds> edgeEdgeTypesMap = new HashMap<Integer, RelationEnds>();
	// key: entity ID, value: Types
	HashMap<Integer, ArrayList<Integer>> entityTypeMap = new HashMap<Integer, ArrayList<Integer>>();
	// key: edge ID, value: set of all sources and object occurrences.
	HashMap<Integer, EdgeEndsSets> dataGraph = new HashMap<Integer, EdgeEndsSets>();
	
	// key: edgeType, value: list of Types
	HashMap<Integer, HashSet<Integer>> edgeTypeToType = new HashMap<Integer, HashSet<Integer>>();
	// key: Type, value: list of edgeTypes
	HashMap<Integer, HashSet<Integer>> typeToEdgeType = new HashMap<Integer, HashSet<Integer>>();
	
	public static void main(String[] args) {
		CreateFreebaseEdgeTypeToType cf = new CreateFreebaseEdgeTypeToType();
		System.out.println("reading domain id file");
		cf.readDomainIdFile();
		System.out.println("reading domain map file");
		cf.readTypeDomainMapFile();
		System.out.println("reading edge type related file");
		cf.readEdgeTypeRelationFile();
		System.out.println("reading entity type file");
		cf.readEntityTypeFile();
		System.out.println("reading data graph");
		cf.readDataGraphFile();
		System.out.println("started the main stuff");
		cf.getTypeEdgeType();
		System.out.println("writing output files");
		cf.writeOutputFiles();
	}
	
	private void getTypeEdgeType() {
		Iterator<Integer> iter = dataGraph.keySet().iterator();
		while(iter.hasNext()) {
			int prop = iter.next();
			if(edgeEdgeTypesMap.containsKey(prop)) {
				int srcEdgeType = edgeEdgeTypesMap.get(prop).sourceEdgeType;
				HashSet<Integer> srcEntities = dataGraph.get(prop).sources;
				HashSet<Integer> topSrcTypes = getTopTypesForEdgeType(prop, srcEntities);
				addToEdgeTypeToType(srcEdgeType, topSrcTypes);
				
				int objEdgeType = edgeEdgeTypesMap.get(prop).objectEdgeType;
				HashSet<Integer> objEntities = dataGraph.get(prop).objects;
				HashSet<Integer> topObjTypes = getTopTypesForEdgeType(prop, objEntities);
				addToEdgeTypeToType(objEdgeType, topObjTypes);
			}
		}
	}
	
	private void writeOutputFiles() {
		try {
			FileWriter fw = new FileWriter("/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_type_edgeType");
			BufferedWriter bw = new BufferedWriter(fw);
			FileWriter fw1 = new FileWriter("/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_edgeType_type");
			BufferedWriter bw1 = new BufferedWriter(fw1);
			writeToFile(bw, typeToEdgeType);
			writeToFile(bw1, edgeTypeToType);
			bw.close();
			bw1.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void getTreeMap(HashMap<Integer, Integer> typeHashMap, Map<Integer, ArrayList<Integer>> typeTreeMap) {
		Iterator<Integer> iter = typeHashMap.keySet().iterator();
		while(iter.hasNext()) {
			int hashkey = iter.next();
			int hashval = typeHashMap.get(hashkey);
			if(typeTreeMap.containsKey(hashval)) {
				typeTreeMap.get(hashval).add(hashkey);
			}
			else {
				ArrayList<Integer> arr = new ArrayList<Integer>();
				arr.add(hashkey);
				typeTreeMap.put(hashval, arr);
			}
		}
	}
	
	private HashSet<Integer> getTopTypes(Map<Integer, ArrayList<Integer>> typeCountTM,
			Map<Integer, ArrayList<Integer>> typeCountOnDomainTM) {
		HashSet<Integer> topTypes = new HashSet<Integer>();
		/*if(typeCountTM.size() > 0) {
			ArrayList<Integer> typeCountTop = typeCountTM.entrySet().iterator().next().getValue();
			for(int type : typeCountTop) {
				topTypes.add(type);
			}
		}*/
		if(typeCountOnDomainTM.size() > 0) {
			ArrayList<Integer> typeCountOnDomainTop = typeCountOnDomainTM.entrySet().iterator().next().getValue();
			for(int type : typeCountOnDomainTop) {
				topTypes.add(type);
			}
		}
		return topTypes;
	}
	
	private void addToHashSet(HashSet<Integer> fromHash, HashSet<Integer> toHash) {
		Iterator<Integer> iter = fromHash.iterator();
		while(iter.hasNext()) {
			toHash.add(iter.next());
		}
	}
	
	private void addToEdgeTypeToType(int edgeType, HashSet<Integer> topTypes) {
		if(edgeTypeToType.containsKey(edgeType)) {
			addToHashSet(topTypes, edgeTypeToType.get(edgeType));
		}
		else {
			HashSet<Integer> types = new HashSet<Integer>();
			addToHashSet(topTypes, types);
			edgeTypeToType.put(edgeType, types);
		}
		
		Iterator<Integer> iter = topTypes.iterator();
		while(iter.hasNext()) {
			int type = iter.next();
			if(typeToEdgeType.containsKey(type)) {
				typeToEdgeType.get(type).add(edgeType);
			}
			else {
				HashSet<Integer> et = new HashSet<Integer>();
				et.add(edgeType);
				typeToEdgeType.put(type, et);
			}
		}
	}
	
	private void writeToFile(BufferedWriter bw, HashMap<Integer, HashSet<Integer>> typeMap) throws IOException {
		Iterator<Integer> iter = typeMap.keySet().iterator();
		while(iter.hasNext()) {
			int key = iter.next();
			MutableString ms = new MutableString();
			ms = ms.append(key).append(":");
			Iterator<Integer> it = typeMap.get(key).iterator();
			while(it.hasNext()) {
				ms = ms.append(it.next()).append(",");
			}
			ms = ms.append("\n");
			bw.write(ms.toString());
		}
	}

	private HashSet<Integer> getTopTypesForEdgeType(int prop, HashSet<Integer> entities) {
		// key: type, value: number of instances it is a type of.
		HashMap<Integer, Integer> typeCount = new HashMap<Integer, Integer>();
		// key: type, value: number of instances it is a type of.
		HashMap<Integer, Integer> typeCountOnDomain = new HashMap<Integer, Integer>();
		Iterator<Integer> it = entities.iterator();
		while(it.hasNext()) {
			int entity = it.next();
			if(entityTypeMap.containsKey(entity)) {
				ArrayList<Integer> types = entityTypeMap.get(entity);
				//Iterator<Integer> it1 = types.iterator();
				for(int type : types) {
					//int type = it1.next();
					if(typeCount.containsKey(type)) {
						int cnt = typeCount.get(type);
						cnt++;
						typeCount.put(type, cnt);
					}
					else {
						typeCount.put(type, 1);
					}
					
					if(isSameDomain(prop, type)) {
						if(typeCountOnDomain.containsKey(type)) {
							int cnt = typeCountOnDomain.get(type);
							cnt++;
							typeCountOnDomain.put(type, cnt);
						}
						else {
							typeCountOnDomain.put(type, 1);
						}
					}
				}
			}
		}
		// key: count, value: types
		Map<Integer, ArrayList<Integer>> typeCountTM = new TreeMap<Integer, ArrayList<Integer>>(Collections.reverseOrder());
		Map<Integer, ArrayList<Integer>> typeCountOnDomainTM = new TreeMap<Integer, ArrayList<Integer>>(Collections.reverseOrder());
		getTreeMap(typeCount, typeCountTM);
		getTreeMap(typeCountOnDomain, typeCountOnDomainTM);
		//System.out.println(typeCountTM.size() + " : " + typeCountOnDomainTM.size());
		HashSet<Integer> topTypes = getTopTypes(typeCountTM, typeCountOnDomainTM);
		return topTypes;
	}
		
	private boolean isSameDomain(int prop, int type) {
		boolean sameDomain = false;
		if(edgeDomainMap.containsKey(prop) && typeDomainMap.containsKey(type)) {
			if(edgeDomainMap.get(prop).intValue() == typeDomainMap.get(type).intValue())
				sameDomain = true;
		}
		return sameDomain;
	}
	
	private void readEntityTypeFile() {
		String inputFile = "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_type_object_type";
		try{
			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] split = line.split("\t");
				int ent = Integer.parseInt(split[0].trim());
				int type = Integer.parseInt(split[1].trim());
				if(entityTypeMap.containsKey(ent)) {
					entityTypeMap.get(ent).add(type);
				}
				else {
					ArrayList<Integer> types = new ArrayList<Integer>();
					types.add(type);
					entityTypeMap.put(ent, types);
				}
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
	private void readTypeDomainMapFile() {
		String inputFile = "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_type_domain";
		try{
			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] split = line.split(":");
				typeDomainMap.put(Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim()));
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
	private void readDomainIdFile() {
		String inputFile = "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_domain_id";
		// key: domainString, value: domainID
		HashMap<String, Integer> domainIdMap = new HashMap<String, Integer>();
		try{
			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] split = line.split("\t");
				domainIdMap.put(split[1].trim(), Integer.parseInt(split[0].trim()));
			}
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		populateEdgeDomain(domainIdMap);
		domainIdMap.clear();
		domainIdMap = null;
	}
	
	private void populateEdgeDomain(HashMap<String, Integer> domainIdMap) {
		// MUST RUN readDomainIdFile() BEFORE THIS!!!!!!!!!!!!!!!!!
		String inputFile = "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_properties_domain_lang_en";
		try{
			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] split = line.split("\t");
				if(domainIdMap.containsKey(split[1].trim())) {
					edgeDomainMap.put(Integer.parseInt(split[0].trim()), domainIdMap.get(split[1].trim()));
				}
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
	private void readEdgeTypeRelationFile() {
		String inputFile = "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_edgetype_relation_id";
		try{
			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] split = line.split(",");
				RelationEnds re = new RelationEnds();
				re.sourceEdgeType = Integer.parseInt(split[0].trim());
				re.objectEdgeType = Integer.parseInt(split[2].trim());
				edgeEdgeTypesMap.put(Integer.parseInt(split[1].trim()), re);
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
	private void readDataGraphFile() {
		String inputFile = "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_datagraph_src_obj_cnt";
		try{
			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] split = line.split(",");
				int src = Integer.parseInt(split[1]);
				int prop = Integer.parseInt(split[2]);
				int obj = Integer.parseInt(split[3]);
				if(dataGraph.containsKey(prop)) {
					dataGraph.get(prop).sources.add(src);
					dataGraph.get(prop).objects.add(obj);
				}
				else {
					EdgeEndsSets ees = new EdgeEndsSets();
					ees.sources.add(src);
					ees.objects.add(obj);
					dataGraph.put(prop, ees);
				}
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
}

final class RelationEnds {
	int sourceEdgeType;
	int objectEdgeType;
}

final class EdgeEndsSets {
	HashSet<Integer> sources = new HashSet<Integer>();
	HashSet<Integer> objects = new HashSet<Integer>();
}
