package viiq.barcelonaToFreebase;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.commons.DataGraphGenerator;
import viiq.commons.ObjNode;

import viiq.barcelonaCorpus.ParseYahooWikiFilesConstants;

import viiq.utils.PropertyKeys;

public class DetermineProperties 
{
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	boolean isFreebaseDataset = false;
	
	HashMap<Integer, ArrayList<ObjNode>> srcDataGraph = new HashMap<Integer, ArrayList<ObjNode>>();
	HashMap<Integer, ArrayList<ObjNode>> objDataGraph = new HashMap<Integer, ArrayList<ObjNode>>();
	
	HashSet<Integer> intermediateNodes = new HashSet<Integer>();
	HashMap<String, Long> seenConcatenatedEdgesMappedIDs = new HashMap<String, Long>();
	
	long newPropID = 30000;
	
	// this is different from traditional data graph storing:
	// <SourceNode, <ObjectNode, {PropertyIDs}>>
	// I need to use the data graph to quickly find if an entity e2 is the neighbor of entity e1. if yes, find all its properties..
	//HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> srcDataGraph = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
	//HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> objDataGraph = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
	
	public DetermineProperties(Config conf)
	{
		this.conf = conf;
		//this.logger = Logger.getLogger(getClass());
	}
	
	public DetermineProperties()
	{
		//this.logger = Logger.getLogger(getClass());
	}
		
	public void identifyProperties(String inputFolderPath, String outputFolderPath)
	{
		loadDataGraph();
		int datasetFlag = Integer.parseInt(conf.getProp(PropertyKeys.datasetFlag));
		if(datasetFlag == 0)
			isFreebaseDataset = true;
		if(isFreebaseDataset) {
			loadIntermediateNodesList(conf.getInputFilePath(PropertyKeys.intermediateNodesFile));
		}
		System.out.println("Done loading data graph!");
		// read individual files having mapping from barcelona to dataset-entities.
		File inputFolder = new File(inputFolderPath);
		File[] entityMappedFiles = inputFolder.listFiles();
		for(int i=0; i<entityMappedFiles.length; i++)
		{
			if(entityMappedFiles[i].isFile())
			{
				String outFileName = entityMappedFiles[i].getName();
				String inputFilePath = entityMappedFiles[i].getAbsolutePath();
				String mappedOutputFilePath = outputFolderPath + outFileName;
				findPropertiesBetweenEntities(inputFilePath, mappedOutputFilePath);
			}
		}
		freeDataGraph();
	}
	
