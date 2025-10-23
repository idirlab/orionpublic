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

import org.apache.log4j.Logger;

import viiq.barcelonaCorpus.ParseYahooWikiFilesConstants;
import viiq.commons.DataGraphGenerator;
import viiq.commons.ObjNodeIntProperty;
import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;


class MyDeterminePropertiesThread implements Runnable {
	private final Object counterLock = new Object();

	/*Logger logger = null;
	private synchronized void instantiateLogger() {
		if(logger == null) {
			logger = Logger.getLogger(getClass());
		}
	}*/

	private void findPropertiesBetweenEntities(String inputFilePath, String outputFilePath) {
		try {
			FileWriter fw = new FileWriter(outputFilePath);
			BufferedWriter bw = new BufferedWriter(fw);
			FileReader fr = new FileReader(inputFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			// If an entity ID corresponding to a ##%PAGE is not found in freebase, skip the entire page and its sentences..
			int mainEntity = -1;
			while((line = br.readLine()) != null) {
				if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER)) {
					bw.write(ParseYahooWikiFilesConstants.PAGE_MARKER + "\n");
					String regex = "\\s+|"+BarcelonaToFreebaseConstants.pageValueDelimiter;
					String[] splitLine = line.split(regex);
					if(splitLine.length >= 2) {
						mainEntity = Integer.parseInt(splitLine[1]);
					} else {
						System.out.println("ERROR in PAGE line of file: " + inputFilePath + " : in line: " + line);
					}
					continue;
				}
				int entCnt = 0;
				int[] entities = new int[ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE];

				// if we are considering to add the main entity too..
				entities[entCnt++] = mainEntity;

				String[] splitLine = line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
				if(splitLine != null && splitLine.length >= 2) {
					String[] farEnts = splitLine[1].split(ParseYahooWikiFilesConstants.farEntityDelimiter);
					for(int i = 0; i<farEnts.length; i++) {
						if(farEnts[i].isEmpty())
							continue;
						String[] nearEnts = farEnts[i].split(ParseYahooWikiFilesConstants.closeEntityDelimiter);
						if(nearEnts.length >= ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE) {
							// this is a ad-hoc fix for lines which have too many entities. I had observed that this normally happens
							// when you are looking at wiki files which talk about category information (and not text). such lines might crop
							// up.. so ignoring lines which have too many entities in them (assuming they are such not-so-useful sentences).
							continue;
						}
						for(int j=0; j<nearEnts.length; j++) {
							if(nearEnts[j].isEmpty())
								continue;
							try {
								entities[entCnt] = Integer.parseInt(nearEnts[j].trim());
							} catch(NumberFormatException nfe) {
								System.out.println("FILE " + inputFilePath + "\t" + line);
								System.out.println("Number format Exception: " + line);
							}
							entCnt++;
						}
					}
					// we now have all entities in the line in an array.. find all pairs relationships between them now..
					if(entCnt < entities.length)
						entities[entCnt] = -1;
					if(entCnt > 1) {
						MutableString propertyLine = getAllPairProperties(entities);
						if(propertyLine != null) {
							// write this output to the file..
							MutableString outline = new MutableString(splitLine[0]).append(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
							outline.append(propertyLine).append("\n");
							bw.write(outline.toString());
						}
					}
				} else {
					System.out.println("ERROR in file: " + inputFilePath + " : in line: " + line);
				}
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private MutableString getAllPairProperties(int[] entities) {
		MutableString propLine = new MutableString();
		try {
			ArrayList<Integer> properties = new ArrayList<Integer>();
			ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes = new ArrayList<HashSet<Integer>>();
			for(int i=0; i<entities.length-1 && entities[i] != -1; i++) {
				getSingleProperty(entities[i], entities, i+1, properties, intermediateNeighborsOfNodes);
			}
			if(!properties.isEmpty()) {
				Collections.sort(properties);
				for(int i=0; i<properties.size(); i++) {
					propLine.append(properties.get(i)).append(BarcelonaToFreebaseConstants.interPropertyDelimiter);
				}
			} else {
				propLine = null;
			}
		} catch(Exception e) {
			propLine = null;
			e.printStackTrace();
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
	private void getSingleProperty(int src, int[] dests, int destStartIndex, ArrayList<Integer> properties,
			ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes) {
		// this is a little better version of the other getSigleProperty for the type of DS used to store the data graph.
		HashSet<Integer> destEnts = new HashSet<Integer>();
		for(int j=destStartIndex; j<dests.length; j++)
		{
			destEnts.add(dests[j]);
		}
		if(DeterminePropertiesThread.srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNodeIntProperty> objList = DeterminePropertiesThread.srcDataGraph.get(src);
			for(ObjNodeIntProperty on : objList) {
				if(destEnts.contains(on.dest)) {
					properties.add(on.prop);
				}
				else if(DeterminePropertiesThread.isFreebaseDataset && DeterminePropertiesThread.intermediateNodes.contains(on.dest)) {
					// this is to be executed only with Freebase for now since it has intermediate nodes.
					// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
					ArrayList<Integer> intermediateProperties = getIntermediateNodeNeighbor(src, on.prop, on.dest, destEnts, intermediateNeighborsOfNodes);
					// on.prop, along with the properties in "intermediateProperties" must be combined together.
					if(!intermediateProperties.isEmpty()) {
						for(int intProp : intermediateProperties) {
							String concat = "";
							if(on.prop < intProp) {
								concat = on.prop + "," + intProp;
							}
							else if(on.prop > intProp){
								concat = intProp + "," + on.prop;
							}
							synchronized (counterLock) {
								if(!DeterminePropertiesThread.seenConcatenatedEdgesMappedIDs.containsKey(concat)) {
									//	System.out.println(concat + " " + newPropID);
									//logger.info(concat + " " + newPropID);
									DeterminePropertiesThread.seenConcatenatedEdgesMappedIDs.put(concat, DeterminePropertiesThread.newPropID++);	
								}
								properties.add(DeterminePropertiesThread.seenConcatenatedEdgesMappedIDs.get(concat));
							}
						}
					}
				}
			}
		}
		if(DeterminePropertiesThread.objDataGraph.containsKey(src))
		{
			ArrayList<ObjNodeIntProperty> objList = DeterminePropertiesThread.objDataGraph.get(src);
			for(ObjNodeIntProperty on : objList) {
				if(destEnts.contains(on.dest)) {
					properties.add(on.prop);
				}
				else if(DeterminePropertiesThread.isFreebaseDataset && DeterminePropertiesThread.intermediateNodes.contains(on.dest)) {
					// this is to be executed only with Freebase for now since it has intermediate nodes.
					// get the neighbors of on.dest, and see if any of those neighbors are in destEnts.
					ArrayList<Integer> intermediateProperties = getIntermediateNodeNeighbor(src, on.prop, on.dest, destEnts, intermediateNeighborsOfNodes);
					// on.prop, along with the properties in "intermediateProperties" must be combined together.
					if(!intermediateProperties.isEmpty()) {
						for(int intProp : intermediateProperties) {
							String concat = "";
							if(on.prop < intProp) {
								concat = on.prop + "," + intProp;
							}
							else if(on.prop > intProp){
								concat = intProp + "," + on.prop;
							}
							synchronized (counterLock) {
								if(!DeterminePropertiesThread.seenConcatenatedEdgesMappedIDs.containsKey(concat)) {
									//	System.out.println(concat + " " + newPropID);
									//logger.info(concat + " " + newPropID);
									DeterminePropertiesThread.seenConcatenatedEdgesMappedIDs.put(concat, DeterminePropertiesThread.newPropID++);
								}
								properties.add(DeterminePropertiesThread.seenConcatenatedEdgesMappedIDs.get(concat));
							}
						}
					}
				}
			}
		}
	}

	private ArrayList<Integer> getIntermediateNodeNeighbor(int origNode, int firstprop, int intermediateNode, HashSet<Integer> destEnts,
			ArrayList<HashSet<Integer>> intermediateNeighborsOfNodes) {
		ArrayList<Integer> intermediateProperties = new ArrayList<Integer>();
		HashSet<Integer> intermediateNeighbors = new HashSet<Integer>();
		if(DeterminePropertiesThread.srcDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNodeIntProperty> objList = DeterminePropertiesThread.srcDataGraph.get(intermediateNode);
			for(ObjNodeIntProperty on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop && !areIntermediateNeighbors(origNode, on.dest, intermediateNeighborsOfNodes)) {
					intermediateNeighbors.add(on.dest);
					intermediateProperties.add(on.prop);
					//logger.info("first prop = " + firstprop + " new prop = " + on.prop + " orig node = " + origNode + " intermediate node = " + intermediateNode + " final node = " + on.dest);
				}
			}
		}
		if(DeterminePropertiesThread.objDataGraph.containsKey(intermediateNode))
		{
			ArrayList<ObjNodeIntProperty> objList = DeterminePropertiesThread.objDataGraph.get(intermediateNode);
			for(ObjNodeIntProperty on : objList) {
				if(destEnts.contains(on.dest) && on.prop != firstprop && !areIntermediateNeighbors(origNode, on.dest, intermediateNeighborsOfNodes)) {
					intermediateNeighbors.add(on.dest);
					intermediateProperties.add(on.prop);
					//logger.info("first prop = " + firstprop + " prop = " + on.prop + " orig node = " + origNode + " intermediate node = " + intermediateNode + " final node = " + on.dest);
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
	
	public void run() {
		//instantiateLogger();
		int tid = Integer.parseInt(Thread.currentThread().getName());
		for(int i=tid; i<DeterminePropertiesThread.entityMappedFiles.length; i=i+DeterminePropertiesThread.nthreads) {
			if(DeterminePropertiesThread.entityMappedFiles[i].isFile())
			{
				String outFileName = DeterminePropertiesThread.entityMappedFiles[i].getName();
				String inputFilePath = DeterminePropertiesThread.entityMappedFiles[i].getAbsolutePath();
				String mappedOutputFilePath = DeterminePropertiesThread.outputFolderPath + outFileName;
				findPropertiesBetweenEntities(inputFilePath, mappedOutputFilePath);
			}
		}
	}
}

public class DeterminePropertiesThread {
	static Config conf = null;
	static boolean isFreebaseDataset = false;
	static HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	static HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph = new HashMap<Integer, ArrayList<ObjNodeIntProperty>>();
	static HashSet<Integer> intermediateNodes = new HashSet<Integer>();
	static HashMap<String, Integer> seenConcatenatedEdgesMappedIDs = new HashMap<String, Integer>();
	static int newPropID = 30000;
	// The largest ID we have for the new Freebase dataset is: 64666131
	//static int newPropID = 64666135; 
	static File[] entityMappedFiles;
	static String outputFolderPath;
	static int nthreads = 8;
	// this is different from traditional data graph storing:
	// <SourceNode, <ObjectNode, {PropertyIDs}>>
	// I need to use the data graph to quickly find if an entity e2 is the neighbor of entity e1. if yes, find all its properties..
	//HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> srcDataGraph = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
	//HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> objDataGraph = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();

	public DeterminePropertiesThread(Config conf) {
		DeterminePropertiesThread.conf = conf;
		//this.logger = Logger.getLogger(getClass());
	}

	public DeterminePropertiesThread() {
		//this.logger = Logger.getLogger(getClass());
	}

	public void identifyPropertiesThread(String inputFolderPath, String outFolderPath) {
		loadDataGraph();
		int datasetFlag = Integer.parseInt(conf.getProp(PropertyKeys.datasetFlag));
		if(datasetFlag == 0)
			isFreebaseDataset = true;
		if(isFreebaseDataset) {
			loadIntermediateNodesList(conf.getInputFilePath(PropertyKeys.intermediateNodesFile));
		}
		nthreads = Integer.parseInt(conf.getProp(PropertyKeys.numberOfThreads));
		System.out.println("Done loading data graph!");
		// read individual files having mapping from barcelona to dataset-entities.
		File inputFolder = new File(inputFolderPath);
		outputFolderPath = new String(outFolderPath);
		entityMappedFiles = inputFolder.listFiles();
		try {
			ArrayList<Thread> at = new ArrayList<Thread>();
			MyDeterminePropertiesThread mt = new MyDeterminePropertiesThread();
			for(int i=0; i<nthreads; i++) {
				Thread thr = new Thread(mt);
				at.add(thr);
				thr.setName(i+"");
				thr.start();
			}
			for(int i=0; i<nthreads; i++) {
				at.get(i).join();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		try {
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
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}

		freeDataGraph();
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

	private void loadDataGraph() {
		DataGraphGenerator dgg = new DataGraphGenerator();
		dgg.loadDataGraphIntProperty(conf.getInputFilePath(PropertyKeys.datagraphFile), srcDataGraph, objDataGraph);
	}

	private void freeDataGraph() {
		freeDS(srcDataGraph);
		freeDS(objDataGraph);
	}

	private void freeDS(HashMap<Integer, ArrayList<ObjNodeIntProperty>> ds) {
		Iterator<Integer> iter = ds.keySet().iterator();
		while(iter.hasNext()) {
			ds.get(iter.next()).clear();
		}
		ds.clear();
		ds = null;
	}
}
