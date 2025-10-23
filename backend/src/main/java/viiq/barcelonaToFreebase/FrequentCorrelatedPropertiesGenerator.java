package viiq.barcelonaToFreebase;

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
import java.util.TreeMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class FrequentCorrelatedPropertiesGenerator 
{
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	ArrayList<ArrayList<Integer>> masterPropsData = new ArrayList<ArrayList<Integer>>();
	HashMap<Integer, ItemsetCount> oneItemCandidateSet = new HashMap<Integer, ItemsetCount>();
	TreeMap<Integer, ItemsetCount> oneItemFrequentSet = new TreeMap<Integer, ItemsetCount>();
	TreeMap<Integer, HashMap<String, ItemsetCount>> allLengthFinalFrequentItemset = new TreeMap<Integer, HashMap<String, ItemsetCount>>();
	TreeMap<Integer, ArrayList<Itemset>> masterCandidateItemset = new TreeMap<Integer, ArrayList<Itemset>>();
	TreeMap<Integer, ArrayList<Itemset>> masterFrequentItemset = new TreeMap<Integer, ArrayList<Itemset>>();
	int support = 0;
	int oneItemSupport = 0;
	float totalNumberOfPropSet = 0;
	int longestPropList = 0;
	
	public FrequentCorrelatedPropertiesGenerator(Config conf)
	{
		this.conf = conf;
	}
	
	public FrequentCorrelatedPropertiesGenerator()
	{
		
	}
	
	public void freeglobals() {
		masterPropsData = null;
		oneItemCandidateSet = null;
		oneItemFrequentSet = null;
		allLengthFinalFrequentItemset = null;
		masterCandidateItemset = null;
		masterFrequentItemset = null;	
	}
	
	public void generateFrequentCorrelatedProperties(boolean isProcessingDatagraph) {
		if(isProcessingDatagraph)
			System.out.println("all processing will be done for DATA GRAPH");
		else
			System.out.println("all processing will be done for WORKLOAD");
		oneItemSupport = Integer.parseInt(conf.getProp(PropertyKeys.oneItemPropsSetThreshod));
		support = Integer.parseInt(conf.getProp(PropertyKeys.frequentPropsSetThreshod));
		if(isProcessingDatagraph) {
			String mergedPropsFile = conf.getInputFilePath(PropertyKeys.datagraphMergedPropertiesFile);
			loadAllProps(mergedPropsFile, isProcessingDatagraph);
		}
		else {
			String mergedPropsFile = conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetMergedPropertiesFile);
			loadAllProps(mergedPropsFile, isProcessingDatagraph);
		}
		logger.info("Done loading data. will start finding frequent itemsets now!");
		System.out.println("Done loading data. will start finding frequent itemsets now!");
		generateOneItemFrequentSet();
		generateTwoItemCandidateSet();
		logger.info("Done generating 2-itemset candidates");
		System.out.println("Done generating 2-itemset candidates");
		generateFrequentSet(2);
		logger.info("Done finding 2 item frequent set");
		System.out.println("Done finding 2 item frequent set");
		logger.info("The longest property list is " + longestPropList + "\n");
		System.out.println("The longest property list is " + longestPropList + "\n");
		for(int i=3; i<longestPropList; i++) {
			logger.info("Begin finding " + i + " item frequent set");
			System.out.println("Begin finding " + i + " item frequent set");
			
			boolean newCandsExist = generateMultipleItemCandidateSet(i);
			if(!newCandsExist)
				break;
			generateFrequentSet(i);
			
			logger.info("Done finding " + i + " item frequent set\n");
			System.out.println("Done finding " + i + " item frequent set\n");
		}
		logger.info("Printing to file!");
		System.out.println("Printing to file!");
		printToFile(isProcessingDatagraph);
	//	printMappedPropertiesToFile();
	}
	
	public void printMappedPropertiesToFile()
	{
		try
		{
			HashMap<String, String> propertyValue = readPropertyMapFile();
			FileWriter fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.frequentPropertiesMappedToValueFile));
			BufferedWriter bw = new BufferedWriter(fw);
			FileReader fr = new FileReader(conf.getOutputFilePath(PropertyKeys.frequentPropertiesFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			TreeMap<Integer, ArrayList<MutableString>> countProps = new TreeMap<Integer, ArrayList<MutableString>>();
			while((line = br.readLine()) != null)
			{
				String[] splitline = line.split(BarcelonaToFreebaseConstants.interPropertyDelimiter);
				MutableString str = new MutableString();
				for(int i=0; i<splitline.length-2; i++)
				{
					if(propertyValue.containsKey(splitline[i]))
					{
						str.append(propertyValue.get(splitline[i])).append(BarcelonaToFreebaseConstants.interPropertyDelimiter);
					}
				}
				ArrayList<MutableString> props;
				str.append(splitline[splitline.length-2]).append(BarcelonaToFreebaseConstants.interPropertyDelimiter).append(splitline[splitline.length-1]).append("\n");
				if(countProps.containsKey(Integer.parseInt(splitline[splitline.length-2])))
				{
					props = countProps.get(Integer.parseInt(splitline[splitline.length-2]));
					props.add(str);
				}
				else
				{
					props = new ArrayList<MutableString>();
					props.add(str);
					countProps.put(Integer.parseInt(splitline[splitline.length-2]), props);
				}
				//str.append(splitline[splitline.length-1]).append("\n");
				//bw.write(str.toString());
			}
			Iterator<Integer> iter = countProps.descendingKeySet().iterator();
			while(iter.hasNext())
			{
				int cnt = iter.next();
				ArrayList<MutableString> propsList = countProps.get(cnt);
				for(MutableString s : propsList)
				{
					//s.append(cnt).append("\n");
					bw.write(s.toString());
				}
			}
			br.close();
			bw.close();
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
	
	private HashMap<String, String> readPropertyMapFile()
	{
		HashMap<String, String> propertyValue = new HashMap<String, String>();
		try
		{
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.propertiesMapFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null)
			{
				String[] splitline = line.split(BarcelonaToFreebaseConstants.propValueDelimiter);
				propertyValue.put(splitline[0].trim(), splitline[1].trim());
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
	
	private void printToFile(boolean isProcessingDatagraph) {
		try {
			FileWriter fw = null;
			BufferedWriter bw = null;
			if(isProcessingDatagraph) {
				fw =new FileWriter(conf.getOutputFilePath(PropertyKeys.datagraphFrequentPropertiesFile));
				bw = new BufferedWriter(fw);
			} else {
				fw =new FileWriter(conf.getOutputFilePath(PropertyKeys.frequentPropertiesFile));
				bw = new BufferedWriter(fw);
			}
			
			// print one item frequent set details first.
			Iterator<Integer> it = oneItemFrequentSet.keySet().iterator();
			while(it.hasNext()) {
				int prop = it.next();
				ItemsetCount itc = oneItemFrequentSet.get(prop);
				MutableString ms = new MutableString();
				ms = ms.append(prop).append(",");
				ms = ms.append(itc.totalCount).append(",").append(itc.specificCount).append("\n");
				bw.write(ms.toString());
			}
			
			// now print the 2 or greater order frequent itemsets details.
			Iterator<Integer> iter = allLengthFinalFrequentItemset.keySet().iterator();
			while(iter.hasNext()) {
				HashMap<String, ItemsetCount> freqProps = allLengthFinalFrequentItemset.get(iter.next());
				Iterator<String> iter1 = freqProps.keySet().iterator();
				while(iter1.hasNext()) {
					String prop = iter1.next();
					bw.write(prop + freqProps.get(prop).totalCount + "," + freqProps.get(prop).specificCount + "," + "\n");
				}
			}
			/*Iterator<Integer> iter = masterFrequentItemset.keySet().iterator();
			while(iter.hasNext())
			{
				ArrayList<Itemset> freqProps = masterFrequentItemset.get(iter.next());
				
				for(int i=0; i<freqProps.size(); i++)
				{
					MutableString props = null;
					for(int j=0; j<freqProps.get(i).itemset.size(); j++)
					{
						if(props == null)
							props = new MutableString();
						props.append(freqProps.get(i).itemset.get(j)).append(",");
					}
					if(props != null)
					{
						props.append(freqProps.get(i).count).append("\n");
						bw.write(props.toString());
					}
				}
			}*/
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean generateMultipleItemCandidateSet(int index) {
		boolean newCandsExist = false;
		// use frequent itemset of "index-1" entries to generate candidate itemsets of "index" length.
		if(masterFrequentItemset.containsKey(index-1)) {
			ArrayList<Itemset> prevFrequentSet = masterFrequentItemset.get(index-1);
			ArrayList<Itemset> newCandidiateItemset = new ArrayList<Itemset>();
			for(int i=0; i<prevFrequentSet.size()-1; i++) {
				Itemset it = prevFrequentSet.get(i);
				for(int j=i+1; j<prevFrequentSet.size(); j++) {
					Itemset merged = mergeItemsets(it.itemset, prevFrequentSet.get(j).itemset, index);
					if(merged != null) {
						newCandidiateItemset.add(merged);
						newCandsExist =  true;
					}
				}
			}
			masterCandidateItemset.put(index, newCandidiateItemset);
		}
		return newCandsExist;
	}
	
	private MutableString[] getSourceItemsStringVersion(ArrayList<Integer> props)
	{
		MutableString[] bothItems = new MutableString[3];
		MutableString propStr = new MutableString();
		int i = 0;
		for(i=0; i<props.size()-2; i++)
		{
			propStr.append(props.get(i)).append(",");
		}
		bothItems[0] = new MutableString(propStr);
		bothItems[1] = new MutableString(propStr);
		bothItems[1].append(props.get(i)).append(",");
		i++;
		bothItems[2] = new MutableString(propStr);
		bothItems[2].append(props.get(i)).append(",");
		return bothItems;
	}
	
	private String getStringVersion(ArrayList<Integer> props)
	{
		MutableString propStr = new MutableString();
		for(int i=0; i<props.size(); i++)
		{
			propStr.append(props.get(i)).append(",");
		}
		return propStr.toString();
	}
	
	private Itemset mergeItemsets(ArrayList<Integer> one, ArrayList<Integer> two, int numOfProps) {
		Itemset it = null;
		ArrayList<Integer> newPropList = new ArrayList<Integer>(numOfProps);
		boolean match = true;
		int i = 0;
		for(i=0; i<one.size()-1; i++) {
			if(one.get(i).intValue() != two.get(i).intValue()) {
				match = false;
				break;
			}
			newPropList.add(one.get(i));
		}
		if(match && one.get(i).intValue() != two.get(i).intValue()) {
			if(one.get(i).intValue() < two.get(i).intValue()) {
				newPropList.add(one.get(i));
				newPropList.add(two.get(i));
			} else {
				newPropList.add(two.get(i));
				newPropList.add(one.get(i));
			}
			it = new Itemset();
			it.itemset = newPropList;
			it.count = 0;
		}
		return it;
	}
	
	private void generateTwoItemCandidateSet() {
		// generate two item candidates using oneitem candidates and add it to allLengthCandidateItemset
		TreeMap<Integer, ItemsetCount> copySet = oneItemFrequentSet;
		Iterator<Integer> iter = oneItemFrequentSet.keySet().iterator();
		ArrayList<Itemset> twoItemSets = new ArrayList<Itemset>();
		while(iter.hasNext()) {
			int prop = iter.next();
			Iterator<Integer> iter1 = copySet.keySet().iterator();
			while(iter1.hasNext()) {
				int prop1 = iter1.next();
				if(prop1 <= prop)
					continue;
				ArrayList<Integer> twoItemset = new ArrayList<Integer>(2);
				twoItemset.add(prop);
				twoItemset.add(prop1);
				Itemset it = new Itemset();
				it.count = 0;
				it.itemset = twoItemset;
				twoItemSets.add(it);
			}
		}
		masterCandidateItemset.put(2, twoItemSets);
	}
	
	private void generateFrequentSet(int index) {
		ArrayList<Itemset> candidateSets = masterCandidateItemset.get(index);
	//	int requiredCount = (int) (totalNumberOfPropSet*support);
		// update the count for each of the candidate sets..
		ArrayList<Itemset> frequentItems = new ArrayList<Itemset>(candidateSets.size());
		HashMap<String, ItemsetCount> strProps = new HashMap<String, ItemsetCount>();
		for(int i=0; i<candidateSets.size(); i++) {
			Itemset it = candidateSets.get(i);
			int specificCount = 0;
			for(int j=0; j<masterPropsData.size(); j++) {
				ArrayList<Integer> dataEntry = masterPropsData.get(j);
				/*if(it.itemset.size() == 2)
				{
					if(it.itemset.get(0) == 462 && it.itemset.get(1) == 4674)
						System.out.println("hi");
				}*/
				// compare the props in it.itemset and dataEntry.
				if(dataEntry.size() < it.itemset.size())
					continue;
				if(isSubset(it.itemset, dataEntry)) {
					it.count++;
					if(dataEntry.size() == it.itemset.size()) {
						// this is an instance of exact occurrence of the itemset in data-set.
						specificCount++;
					}
				}
			}
			if(it.count >= support) {
				ItemsetCount itc = new ItemsetCount();
				itc.specificCount = specificCount;
				itc.totalCount = it.count;
				frequentItems.add(it);
				String s = getStringVersion(it.itemset);
				strProps.put(s, itc);
			}
			//else if(it.count > 0){
				// delete the itemsets that were used to generate the new one from the mutablestring list of all itemsets.
				/*if(index > 2)
				{
					// not planning to delete the smaller frequent items used to create the new one. instead reducing its count.
					HashMap<String, ItemsetCount> allIndexLenProps = allLengthFinalFrequentItemset.get(index-1);
					MutableString[] sourceFrequentItems = getSourceItemsStringVersion(it.itemset);
					if(allIndexLenProps.containsKey(sourceFrequentItems[1].toString()))
						allIndexLenProps.get(sourceFrequentItems[1].toString()).specificCount -= it.count;
					if(allIndexLenProps.containsKey(sourceFrequentItems[2].toString()))
						allIndexLenProps.get(sourceFrequentItems[2].toString()).specificCount -= it.count;
					allIndexLenProps.get(sourceFrequentItems[1].toString()).specificCount -= it.count;
					allIndexLenProps.get(sourceFrequentItems[2].toString()).specificCount -= it.count;
					
					// Now add back some count:
					 EXAMPLE:
					 * If abc, abd, abe gave rise to abcd and abce, which then gave rise to abcde.
					 * we would have decremented count of 'abc' more than required, that is abcd+abce+abcde;
					 * not undo that buy adding back the count of abcde into abc.
					 
					if(allLengthFinalFrequentItemset.containsKey(index-2)){
						allLengthFinalFrequentItemset.get(index-2).get(sourceFrequentItems[0].toString()).specificCount += it.count;
					}
					else if(index == 3){
						
						 * This is similar to the above if. Its just that one item frequent set is maintained in a different DS and we are
						 * accessing that increasing its count directly.
						 
						oneItemFrequentSet.get(it.itemset.get(0)).specificCount += it.count;
					}
					//allIndexLenProps.remove(sourceFrequentItems[0].toString());
					//allIndexLenProps.remove(sourceFrequentItems[1].toString());
				}
				else if(index == 2){
					
					 * This is similar to the above if. Its just that one item frequent set is maintained in a different DS and we are
					 * accessing that reducing its count directly.
					 
					oneItemFrequentSet.get(it.itemset.get(0)).specificCount -= it.count;
					oneItemFrequentSet.get(it.itemset.get(1)).specificCount -= it.count;
				}*/
			//}
			
		}
		if(!frequentItems.isEmpty()) {
			allLengthFinalFrequentItemset.put(index, strProps);
			masterFrequentItemset.put(index, frequentItems);
		}
		/*// update frequent item sets for "index" based on support of the candidate sets..
		ArrayList<Itemset> frequentItems = new ArrayList<Itemset>();
		HashMap<String, Integer> strProps = new HashMap<String, Integer>();
		for(int i=0; i<candidateSets.size(); i++)
		{
			if(candidateSets.get(i).count/totalNumberOfPropSet > support)
			{
				strProps.put(getStringVersion(candidateSets.get(i).itemset), candidateSets.get(i).count);
				frequentItems.add(candidateSets.get(i));
			}
		}
		if(!frequentItems.isEmpty())
		{
			allLengthFinalFrequentItemset.put(index, strProps);
			masterFrequentItemset.put(index, frequentItems);
		}*/
	}
	
	private boolean isSubset(ArrayList<Integer> candEntry, ArrayList<Integer> dataEntry) {
		boolean issubset = true;
		int j=0;
		for(int i=0; i<candEntry.size() && issubset; i++) { 
			int k = 0;
			for(k=j; k<dataEntry.size(); k++) {
				if(dataEntry.get(k).intValue() == candEntry.get(i).intValue()) {
					j = k + 1;
					i++;
				}
				else if((dataEntry.size()-k) < (candEntry.size() - i)) {
					issubset = false;
					break;
				}
				if(i == candEntry.size())
					break;
			}
			if(k == dataEntry.size())
				issubset = false;
		}
		return issubset;
	}
	
	private void generateOneItemFrequentSet() {
		Iterator<Integer> iter = oneItemCandidateSet.keySet().iterator();
		while(iter.hasNext()) {
			int prop = iter.next();
			ItemsetCount canditc = oneItemCandidateSet.get(prop);
			if(canditc.totalCount >= oneItemSupport) {
				ItemsetCount itc = new ItemsetCount();
				itc.specificCount = canditc.specificCount;
				itc.totalCount = canditc.totalCount;
				oneItemFrequentSet.put(prop, itc);
			}
		}
	}
	
	private void loadAllProps(String mergedPropsFilePath, boolean isProcessingDatagraph) {
		try {
			FileReader fr = new FileReader(mergedPropsFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			int lnum = 0;
			while((line = br.readLine()) != null) {
				if(line.isEmpty()) {
					lnum++;
					continue;
				}
				//System.out.println(lnum + " -> " + line);
				totalNumberOfPropSet++;
				String[] props = line.split(BarcelonaToFreebaseConstants.interPropertyDelimiter);
				ArrayList<Integer> sortedProps = new ArrayList<Integer>(props.length);
				int specificCount = 0;
				if(props.length == 1)
					specificCount++;
				for(int i=0; i<props.length; i++) {
					int prop = Integer.parseInt(props[i]);
					sortedProps.add(prop);
				//	int cnt = 1;
					ItemsetCount itc;
					if(oneItemCandidateSet.containsKey(prop)) {
						itc = oneItemCandidateSet.get(prop);
						itc.totalCount++;
						itc.specificCount += specificCount;
					} else {
						itc = new ItemsetCount();
						itc.totalCount = 1;
						itc.specificCount += specificCount;
						oneItemCandidateSet.put(prop, itc);
					}
				}
				if(isProcessingDatagraph)
					Collections.sort(sortedProps);
				masterPropsData.add(sortedProps);
				if(sortedProps.size() > longestPropList)
					longestPropList = sortedProps.size();
			}
			System.out.println("number of lines ignored: " + lnum);
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		FrequentCorrelatedPropertiesGenerator fcpg = new FrequentCorrelatedPropertiesGenerator();
		if(args.length < 1)
		{
			System.out.println("Need an input properties file! Exiting program...");
			fcpg.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else
		{
			try
			{
				fcpg.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce)
			{
				System.out.println("Error in properties file configuration! Exiting program...");
				fcpg.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe)
			{
				System.out.println("IO exception while reading the properties file! Exiting program...");
				fcpg.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		fcpg.generateFrequentCorrelatedProperties(false);
	}
}

class Itemset
{
	ArrayList<Integer> itemset;
	int count;
}

class ItemsetCount
{
	// stores the number of times the itemset appears in the dataset, including as a subset.
	int totalCount = 0;
	// stores the number of times the specified item occurs without any superset.
	int specificCount = 0;
}