	private void findPropertiesBetweenEntities(String inputFilePath, String outputFilePath)
	{
		try
		{
			FileWriter fw = new FileWriter(outputFilePath);
			BufferedWriter bw = new BufferedWriter(fw);
			FileReader fr = new FileReader(inputFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			// If an entity ID corresponding to a ##%PAGE is not found in freebase, skip the entire page and its sentences..
			int mainEntity = -1;
			while((line = br.readLine()) != null)
			{
				if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER))
				{
					bw.write(ParseYahooWikiFilesConstants.PAGE_MARKER + "\n");
					mainEntity = Integer.parseInt(line.split(BarcelonaToFreebaseConstants.pageValueDelimiter)[1]);
					continue;
				}
				int entCnt = 0;
				int[] entities = new int[ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE];
				
				// if we are considering to add the main entity too..
				entities[entCnt++] = mainEntity;
				
				String[] splitLine = line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
				
				String[] farEnts = splitLine[1].split(ParseYahooWikiFilesConstants.farEntityDelimiter);
				for(int i = 0; i<farEnts.length; i++)
				{
					if(farEnts[i].isEmpty())
						continue;
					String[] nearEnts = farEnts[i].split(ParseYahooWikiFilesConstants.closeEntityDelimiter);
					if(nearEnts.length >= ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE) {
						// this is a ad-hoc fix for lines which have too many entities. I had observed that this normally happens
						// when you are looking at wiki files which talk about category information (and not text). such lines might crop
						// up.. so ignoring lines which have too many entities in them (assuming they are such not-so-useful sentences).
						continue;
					}
					for(int j=0; j<nearEnts.length; j++)
					{
						if(nearEnts[j].isEmpty())
							continue;
						try
						{
							entities[entCnt] = Integer.parseInt(nearEnts[j].trim());
						}
						catch(NumberFormatException nfe)
						{
							System.out.println("FILE " + inputFilePath + "\t" + line);
							logger.debug("Number format Exception: " + line);
						}
						entCnt++;
					}
				}
				// we now have all entities in the line in an array.. find all pairs relationships between them now..
				if(entCnt < entities.length)
					entities[entCnt] = -1;
				if(entCnt > 1)
				{
					MutableString propertyLine = getAllPairProperties(entities);
					if(propertyLine != null)
					{
						// write this output to the file..
						MutableString outline = new MutableString(splitLine[0]).append(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
						outline.append(propertyLine).append("\n");
						bw.write(outline.toString());
					}
					else
					{
						logger.info("NO properties found for line: " + line);
					}
				}
				else
				{
					logger.info("NOT enough entities found for line: " + line);
				}
			}
			br.close();
			bw.close();
			if(isFreebaseDataset && !seenConcatenatedEdgesMappedIDs.isEmpty()) {
				// write the new properties that were created by merging the intermediate node based edges.
				FileWriter fw1 = new FileWriter(conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseNewConcatenatedPropertiesFile));
				BufferedWriter bw1 = new BufferedWriter(fw1);
				Iterator<String> iter = seenConcatenatedEdgesMappedIDs.keySet().iterator();
				while(iter.hasNext()) {
					String concatProp = iter.next();
					bw1.write(concatProp + "\t" + seenConcatenatedEdgesMappedIDs.get(concatProp) + "\n");
				}
				bw1.close();
			}
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
	
	private MutableString getAllPairProperties(int[] entities)
	{
		MutableString propLine = new MutableString();
		ArrayList<Long> properties = new ArrayList<Long>();
		ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes = new ArrayList<HashSet<Integer>>();
		for(int i=0; i<entities.length-1 && entities[i] != -1; i++)
		{
			getSingleProperty(entities[i], entities, i+1, properties, intermediateNeighborsOfNodes);
		}
		/*for(int i=0; i<entities.length-1 && entities[i] != -1; i++)
		{
			for(int j=i+1; j<entities.length; j++)
			{
				getSingleProperty(entities[i], entities[j], properties);
			}
		}*/
		if(!properties.isEmpty())
		{
			Collections.sort(properties);
			for(int i=0; i<properties.size(); i++)
			{
				propLine.append(properties.get(i)).append(BarcelonaToFreebaseConstants.interPropertyDelimiter);
			}
		}
		else
		{
			propLine = null;
		}
		return propLine;
	}
	
	/**
	 * Given an source(object) entity and the set of all the object(source) entities, find the properties between pairs of these entities
	 * and add it into properties.
	 * @param src
	 * @param dests
	 * @param destStartIndex
	 * @param properties
	 */
	private void getSingleProperty(int src, int[] dests, int destStartIndex, ArrayList<Long> properties,
			ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes) {
		// this is a little better version of the other getSigleProperty for the type of DS used to store the data graph.
		HashSet<Integer> destEnts = new HashSet<Integer>();
		for(int j=destStartIndex; j<dests.length; j++)
		{
			destEnts.add(dests[j]);
		}
		if(srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNode> objList = srcDataGraph.get(src);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest)) {
					properties.add(on.prop);
				}
				else if(isFreebaseDataset && intermediateNodes.contains(on.dest)) {
					/*if(destStartIndex == 1) {
						// this means "src" is the mainEntity. Mark all nodes that are connected to main entity thru intermediate node. In future, we
						// will not look for intermediate nodes connecting such nodes.
						// this is to be executed only with Freebase for now since it has intermediate nodes.
						// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
						
						ArrayList<Long> intermediateProperties = getIntermediateNodeNeighbor(on.prop, on.dest, destEnts, mainEntityIntermediateNeighbors);
						// on.prop, along with the properties in "intermediateProperties" must be combined together.
						if(!intermediateProperties.isEmpty()) {
							for(long intProp : intermediateProperties) {
								String concat = "";
								if(on.prop < intProp) {
									concat = on.prop + "," + intProp;
								}
								else if(on.prop > intProp){
									concat = intProp + "," + on.prop;
								}
								if(!seenConcatenatedEdgesMappedIDs.containsKey(concat)) {
									//System.out.println(concat);
									seenConcatenatedEdgesMappedIDs.put(concat, newPropID++);
								}
								properties.add(seenConcatenatedEdgesMappedIDs.get(concat));
							}
							properties.add(on.prop);
							for(long intProp : intermediateProperties) {
								properties.add(intProp);
							}
						}
					}
					else {
						// this is to be executed only with Freebase for now since it has intermediate nodes.
						// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
						ArrayList<Long> intermediateProperties = getIntermediateNodeNeighbor(src, on.prop, on.dest, destEnts, intermediateNeighborsOfNodes);
						// on.prop, along with the properties in "intermediateProperties" must be combined together.
						if(!intermediateProperties.isEmpty()) {
							for(long intProp : intermediateProperties) {
								String concat = "";
								if(on.prop < intProp) {
									concat = on.prop + "," + intProp;
								}
								else if(on.prop > intProp){
									concat = intProp + "," + on.prop;
								}
								if(!seenConcatenatedEdgesMappedIDs.containsKey(concat)) {
									//System.out.println(concat);
									seenConcatenatedEdgesMappedIDs.put(concat, newPropID++);
								}
								properties.add(seenConcatenatedEdgesMappedIDs.get(concat));
							}
							properties.add(on.prop);
							for(long intProp : intermediateProperties) {
								properties.add(intProp);
							}
						}
					}*/
					// this is to be executed only with Freebase for now since it has intermediate nodes.
					// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
					ArrayList<Long> intermediateProperties = getIntermediateNodeNeighbor(src, on.prop, on.dest, destEnts, intermediateNeighborsOfNodes);
					// on.prop, along with the properties in "intermediateProperties" must be combined together.
					if(!intermediateProperties.isEmpty()) {
						for(long intProp : intermediateProperties) {
							String concat = "";
							if(on.prop < intProp) {
								concat = on.prop + "," + intProp;
							}
							else if(on.prop > intProp){
								concat = intProp + "," + on.prop;
							}
							if(!seenConcatenatedEdgesMappedIDs.containsKey(concat)) {
							//	System.out.println(concat + " " + newPropID);
								logger.info(concat + " " + newPropID);
								seenConcatenatedEdgesMappedIDs.put(concat, newPropID++);
							}
							properties.add(seenConcatenatedEdgesMappedIDs.get(concat));
						}
						/*properties.add(on.prop);
						for(long intProp : intermediateProperties) {
							properties.add(intProp);
						}*/
					}
				}
			}
		}
		if(objDataGraph.containsKey(src))
		{
			ArrayList<ObjNode> objList = objDataGraph.get(src);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest)) {
					properties.add(on.prop);
				}
				else if(isFreebaseDataset && intermediateNodes.contains(on.dest)) {
					// this is to be executed only with Freebase for now since it has intermediate nodes.
					// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
					ArrayList<Long> intermediateProperties = getIntermediateNodeNeighbor(src, on.prop, on.dest, destEnts, intermediateNeighborsOfNodes);
					// on.prop, along with the properties in "intermediateProperties" must be combined together.
					if(!intermediateProperties.isEmpty()) {
						for(long intProp : intermediateProperties) {
							String concat = "";
							if(on.prop < intProp) {
								concat = on.prop + "," + intProp;
							}
							else if(on.prop > intProp){
								concat = intProp + "," + on.prop;
							}
							if(!seenConcatenatedEdgesMappedIDs.containsKey(concat)) {
							//	System.out.println(concat + " " + newPropID);
								logger.info(concat + " " + newPropID);
								seenConcatenatedEdgesMappedIDs.put(concat, newPropID++);
							}
							properties.add(seenConcatenatedEdgesMappedIDs.get(concat));
						}
						/*properties.add(on.prop);
						for(long intProp : intermediateProperties) {
							properties.add(intProp);
						}*/
					}
					
					
					/*// this is to be executed only with Freebase for now since it has intermediate nodes.
					// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
					ArrayList<Long> intermediateProperties = getIntermediateNodeNeighbor(on.prop, on.dest, destEnts);
					// on.prop, along with the properties in "intermediateProperties" must be combined together.
					if(!intermediateProperties.isEmpty()) {
						for(long intProp : intermediateProperties) {
							String concat = "";
							if(on.prop < intProp) {
								concat = on.prop + "" + intProp;
							}
							else if(on.prop > intProp){
								concat = intProp + "" + on.prop;
							}
							if(!seenConcatenatedEdges.contains(concat)) {
								System.out.println(concat);
								seenConcatenatedEdges.add(concat);
							}
							properties.add(Long.parseLong(concat));
						}
						properties.add(on.prop);
						for(long intProp : intermediateProperties) {
							properties.add(intProp);
						}
					}*/
				}
			}
		}
	}
	
	private ArrayList<Long> getIntermediateNodeNeighbor(long firstprop, int intermediateNode, HashSet<Integer> destEnts,
			HashSet<Integer> mainEntityIntermediateNeighbors) {
		ArrayList<Long> intermediateProperties = new ArrayList<Long>();
		if(srcDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNode> objList = srcDataGraph.get(intermediateNode);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop) {
					mainEntityIntermediateNeighbors.add(on.dest);
					intermediateProperties.add(on.prop);
				}
			}
		}
		if(objDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNode> objList = objDataGraph.get(intermediateNode);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop) {
					mainEntityIntermediateNeighbors.add(on.dest);
					intermediateProperties.add(on.prop);
				}
			}
		}
		return intermediateProperties;
	}
	
	private ArrayList<Long> getIntermediateNodeNeighbor(int origNode, long firstprop, int intermediateNode, HashSet<Integer> destEnts,
			ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes) {
		ArrayList<Long> intermediateProperties = new ArrayList<Long>();
		HashSet<Integer> intermediateNeighbors = new HashSet<Integer>();
		if(srcDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNode> objList = srcDataGraph.get(intermediateNode);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop && !areIntermediateNeighbors(origNode, on.dest, intermediateNeighborsOfNodes)) {
					intermediateNeighbors.add(on.dest);
					intermediateProperties.add(on.prop);
					logger.info("first prop = " + firstprop + " new prop = " + on.prop + " orig node = " + origNode + " intermediate node = " + intermediateNode + " final node = " + on.dest);
				}
			}
		}
		if(objDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNode> objList = objDataGraph.get(intermediateNode);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop && !areIntermediateNeighbors(origNode, on.dest, intermediateNeighborsOfNodes)) {
					intermediateNeighbors.add(on.dest);
					intermediateProperties.add(on.prop);
					logger.info("first prop = " + firstprop + " prop = " + on.prop + " orig node = " + origNode + " intermediate node = " + intermediateNode + " final node = " + on.dest);
				}
			}
		}
		if(!intermediateNeighbors.isEmpty()) {
			intermediateNeighborsOfNodes.add(intermediateNeighbors);
		}
		return intermediateProperties;
	}
	
	private boolean areIntermediateNeighbors(int ent1, int ent2, ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes) {
		boolean areNeighbors = false;
		for(HashSet<Integer> interNeighs : intermediateNeighborsOfNodes) {
			if(interNeighs.contains(ent1) && interNeighs.contains(ent2)) {
				areNeighbors = true;
				break;
			}
		}
		return areNeighbors;
	}
	
	private ArrayList<Long> getIntermediateNodeNeighbor(long firstprop, int intermediateNode, HashSet<Integer> destEnts) {
		ArrayList<Long> intermediateProperties = new ArrayList<Long>();
		if(srcDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNode> objList = srcDataGraph.get(intermediateNode);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop) {
					intermediateProperties.add(on.prop);
				}
			}
		}
		if(objDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNode> objList = objDataGraph.get(intermediateNode);
			for(ObjNode on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop) {
					intermediateProperties.add(on.prop);
				}
			}
		}
		return intermediateProperties;
	}
	
	private void getSingleProperty(int src, int obj, ArrayList<Long> properties)
	{
		if(srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNode> objList = srcDataGraph.get(src);
			for(ObjNode on : objList) {
				if(on.dest == obj) {
					properties.add(on.prop);
				}
			}
		}
		if(objDataGraph.containsKey(src))
		{
			ArrayList<ObjNode> objList = objDataGraph.get(src);
			for(ObjNode on : objList) {
				if(on.dest == obj) {
					properties.add(on.prop);
				}
			}
		}
	}
	
	private void loadIntermediateNodesList(String inputFilePath) {
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					intermediateNodes.add(Integer.parseInt(line));
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
	
	private void loadDataGraph()
	{
		DataGraphGenerator dgg = new DataGraphGenerator();
		dgg.loadDataGraph(conf.getInputFilePath(PropertyKeys.datagraphFile), srcDataGraph, objDataGraph);
		/*try
		{
			BufferedReader br = new BufferedReader(new FileReader(conf.getInputFilePath(PropertyKeys.datagraphFile)));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] tokens = line.split(",");
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraph(src, prop, dest);
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
		}*/
	}
	
	private void freeDataGraph() {
		freeDS(srcDataGraph);
		freeDS(objDataGraph);
	}
	private void freeDS(HashMap<Integer, ArrayList<ObjNode>> ds) {
		Iterator<Integer> iter = ds.keySet().iterator();
		while(iter.hasNext()) {
			ds.get(iter.next()).clear();
		}
		ds.clear();
		ds = null;
	}
	
	/*private void addEdgeToDataGraph(int src, int prop, int obj)
	{
		// Add edge in forward direction...
		addEdge(srcDataGraph, src, prop, obj);
		// Add this same edge in the opposite direction..
		addEdge(objDataGraph, obj, prop, src);
	}
	
	private void addEdge(HashMap<Integer, ArrayList<ObjNode>> dataGraph, int src, int prop, int obj)
	{
		if(dataGraph.containsKey(src))
		{
			HashMap<Integer, ArrayList<Integer>> tuple = dataGraph.get(src);
			if(tuple.containsKey(obj))
			{
				ArrayList<Integer> props = tuple.get(obj);
				props.add(prop);
			}
			else
			{
				ArrayList<Integer> props = new ArrayList<Integer>();
				props.add(prop);
				tuple.put(obj, props);
			}
		}
		else
		{
			HashMap<Integer, ArrayList<Integer>> tuple = new HashMap<Integer, ArrayList<Integer>>();
			ArrayList<Integer> props = new ArrayList<Integer>();
			props.add(prop);
			tuple.put(obj, props);
			dataGraph.put(src, tuple);
		}
	}*/
	
	public static void main(String[] args)
	{
		DetermineProperties dp = new DetermineProperties();
		if(args.length < 1)
		{
			System.out.println("Need an input properties file! Exiting program...");
			dp.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else
		{
			try
			{
				dp.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce)
			{
				System.out.println("Error in properties file configuration! Exiting program...");
				dp.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe)
			{
				System.out.println("IO exception while reading the properties file! Exiting program...");
				dp.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		dp.identifyProperties(dp.conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetEntityMapping), 
				dp.conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetPropertyFolder));
	}
}
