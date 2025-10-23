package viiq.prepareTrainingData;

import viiq.graphQuerySuggestionMain.Config;

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
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

import viiq.commons.DataGraphGenerator;
import viiq.commons.ObjNodeIntProperty;

public class DatagraphMergeProperties {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());

	HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	HashSet<Integer> intermediateNodes = new HashSet<Integer>();
	int numIntermediateNodes = 0;
	int numEntityNodes = 0;
	// key = concatenation of two edges connecting an intermediate node, value = new edge ID corresponding to the key
	HashMap<String, Integer> concatenatedEdgesToNewEdgeIdMap = new HashMap<String, Integer>();
	boolean isFreebaseDataset = false;
	// The largest ID we have for the new Freebase dataset is: 64666131
	// int newCombinedPropID = 64666135; 
	int newCombinedPropID = 30000;
	int arrayIndex = 0;

	public DatagraphMergeProperties() {

	}
	public DatagraphMergeProperties(Config conf) {
		this.conf = conf;
	}

	public void mergeCorrelatedProperties() {
		// first load data graph
		DataGraphGenerator dgg = new DataGraphGenerator();
		dgg.loadDataGraphIntProperty(conf.getInputFilePath(PropertyKeys.datagraphFile), srcDataGraph, objDataGraph);
		System.out.println("Done loading data graph");
		// now process the data graph to print out all distinct edges incident on a node.
		int datasetFlag = Integer.parseInt(conf.getProp(PropertyKeys.datasetFlag));
		if(datasetFlag == 0)
			isFreebaseDataset = true;
		if(isFreebaseDataset) {
			// if it's freebase, need to handle the intermediate nodes!
			// read all intermediate nodes list
			loadIntermediateNodesList(conf.getInputFilePath(PropertyKeys.intermediateNodesFile));
			// get the list of appended edges and its new ID. if it is not already there, create a new one, and append it to the list of
			// concatenated edges.
			loadConcatedPropertiesList(conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseNewConcatenatedPropertiesFile));
			int originalListOfConcatenatedProps = concatenatedEdgesToNewEdgeIdMap.size();

			// now parse thru the data graph, to create the list of edges incident on a node. Ignore intermediate nodes, merge properties
			// when we find an intermediate nodes.
			ArrayList<HashSet<Integer>> mergedPropList = new ArrayList<HashSet<Integer>>();
			processFreebaseGraph(mergedPropList);
			// write the merged list to file.
			writeMergedListToFile(mergedPropList, conf.getInputFilePath(PropertyKeys.datagraphMergedPropertiesFile));

			// in case we added some new concatenated list of properties, add it to the already existing list. the previous list was created
			// only using the workload. that may have not covered all possible concatenated prop list.
			if(concatenatedEdgesToNewEdgeIdMap.size() > originalListOfConcatenatedProps) {
				System.out.println("writing a new concatenated prop list");
				writeNewConcatenatedPropList();
				writeNewDistinctProperties();
			}

		} else {
			// now parse thru the data graph, to create the list of edges incident on a node. Ignore intermediate nodes, merge properties
			// when we find an intermediate nodes.
			ArrayList<HashSet<Integer>> mergedPropList = new ArrayList<HashSet<Integer>>();
			processFreebaseGraph(mergedPropList);
			// write the merged list to file.
			writeMergedListToFile(mergedPropList, conf.getInputFilePath(PropertyKeys.datagraphMergedPropertiesFile));
		}
		System.out.println("Number of intermediate nodes = " + numIntermediateNodes);
		System.out.println("Number of entity nodes = " + numEntityNodes);
	}

	private void writeNewDistinctProperties() {
		HashSet<Integer> alreadyPresentProps = new HashSet<Integer>();
		ArrayList<String> props = new ArrayList<String>();
		try{
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.distinctPropertiesList));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				alreadyPresentProps.add(Integer.parseInt(line.trim()));
				props.add(line);
			}
			br.close();
			// write the existing entries, and new ones created now to the same file, afresh.
			Iterator<String> iter = concatenatedEdgesToNewEdgeIdMap.keySet().iterator();
			while(iter.hasNext()) {
				int newprop = concatenatedEdgesToNewEdgeIdMap.get(iter.next());
				if(!alreadyPresentProps.contains(newprop)) {
					alreadyPresentProps.add(newprop);
					props.add(""+newprop);
				}
			}

			BufferedWriter bw = new BufferedWriter(new FileWriter(conf.getInputFilePath(PropertyKeys.distinctPropertiesList)));
			for(String prop : props) {
				bw.write(prop+"\n");
			}
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void writeMergedListToFile(ArrayList<HashSet<Integer>> mergedPropList, String datagraphMergedPropsFilePath) {
		try {
			System.out.println("writing output to file: " + datagraphMergedPropsFilePath);
			// write the new properties that were created by merging the intermediate node based edges.
			FileWriter fw1 = new FileWriter(datagraphMergedPropsFilePath);
			BufferedWriter bw1 = new BufferedWriter(fw1);
			for(HashSet<Integer> edges : mergedPropList) {
				Iterator<Integer> iter = edges.iterator();
				MutableString elist = new MutableString();
				while(iter.hasNext()) {
					elist = elist.append(iter.next()).append(",");
				}
				elist = elist.append("\n");
				bw1.write(elist.toString());
			}
			bw1.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void processFreebaseGraph(ArrayList<HashSet<Integer>> mergedPropList) {
		HashMap<Integer, Integer> originalToTempNodeIDMap = new HashMap<Integer, Integer>();
		processIntermediateNeighbors(mergedPropList, originalToTempNodeIDMap, srcDataGraph);
		processIntermediateNeighbors(mergedPropList, originalToTempNodeIDMap, objDataGraph);
	}

	private void processIntermediateNeighbors(ArrayList<HashSet<Integer>> mergedPropList, 
			HashMap<Integer, Integer> originalToArrayIndexMap, HashMap<Integer, ArrayList<ObjNodeIntProperty>> datagraph) {
		Iterator<Integer> iter = datagraph.keySet().iterator();
		while(iter.hasNext()) {
			int origNode = iter.next();
			// ignore intermediate nodes.
			if(isFreebaseDataset && intermediateNodes.contains(origNode)) {
				numIntermediateNodes++;
				continue;
			}
			int index;
			if(originalToArrayIndexMap.containsKey(origNode))
				index = originalToArrayIndexMap.get(origNode);
			else {
				index = arrayIndex++;
				originalToArrayIndexMap.put(origNode, index);
				//	System.out.println(origNode);
				numEntityNodes++;
			}
			HashSet<Integer> edgeList;
			if(index < mergedPropList.size()) {
				// this node was already seen. add to existing hashset
				edgeList = mergedPropList.get(index);
			}
			else if(index == mergedPropList.size()) {
				// this is a newly seen node. push to the arraylist
				edgeList = new HashSet<Integer>();
			}
			else {
				// this should not be happening!!!!!!!! this is wrong!
				System.out.println("ERROR: this should not happen! Lets fail with a null pointer soon!");
				logger.error("ERROR: this should not happen!");
				edgeList = null;
			}

			ArrayList<ObjNodeIntProperty> nodeNeighs = datagraph.get(origNode);
			for(ObjNodeIntProperty on : nodeNeighs) {
				if(isFreebaseDataset && intermediateNodes.contains(on.dest)) {
					// this is an intermediate node. handle the properties by concatenating.
					concatenatePropertyAndAdd(on.prop, on.dest, edgeList);
				}
				else {
					edgeList.add(on.prop);
				}
			}
			if(index == mergedPropList.size())
				mergedPropList.add(edgeList);
		}
	}

	private void concatenateProperty(int startProp, int intermediateNode, HashSet<Integer> edgeList,
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> datagraph) {
		if(datagraph.containsKey(intermediateNode)) {
			for(ObjNodeIntProperty on : datagraph.get(intermediateNode)) {
				if(on.prop == startProp)
					continue;
				String concatProp = "";
				if(startProp < on.prop)
					concatProp = startProp + "," + on.prop;
				else
					concatProp = on.prop + "," + startProp;
				int mergedProp;
				if(concatenatedEdgesToNewEdgeIdMap.containsKey(concatProp)) {
					mergedProp = concatenatedEdgesToNewEdgeIdMap.get(concatProp);
				}
				else {
					mergedProp = newCombinedPropID++;
					concatenatedEdgesToNewEdgeIdMap.put(concatProp, mergedProp);
				}
				edgeList.add(mergedProp);
			}
		}
	}

	private void concatenatePropertyAndAdd(int startProp, int intermediateNode, HashSet<Integer> edgeList) {
		concatenateProperty(startProp, intermediateNode, edgeList, srcDataGraph);
		concatenateProperty(startProp, intermediateNode, edgeList, objDataGraph);
	}

	private void writeNewConcatenatedPropList() {
		try {
			// write the new properties that were created by merging the intermediate node based edges.
			FileWriter fw1 = new FileWriter(conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseNewConcatenatedPropertiesFile));
			BufferedWriter bw1 = new BufferedWriter(fw1);
			Iterator<String> iter = concatenatedEdgesToNewEdgeIdMap.keySet().iterator();
			while(iter.hasNext()) {
				String concatProp = iter.next();
				bw1.write(concatProp + "\t" + concatenatedEdgesToNewEdgeIdMap.get(concatProp) + "\n");
			}
			bw1.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void loadConcatedPropertiesList(String inputFilePath) {
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] split = line.split("\t");
					int newPropID = Integer.parseInt(split[1]);
					if(newPropID > newCombinedPropID) {
						newCombinedPropID = newPropID;
					}
					concatenatedEdgesToNewEdgeIdMap.put(split[0].trim(), newPropID);
				}
				newCombinedPropID++;
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

	private void loadIntermediateNodesList(String inputFilePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try {
				String line = null;
				while((line = br.readLine()) != null) {
					intermediateNodes.add(Integer.parseInt(line));
				}
			} finally {
				br.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		DatagraphMergeProperties dmp = new DatagraphMergeProperties();
		System.out.println("start finding the correlated props in the data graph");
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			dmp.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				dmp.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				dmp.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				dmp.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		dmp.mergeCorrelatedProperties();
		System.out.println("Done generating correlated props in the data graph");
	}
}
