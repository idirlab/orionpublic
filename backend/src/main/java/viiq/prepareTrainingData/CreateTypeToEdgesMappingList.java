package viiq.prepareTrainingData;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;

import viiq.commons.EdgeTypeInfo;
import viiq.commons.ObjNodeIntProperty;
import viiq.graphQuerySuggestionMain.Config;
import viiq.graphQuerySuggestionMain.GraphQuerySuggestionConstants;
import viiq.utils.BufferedRandomAccessFile;
import viiq.utils.PropertyKeys;

public class CreateTypeToEdgesMappingList {
	Config conf = null;
	// key: type, value: arraylist of instances.
	HashMap<Integer, ArrayList<Integer>> typeToInstancesMap = new HashMap<Integer, ArrayList<Integer>>();
	// list of intermediate nodes
	HashSet<Integer> intermediateNodes = new HashSet<Integer>();
	// key: concatenated pair of edges that connect an intermediate node, value: new property id that is created 
	HashMap<String, Integer> combinedEdgeToNewPropMap =  new HashMap<String, Integer>();
	// contains list of edges that connect two entities (not an intermediate edge)
	HashSet<Integer> normalEdgeList = new HashSet<Integer>();
	// rough estimate of number of occurrences of an edge in the data graph:
	// key=property, value=number of occurrences of it (only an estimate for concatenated edges)
	HashMap<Integer, Integer> edgeCardinality = new HashMap<Integer, Integer>();
	
	int supportForAssigningEdgeToType = 5;
	double recallThreshold = 0.2;
	double recallThresholdMin = 0.05;
	double precisionThresholdMin= 0.05;

	// key = edge, value = (source vertex Type, dest vertex Type)
	HashMap<Integer, EdgeTypeInfo> edgeType = new HashMap<Integer, EdgeTypeInfo>();

	public void createEdgesForTypes() {
		readTypesInstancesFile();
		System.out.println("Done reading type instances file");
		readIntermediateNodes();
		System.out.println("done reading intermediate nodes list");
		readPropertiesList();
		System.out.println("done reading properties list");
		readConcatenatedPropList();
		System.out.println("done reading concatenated prop list");
		loadEdgeTypeInfo(conf.getInputFilePath(PropertyKeys.edgeTypeFile));
		System.out.println("done loading edge type info");
		loadEdgeCardinality(conf.getInputFilePath(PropertyKeys.propertiesFileCardinality));
		System.out.println("Done loading edge cardinality info");
		try {
			String filePath = conf.getInputFilePath(PropertyKeys.instancesSortedToTypesIndexPaddedFile);
			int bufferSize = Integer.parseInt(conf.getProp(PropertyKeys.instancesSortedToTypesIndexAlignmentLength)) + 1;
			int numOfLines = Integer.parseInt(conf.getProp(PropertyKeys.instancesSortedToTypesIndexNumberOfLines));
			BufferedRandomAccessFile baf = new BufferedRandomAccessFile(filePath, "r", numOfLines, bufferSize);

			int numOfTotalEdges = Integer.parseInt(conf.getProp(PropertyKeys.datagraphNumberOfLines));
			BufferedRandomAccessFile sourceDataGraphFileHandler = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile), "r", numOfTotalEdges, conf);
			BufferedRandomAccessFile objectDataGraphFileHandler = new BufferedRandomAccessFile(conf.getInputFilePath(PropertyKeys.datagraphObjectAlignedFile), "r", numOfTotalEdges, conf);

			FileWriter fwsrc = new FileWriter(conf.getInputFilePath(PropertyKeys.typeEdgesListSource));
			BufferedWriter bwsrc = new BufferedWriter(fwsrc);
			FileWriter fwobj = new FileWriter(conf.getInputFilePath(PropertyKeys.typeEdgesListObject));
			BufferedWriter bwobj = new BufferedWriter(fwobj);

