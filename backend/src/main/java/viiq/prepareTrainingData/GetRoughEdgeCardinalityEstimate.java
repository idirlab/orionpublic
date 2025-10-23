package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

public class GetRoughEdgeCardinalityEstimate {
	Config conf = null;
	HashMap<Integer, Integer> edgeCard = new HashMap<Integer, Integer>();
	HashMap<Integer, String> concatEdges = new HashMap<Integer, String>();
	ArrayList<Integer> edgesToConsider = new ArrayList<Integer>();
	
	
	private void readConcatProps() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.concatenatedPropertiesMappingFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] splitstr = line.split("\t");
				concatEdges.put(Integer.parseInt(splitstr[1].trim()), splitstr[0].trim());
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void countEdgesFromDataGraph() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile)));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] splitstr = line.split(",");
				int prop = Integer.parseInt(splitstr[2].trim());
				int cnt = 1;
				if(edgeCard.containsKey(prop)) {
					cnt = edgeCard.get(prop);
					cnt++;
				}
				edgeCard.put(prop, cnt);
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void readPropertiesList() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.propertiesFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				edgesToConsider.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void createRoughEstimates() {
		readConcatProps();
		System.out.println("Done reading concat list");
		countEdgesFromDataGraph();
		System.out.println("done reading data graph");
		readPropertiesList();
		System.out.println("done reading edges to consider list");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(conf.getInputFilePath(PropertyKeys.propertiesFileCardinality)));
			for(int prop : edgesToConsider) {
				int cnt = 0;
				if(concatEdges.containsKey(prop)) {
					String concat = concatEdges.get(prop);
					String[] edges = concat.split(",");
					cnt = Math.min(edgeCard.get(Integer.parseInt(edges[0].trim())), edgeCard.get(Integer.parseInt(edges[1].trim())));
				} else {
					if(!edgeCard.containsKey(prop))
						System.out.println("Did not find this edge in graph: " + prop);
					else
						cnt = edgeCard.get(prop);
				}
				bw.write(prop + ":" + cnt + "\n");
			}
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("Done!");
	}
	
	public static void main(String[] args) {
		GetRoughEdgeCardinalityEstimate gr = new GetRoughEdgeCardinalityEstimate();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				gr.conf = new Config(args[0]);
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
		gr.createRoughEstimates();
		System.out.println("Done!");
	}
}
