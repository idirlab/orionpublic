package viiq.prepareTrainingData;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class CreatePseudoPropertyFrequenItemset {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	// key = the property ID directly taken from the frequent itemset file (so it is a string instead of long).
	// value = the new pseudo ID given (Start from 0).
	HashMap<String, Integer> realToPseudoPropertyMapping = new HashMap<String, Integer>();
	
	public CreatePseudoPropertyFrequenItemset() {
		
	}
	
	public CreatePseudoPropertyFrequenItemset(Config conf) {
		this.conf = conf;
	}
	
	public static void main(String[] args) {
		CreatePseudoPropertyFrequenItemset cdpm = new CreatePseudoPropertyFrequenItemset();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			cdpm.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				cdpm.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				cdpm.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				cdpm.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		cdpm.createPseudoPropertyListAndFrequentItemset(false);
	//	cdpm.createPseudoPropertyListAndFrequentItemset(true);
	}
	
	public void createPseudoPropertyListAndFrequentItemset(boolean isProcessingDataGraph) {
		if(isProcessingDataGraph)
			System.out.println("all processing will be done for DATA GRAPH");
		else
			System.out.println("all processing will be done for WORKLOAD");
		createDistinctPropertyList();
		createPseudoFrequentItemsetFile(isProcessingDataGraph);
	}
	
	private void createPseudoFrequentItemsetFile(boolean isProcessingDataGraph) {
		// we now have the hashmap populated. Read the frequent itemset file and create a pseudo property based frequent itemset file.
		try{
			FileReader fr;
			BufferedReader br;
			FileWriter fw;
			BufferedWriter bw;
			if(isProcessingDataGraph) {
				fr = new FileReader(conf.getOutputFilePath(PropertyKeys.datagraphFrequentPropertiesFile));
				br = new BufferedReader(fr);
				fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.datagraphFrequentPseudoPropertiesFile));
				bw = new BufferedWriter(fw);
			}
			else {
				fr = new FileReader(conf.getOutputFilePath(PropertyKeys.frequentPropertiesFile));
				br = new BufferedReader(fr);
				fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.frequentPseudoPropertiesFile));
				bw = new BufferedWriter(fw);
			}
			
			
			String line;
			while((line = br.readLine()) != null) {
				String[] props = line.split(",");
				MutableString ms = new MutableString();
				for(int i=0; i<props.length-2; i++) {
					ms = ms.append(realToPseudoPropertyMapping.get(props[i])).append(",");
				}
				ms = ms.append(props[props.length-2]).append(",").append(props[props.length-1]).append("\n");
				bw.write(ms.toString());
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
	
	public void createDistinctPropertyList() {
		try{
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.distinctPropertiesList));
			BufferedReader br = new BufferedReader(fr);
			
			FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.distinctPropertiesListMapping));
			BufferedWriter bw = new BufferedWriter(fw);
			String line;
			int pseudoPropIndex = 1;
			// first write a dummy line. we want the pseudo indexes to start from 1 rather than 0.
			bw.write("0,0\n");
			while((line = br.readLine()) != null) {
				realToPseudoPropertyMapping.put(line.trim(), pseudoPropIndex);
				MutableString ms = new MutableString();
				ms = ms.append(line.trim()).append(",").append(pseudoPropIndex).append("\n");
				bw.write(ms.toString());
				pseudoPropIndex++;
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
}
