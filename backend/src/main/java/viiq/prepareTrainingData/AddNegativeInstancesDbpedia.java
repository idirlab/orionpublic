package viiq.prepareTrainingData;

import viiq.graphQuerySuggestionMain.Config;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;
import viiq.barcelonaToFreebase.BarcelonaToFreebaseConstants;

public class AddNegativeInstancesDbpedia {

	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	int numOfProps = 0;
	
	// index to assign array index to the actual property ID. key = actual property ID in dataset, value = new property index starting from 0.
	HashMap<Integer, Integer> propIdToTempIdMap = new HashMap<Integer, Integer>();
	// keep an inverted index to track the array index to actual property ID. key = new property index starting from 0, value = actual property ID in dataset.
	HashMap<Integer, Integer> propIdToTempIdInvertedMap = new HashMap<Integer, Integer>();
	// keep the list of all co-occurring properties in a 2-D arraylist. The index of the outer arraylist is the line ID index.
	// each arraylist contains co-occurring props. initially only props found in log file are set to 1, rest are 0. they change later.
	ArrayList<ArrayList<Integer>> cooccurringProperties = new ArrayList<ArrayList<Integer>>();
	//ArrayList<ArrayList<Integer>> onlyPositiveProperties = new ArrayList<ArrayList<Integer>>();
	// keep a list of line ID indexes that a property appears in.
	// key = property Index, value = list of line ID index (basically the index of outer array in cooccurringProperties 
	HashMap<Integer, ArrayList<Integer>> propertyToListInvertedIndex = new HashMap<Integer, ArrayList<Integer>>();
	
	public AddNegativeInstancesDbpedia(Config conf) {
		this.conf = conf;
	}
	
	public AddNegativeInstancesDbpedia() {
		
	}
	
	public static void main(String[] args){
		AddNegativeInstancesDbpedia ani = new AddNegativeInstancesDbpedia();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			ani.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				ani.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				ani.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ani.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		/*ani.readAllProperties();
		ani.readFrequentPropertyList();
		ani.insertNegativeEdges();
		ani.printTrainingData();
		ani.printTrainingDataWithDuplicates();*/
		ani.addNegativeEdgesToLog();
	}
	
	public void addNegativeEdgesToLog() {
		readAllProperties();
		System.out.println("Done reading distinct properties list");
		readFrequentPropertyList();
		System.out.println("Done reading the frequent itemsets file");
		insertNegativeEdges();
		System.out.println("Done inserting negative edges");
		printTrainingData();
		//printTrainingDataWithDuplicates();
	}
	
	private void insertNegativeEdges(){
		/*
		 * The arraylist containing co-occurring properties can take 4 values:
		 * 0 = original entry for negative edge (all 0's get converted to 2 or 3 at the end of this method)
		 * 1 = positive edge
		 * 2 = negative edge 
		 * 3 = deleted edge (this edge will not be displayed in the training set.
		 */
		Iterator<Integer> iter = propertyToListInvertedIndex.keySet().iterator();
		while(iter.hasNext()){
			int propIndex = iter.next();
			ArrayList<Integer> lines = propertyToListInvertedIndex.get(propIndex);
			for(int i=0; i<numOfProps; i++){
				boolean colContainsOne = doesPropertyExist(lines, i);
				if(colContainsOne)
					addNegativeEdge(lines, i);
				else
					deleteNegativeEdge(lines, i);
			}
		}
	}
	
	private boolean doesPropertyExist(ArrayList<Integer> lineIDs, int ind)
	{
		boolean containsOne = false;
		for(int line : lineIDs){
			if(cooccurringProperties.get(line).get(ind) == 1){
				containsOne = true;
				break;
			}
		}
		return containsOne;
	}
	
	private void addNegativeEdge(ArrayList<Integer> lineIDs, int ind)
	{
		for(int line : lineIDs){
			if(cooccurringProperties.get(line).get(ind) != 1)
				cooccurringProperties.get(line).set(ind, 2);
		}
	}
	
	private void deleteNegativeEdge(ArrayList<Integer> lineIDs, int ind)
	{
		for(int line : lineIDs){
			if(cooccurringProperties.get(line).get(ind) == 0)
				cooccurringProperties.get(line).set(ind, 3);
		}
	}
	
