package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

public class CleanFreebaseDatagraph {
	Config conf = null;
	// list of both entities and intermediate nodes.
	HashSet<Integer> entitiesIntermediateNodes = new HashSet<Integer>();
	
	private void keepOnlyEntityAndIntermediateTuples() {
		System.out.println("start!");
		readIntermediateNodes();
		System.out.println("done reading intermediate nodes");
		readEntitiesList();
		System.out.println("done reading entities list");
		rewriteDatagraphFile();
		System.out.println("done!");
	}
	
	private void rewriteDatagraphFile() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.datagraphFile));
			BufferedReader br = new BufferedReader(fr);
			FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.datagraphFileOnlyEntityIntermediate));
			BufferedWriter bw = new BufferedWriter(fw);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] split = line.split(",");
				if(entitiesIntermediateNodes.contains(Integer.parseInt(split[1].trim())) && entitiesIntermediateNodes.contains(Integer.parseInt(split[3].trim()))) {
					// keep only those entries which are either entities or intermediate nodes. if something is not found in our
					// intermediate nodes list and entities list (with labels), ignore them.
					bw.write(line+"\n");
				}
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void readEntitiesList() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.entityLangEn));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			int lnum = 0;
			while((line = br.readLine()) != null) {
				lnum++;
				String[] split = line.split(",");
				try {
					entitiesIntermediateNodes.add(Integer.parseInt(split[0].trim()));
				} catch(NumberFormatException nfe) {
					System.out.println(lnum + " -> " + line);
				}
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void readIntermediateNodes() {
		try {
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.intermediateNodesFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				entitiesIntermediateNodes.add(Integer.parseInt(line.trim()));
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		CleanFreebaseDatagraph cfd = new CleanFreebaseDatagraph();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				cfd.conf = new Config(args[0]);
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
		cfd.keepOnlyEntityAndIntermediateTuples();
	}
}
