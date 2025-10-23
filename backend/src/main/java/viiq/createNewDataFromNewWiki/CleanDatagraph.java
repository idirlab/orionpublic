package viiq.createNewDataFromNewWiki;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


class Ends {
	int stype;
	int otype;
}

public class CleanDatagraph {
	public static void main(String[] args) {
		CleanDatagraph cd = new CleanDatagraph();
		//cd.cleanFreebase();
		cd.cleanReverse();
	}
	
	private void writeNewFiles(HashSet<Integer> uniqProps, HashMap<Integer, Ends> relation,
			HashMap<Integer, String> propMap) throws IOException {
		BufferedWriter rbw = new BufferedWriter(new FileWriter("/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/freebase_edgetype_relation_id.out"));
		BufferedWriter pbw = new BufferedWriter(new FileWriter("/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/freebase_propertiesMappingFile.out"));
		Iterator<Integer> iter = uniqProps.iterator();
		while(iter.hasNext()) {
			int prop = iter.next();
			if(relation.containsKey(prop))
				rbw.write(relation.get(prop).stype+","+prop+","+relation.get(prop).otype+"\n");
			if(propMap.containsKey(prop))
				pbw.write(prop+"\t"+propMap.get(prop)+"\n");
		}
		rbw.close();
		pbw.close();
	}
	private HashSet<Integer> getReverseEdges() {
		HashSet<Integer> rev = new HashSet<Integer>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/reverse_property";
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split(",");
				rev.add(Integer.parseInt(spl[1].trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return rev;
	}
	private HashMap<Integer, Ends>  getEdgeRelation() {
		HashMap<Integer, Ends> relation = new HashMap<Integer, Ends>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/freebase_edgetype_relation_id";
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split(",");
				Ends e = new Ends();
				e.stype = Integer.parseInt(spl[0].trim());
				int prop = Integer.parseInt(spl[1].trim());
				e.otype = Integer.parseInt(spl[2].trim());
				relation.put(prop, e);
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return relation;
	}
	private HashMap<Integer, String>  getPropMap() {
		HashMap<Integer, String> propmap = new HashMap<Integer, String>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/freebase_propertiesMappingFile";
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split("\t");
				propmap.put(Integer.parseInt(spl[0].trim()), spl[1].trim());
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return propmap;
	}
	private HashSet<Integer> getEntities() {
		HashSet<Integer> entities = new HashSet<Integer>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/entity_label";
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				entities.add(Integer.parseInt(line.split(",")[0].trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return entities;
	}
	private HashSet<Integer> getIntermediate() {
		HashSet<Integer> intermediate = new HashSet<Integer>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/intermediateNodes-new";
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				intermediate.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return intermediate;
	}
	private void cleanReverse() {
		HashSet<Integer> reverse = getReverseEdges();
		System.out.println("done loading reverse edges");
		HashMap<Integer, Ends> relation = getEdgeRelation();
		System.out.println("done loading relation info");
		HashMap<Integer, String> propMap = getPropMap();
		System.out.println("done loading property map");
		HashSet<Integer> entities = getEntities();
		System.out.println("done loading entities id");
		HashSet<Integer> intermediate = getIntermediate();
		System.out.println("done loading intermediate");
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/freebase_datagraph_withoutTypesDomains";
		String outputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/freebase_datagraph_withoutTypesDomains.out";
		HashSet<Integer> uniqProps = new HashSet<Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] broken = line.split(",");
				if(broken.length == 4) {
					if(!reverse.contains(Integer.parseInt(broken[2].trim())) && (entities.contains(Integer.parseInt(broken[1].trim())) || intermediate.contains(Integer.parseInt(broken[1].trim()))) && (entities.contains(Integer.parseInt(broken[3].trim())) || intermediate.contains(Integer.parseInt(broken[3].trim())))) {
						bw.write(line+"\n");
						uniqProps.add(Integer.parseInt(broken[2].trim()));
					}
				}
			}
			br.close();
			bw.close();
			System.out.println("done writing data graph output file");
			writeNewFiles(uniqProps, relation, propMap);
			System.out.println("DONE writing other two files too!");
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private HashSet<String> getNumericProps() {
		HashSet<String> nums = new HashSet<String>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/all_numeric_properties";
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				nums.add(line.trim());
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return nums;
	}
	
	private void cleanFreebase() {
		HashSet<String> numericProps = getNumericProps();
		HashSet<String> tuples = new HashSet<String>();
		String inputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/data_graph";
		String outputFile = "/mounts/[server_name]/proj/nj/viiq/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/input/freebase_latest/data_graph.out";
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] broken = line.split(",");
				if(broken.length == 4) {
					if(!numericProps.contains(broken[2].trim())) {
						MutableString ms = new MutableString();
						ms.append(broken[1].trim()).append(",").append(broken[2].trim()).append(",").append(broken[3]);
						if(!tuples.contains(ms.toString())) {
							tuples.add(ms.toString());
							bw.write(line+"\n");
						}
					}
				}
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