			Iterator<Integer> iter = typeToInstancesMap.keySet().iterator();
			while(iter.hasNext()) {
				int type = iter.next();
				// film
				/*if(type != 47412424)
					continue;*/
				// film actor
				/*if(type != 47412434)
					continue;*/
				// organization
				/*if(type != 5195879)
					continue;*/
				HashSet<Integer> seenTupleIDs = new HashSet<Integer>();
			//	HashSet<Integer> seenEdges = new HashSet<Integer>();
				ArrayList<Integer> instances = typeToInstancesMap.get(type);
				HashMap<Integer, Integer> sourcePropsList = new HashMap<Integer, Integer>();
				HashMap<Integer, Integer> objectPropsList = new HashMap<Integer, Integer>();
				for(int entity : instances) {
					if(intermediateNodes.contains(entity))
						continue;
					HashSet<Integer> entityTypes = baf.getEntityTypes(entity);
					ArrayList<ObjNodeIntProperty> neighborsSrc = sourceDataGraphFileHandler.getVertexNeighbors(entity);
					if(neighborsSrc != null && !neighborsSrc.isEmpty()) {
						for(ObjNodeIntProperty node : neighborsSrc) {
							if(seenTupleIDs.contains(node.tupleId))
								continue;
					/////		if(seenEdges.contains(node.prop))
						/////		continue;
							seenTupleIDs.add(node.tupleId);
							if(normalEdgeList.contains(node.prop)) {
								HashSet<Integer> destEntityTypes = baf.getEntityTypes(node.dest);
								if(!edgeType.containsKey(node.prop) || !entityTypes.contains(edgeType.get(node.prop).source_type) || !destEntityTypes.contains(edgeType.get(node.prop).object_type))
									continue;
							///////	seenEdges.add(node.prop);
								addToMap(sourcePropsList, node.prop);
							//	System.out.println("adding to SOURCE: " + node.tupleId + " : " + entity + ", " + node.prop + ", " + node.dest);
							} else if(intermediateNodes.contains(node.dest)) {
								ArrayList<ObjNodeIntProperty> intermediateNeighborsSrc = sourceDataGraphFileHandler.getVertexNeighbors(node.dest);
								addIntermediateEdges(entity, entityTypes, intermediateNeighborsSrc, node, sourcePropsList, objectPropsList, 
										seenTupleIDs, baf);
								ArrayList<ObjNodeIntProperty> intermediateNeighborsObj = objectDataGraphFileHandler.getVertexNeighbors(node.dest);
								addIntermediateEdges(entity, entityTypes, intermediateNeighborsObj, node, sourcePropsList, objectPropsList, 
										seenTupleIDs, baf);
							}
						}
					}
				}

				for(int entity : instances) {
					if(intermediateNodes.contains(entity))
						continue;
					ArrayList<ObjNodeIntProperty> neighborsObj = objectDataGraphFileHandler.getVertexNeighbors(entity);
					HashSet<Integer> entityTypes = baf.getEntityTypes(entity);
					if(neighborsObj != null && !neighborsObj.isEmpty()) {
						for(ObjNodeIntProperty node : neighborsObj) {
							if(seenTupleIDs.contains(node.tupleId))
								continue;
					//////		if(seenEdges.contains(node.prop))
					/////			continue;
							seenTupleIDs.add(node.tupleId);
							if(normalEdgeList.contains(node.prop)) {
								HashSet<Integer> destEntityTypes = baf.getEntityTypes(node.dest);
								if(!edgeType.containsKey(node.prop) || !entityTypes.contains(edgeType.get(node.prop).object_type) || !destEntityTypes.contains(edgeType.get(node.prop).source_type))
									continue;
								
						/////		seenEdges.add(node.prop);
								addToMap(objectPropsList, node.prop);
						//		System.out.println("adding to OBJECT: " + node.tupleId + " : "  + node.dest + ", " + node.prop + ", " + entity);
							} else if(intermediateNodes.contains(node.dest)) {
								ArrayList<ObjNodeIntProperty> intermediateNeighborsSrc = sourceDataGraphFileHandler.getVertexNeighbors(node.dest);
								addIntermediateEdges(entity, entityTypes, intermediateNeighborsSrc, node, sourcePropsList, objectPropsList, 
										seenTupleIDs, baf);
								ArrayList<ObjNodeIntProperty> intermediateNeighborsObj = objectDataGraphFileHandler.getVertexNeighbors(node.dest);
								addIntermediateEdges(entity, entityTypes, intermediateNeighborsObj, node, sourcePropsList, objectPropsList, 
										seenTupleIDs, baf);
							}
						}
					}
				}
				// now write the edges of this type to output file	
			//	if(type == 47412424){// || type != 47412424){
					
				writeToFile(type, sourcePropsList, instances.size(), bwsrc);
				writeToFile(type, objectPropsList, instances.size(), bwobj);
			//	}
				//System.out.println(type);
			}
			bwsrc.close();
			bwobj.close();
			baf.close();
			sourceDataGraphFileHandler.close();
			objectDataGraphFileHandler.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void addToMap(HashMap<Integer, Integer> map, int prop) {
		int cnt = 1;
		if(map.containsKey(prop)) {
			cnt = map.get(prop);
			cnt++;
		}
		map.put(prop, cnt);
	}