	private void readFrequentPropertyList()
	{
		/*
		 * This method reads the list of frequently co-occurring properties list. An inverted list of the same is also created. 
		 */
		try{
			FileReader fr = new FileReader(conf.getOutputFilePath(PropertyKeys.frequentPropertiesFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			int lineID = 0;
			while((line = br.readLine()) != null){
				ArrayList<Integer> allPropsList = new ArrayList<Integer>(numOfProps);
				initializeArrayList(allPropsList);
				String[] props = line.split(",");
				// keeping track of only the specific count and not total count.
				int specificCount = Integer.parseInt(props[props.length-1]);
				int totalCount = Integer.parseInt(props[props.length-2]);
				/*if(specificCount == 0){
				//	ArrayList<Integer> 
				}*/
				for(int i=0; i<props.length-2; i++){
					String prop = props[i];
					int p = propIdToTempIdMap.get(Integer.parseInt(prop));
					allPropsList.set(p, 1);
					if(propertyToListInvertedIndex.containsKey(p)){
						ArrayList<Integer> lines = propertyToListInvertedIndex.get(p);
						lines.add(lineID);
					}
					else{
						ArrayList<Integer> lines = new ArrayList<Integer>();
						lines.add(lineID);
						propertyToListInvertedIndex.put(p, lines);
					}
				}
				// add the (specific) count too.
				allPropsList.add(totalCount);
				allPropsList.add(specificCount);
				// I don't have to specify the index since I am reading the file serially.
				cooccurringProperties.add(allPropsList);
				lineID++;
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
	
	private void initializeArrayList(ArrayList<Integer> arr)
	{
		for(int i=0; i<numOfProps; i++)
			arr.add(0);
	}
	
	private void readAllProperties()
	{
		/* 
		 * Read in property labels from the corresponding data source and find their integer IDs.
		 * We can then use these integer IDs to represent co-occurring properties.
		 */
		try{
			//FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.distinctPropertiesList));
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.distinctPropertiesListMapping));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			int propIndex = 0;
			while((line = br.readLine()) != null) {
				int prop = Integer.parseInt(line);
				propIdToTempIdMap.put(prop, propIndex);
				propIdToTempIdInvertedMap.put(propIndex, prop);
				propIndex++;
			}
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		numOfProps = propIdToTempIdMap.size();
	}
	
	private void printTrainingDataWithDuplicates()
	{
		try{
			FileReader fr = new FileReader(conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile));
			BufferedReader br = new BufferedReader(fr);
			
			FileWriter fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.trainigDataWithDuplicatesIDFile));
			BufferedWriter bw = new BufferedWriter(fw);
			
			/*FileWriter fw2 = new FileWriter(conf.getOutputFilePath(PropertyKeys.trainigDataWithDuplicatesValueFile));
			BufferedWriter bw2 = new BufferedWriter(fw2);*/
			
			String line = null;
			while((line = br.readLine()) != null){
				bw.write(line+"\n");
				String[] splitline = line.split(",");
				ArrayList<Integer> negEdges = new ArrayList<Integer>();
				ArrayList<Integer> posEdges = new ArrayList<Integer>();
				MutableString permEdgeList = new MutableString();
				for(int i=0; i<splitline.length-2; i++){
					int prop = Integer.parseInt(splitline[i]);
					if(prop < 0){
						negEdges.add(prop);
						permEdgeList = permEdgeList.append(prop).append(",");
					}
					else
						posEdges.add(prop);
				}
				if(posEdges.size() == 1 && !negEdges.isEmpty()){
					MutableString edgeList = new MutableString();
					edgeList = edgeList.append(posEdges.get(0)).append(",").append(splitline[splitline.length-2]);
					edgeList = edgeList.append(",").append(splitline[splitline.length-1]).append("\n");
					bw.write(edgeList.toString());
				}
				for(int i=0; i<posEdges.size()-1; i++){
					int p = posEdges.get(i);
					posEdges.set(i, posEdges.get(posEdges.size()-1));
					posEdges.set(posEdges.size()-1, p);
					MutableString edgeList = new MutableString(permEdgeList);
					for(int j=0; j<posEdges.size(); j++){
						edgeList = edgeList.append(posEdges.get(j)).append(",");
					}
					edgeList = edgeList.append(splitline[splitline.length-2]).append(",").append(splitline[splitline.length-1]).append("\n");
					bw.write(edgeList.toString());
				}
			}
			br.close();
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void printTrainingData()
	{
		try{
			FileWriter fw1 = new FileWriter(conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile));
			BufferedWriter bw1 = new BufferedWriter(fw1);
			FileWriter fw2 = new FileWriter(conf.getOutputFilePath(PropertyKeys.trainigDataWithValueFile));
			BufferedWriter bw2 = new BufferedWriter(fw2);
			HashMap<Integer, String> propertyValue = readPropertyMapFile();
			for(ArrayList<Integer> line : cooccurringProperties){
				ArrayList<Integer> propsList = new ArrayList<Integer>();
				int i = 0;
				boolean allPositives = false;
				if(line.get(line.size()-1) == 0)
					allPositives = true;
				for(i=0; i<numOfProps; i++){
					/*
					 * if the edge is 1 => print as positive
					 * if the edge is 2 => print as negative
					 * if the edge is 3 => ignore
					 */
					int prop = propIdToTempIdInvertedMap.get(i);
					if(line.get(i) == 1) {
						propsList.add(prop);
						//props = props.append(prop).append(",");
					}
					else if(line.get(i) == 2 && !allPositives) {
						propsList.add(prop*(-1));
						//props = props.append(prop*(-1)).append(",");
					}
					else if(line.get(i) == 3) {
						// ignore
					}
					else {
						System.out.println("THAT shouldn't have happened!");
						logger.error("That edge should not be 0 : " + i);
					}
				}
				Collections.sort(propsList);
				MutableString propsIDList = new MutableString();
				MutableString propsLabelList = new MutableString();
				for(int prop : propsList){
					propsIDList = propsIDList.append(prop).append(",");
					if(prop < 0)
						propsLabelList = propsLabelList.append("-");
					propsLabelList = propsLabelList.append(propertyValue.get(Math.abs(prop))).append(",");
				}
				// adding the count here.
				propsIDList = propsIDList.append(line.get(i)).append(",").append(line.get(i+1)).append("\n");
				propsLabelList = propsLabelList.append(line.get(i)).append(",").append(line.get(i+1)).append("\n");
				bw1.write(propsIDList.toString());
				bw2.write(propsLabelList.toString());
			}
			bw1.close();
			bw2.close();
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
	
	private HashMap<Integer, String> readPropertyMapFile()
	{
		HashMap<Integer, String> propertyValue = new HashMap<Integer, String>();
		try
		{
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.propertiesMapFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null)
			{
				String[] splitline = line.split(BarcelonaToFreebaseConstants.propValueDelimiter);
				propertyValue.put(Integer.parseInt(splitline[0].trim()), splitline[1].trim());
				//propertyValue.put(splitline[0].trim(), URLDecoder.decode(splitline[1].trim(), "UTF-8"));
			}
			br.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return propertyValue;
	}
}