	private void writeToFile(int type, HashMap<Integer, Integer> props, int typeOccurrenceCnt, BufferedWriter bw) throws IOException {
		if(!props.isEmpty()) {
			MutableString ms = new MutableString();
			ms.append(type).append(":");
			Iterator<Integer> iter = props.keySet().iterator();
			boolean hasEdge = false;
			int numberOfEdgesIgnored = 0;
		//	System.out.println("Number of instances of this type is: " + typeOccurrenceCnt);
			while(iter.hasNext()) {
				int prop = iter.next();
				// This is the threshold I am assuming for the frequency. This is not scientifically chosen. Just saw edges with support up to
				// 4 for film type, and chose this number!
				double t = 1.0;
				double recall = edgeCardinality.containsKey(prop) ? ((t*props.get(prop))/(t*edgeCardinality.get(prop))) : 0.0;
				double precision = (t*props.get(prop))/(t*typeOccurrenceCnt);
				if(props.get(prop) > supportForAssigningEdgeToType && (recall >= recallThreshold || (recall >= recallThresholdMin && precision >= precisionThresholdMin))) {
					ms.append(prop).append(",");
					hasEdge = true;
			//		System.out.println("************************************ " + prop + ": " + props.get(prop) + "\t" + recall + "\t" + edgeCardinality.get(prop) + "\t" + precision);
				} else {
			//		System.out.println(prop + ": " + props.get(prop) + "\t" + recall + "\t" + edgeCardinality.get(prop) + "\t" + precision);
					numberOfEdgesIgnored++;
				}
				/*if(props.get(prop) > supportForAssigningEdgeToType) {
					ms.append(prop).append(",");
					hasEdge = true;
				} else {
					numberOfEdgesIgnored++;
				}*/
			}
			if(hasEdge) {
				bw.write(ms.toString()+"\n");
				System.out.println(type + ": Number of edges ignored = " + numberOfEdgesIgnored);
			} else {
				System.out.println("******** " + type + ": All edges for this type were ignored!!! = " + numberOfEdgesIgnored);
			}
			
		} else {
			System.out.println("No edge for type " + type);
		}
	}

	private void addIntermediateEdges(int entity, HashSet<Integer> entityTypes, ArrayList<ObjNodeIntProperty> intermediateNeighbors, 
			ObjNodeIntProperty node, HashMap<Integer, Integer> sourcePropsList, HashMap<Integer, Integer> objectPropsList, 
			HashSet<Integer> seenTupleIDs, BufferedRandomAccessFile baf) throws IOException {
		if(intermediateNeighbors != null && !intermediateNeighbors.isEmpty()) {
			for(ObjNodeIntProperty intnode : intermediateNeighbors) {
				if(node.prop == intnode.prop)
					continue;
				if(seenTupleIDs.contains(intnode.tupleId))
					continue;
				
				seenTupleIDs.add(intnode.tupleId);
				
				MutableString str = new MutableString();
				str.append(node.prop<intnode.prop ? node.prop : intnode.prop).append(",");
				str.append(node.prop<intnode.prop ? intnode.prop : node.prop);
				String fulstr = str.toString().trim();
				if(combinedEdgeToNewPropMap.containsKey(fulstr) && normalEdgeList.contains(combinedEdgeToNewPropMap.get(fulstr))) {
					int concatprop = combinedEdgeToNewPropMap.get(fulstr);
		//////			if(seenEdges.contains(concatprop))
				//////		continue;
					HashSet<Integer> intnodeEntityTypes = baf.getEntityTypes(intnode.dest);
			//		System.out.println("concatenated edge here = " + fulstr + " ==> " + concatprop);
					if(node.prop < intnode.prop) {
						if(!edgeType.containsKey(concatprop) || !intnodeEntityTypes.contains(edgeType.get(concatprop).object_type) || !entityTypes.contains(edgeType.get(concatprop).source_type))
							continue;
						addToMap(sourcePropsList, concatprop);
				//		System.out.println("inter " + node.tupleId + ": " + entity + ", " + node.prop + ", " + node.dest + " --> " + intnode.tupleId + ": " + node.dest + " " + intnode.prop + ", " + intnode.dest + " concat prop = " + concatprop);
					} else {
						if(!edgeType.containsKey(concatprop) || !intnodeEntityTypes.contains(edgeType.get(concatprop).source_type) || !entityTypes.contains(edgeType.get(concatprop).object_type))
							continue;
						addToMap(objectPropsList, concatprop);
					//	System.out.println("inter " + intnode.tupleId + ": " + node.dest + " " + intnode.prop + ", " + intnode.dest + " --> " + node.tupleId + ": " + entity + ", " + node.prop + ", " + node.dest + " concat prop = " + concatprop);
					}
			//////		seenEdges.add(concatprop);
				}
			}
		}
	}
	
	private void loadEdgeCardinality(String filePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split(":");
				edgeCardinality.put(Integer.parseInt(spl[0].trim()), Integer.parseInt(spl[1].trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void readConcatenatedPropList() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.concatenatedPropertiesMappingFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] splitstr = line.split("\t");
				combinedEdgeToNewPropMap.put(splitstr[0].trim(), Integer.parseInt(splitstr[1].trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void readIntermediateNodes() {
		readSingleColumnFile(conf.getInputFilePath(PropertyKeys.intermediateNodesFile), intermediateNodes);
	}
	private void readPropertiesList() {
		readSingleColumnFile(conf.getInputFilePath(PropertyKeys.propertiesFile), normalEdgeList);
	}
	private void readSingleColumnFile(String filepath, HashSet<Integer> list) {
		try {
			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				list.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void readTypesInstancesFile() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.typesIdSortedInstances));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] typeInst = line.split(",");
				int type = Integer.parseInt(typeInst[0].trim());
				int entity = Integer.parseInt(typeInst[1].trim());
				if(typeToInstancesMap.containsKey(type)) {
					typeToInstancesMap.get(type).add(entity);
				} else {
					ArrayList<Integer> entities = new ArrayList<Integer>();
					entities.add(entity);
					typeToInstancesMap.put(type, entities);
				}
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void loadEdgeTypeInfo(String inputFilePath) {
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

	public static void main(String[] args) {
		CreateTypeToEdgesMappingList ctem = new CreateTypeToEdgesMappingList();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				ctem.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		ctem.createEdgesForTypes();
		System.out.println("Done!");
	}
}
