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

public class RemoveTypeAndDomainNodesFromFreebaseDatagraph {

	Config conf = null;
	
	private HashSet<String> getTypesDomainsList() {
		HashSet<String> typesAndDomains = new HashSet<String>();
		String typesFile = conf.getInputFilePath(PropertyKeys.typesListFile);
		String domainsFile = conf.getInputFilePath(PropertyKeys.domainsListFile);
		readFromFile(typesFile, typesAndDomains);
		readFromFile(domainsFile, typesAndDomains);
		return typesAndDomains;
	}
	
	private void readFromFile(String filename, HashSet<String> typesAndDomains) {
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				if(!typesAndDomains.contains(line.trim()))
					typesAndDomains.add(line.trim());
			}
			br.close();
		} catch(IOException ioe) {
			
		} catch(Exception e) {
			
		}
	}
	
	public void removeTypeAndDomainNodes() {
		try {
			HashSet<String> typesAndDomains = getTypesDomainsList();
			String inputFilepath = conf.getInputFilePath(PropertyKeys.datagraphFile);
			FileReader fr = new FileReader(inputFilepath);
			BufferedReader br = new BufferedReader(fr);
			
			String srcOutputFilepath = conf.getInputFilePath(PropertyKeys.datagraphWithoutTypesDomainsFile);
			FileWriter fw = new FileWriter(srcOutputFilepath);
			BufferedWriter bw = new BufferedWriter(fw);
			
			String line = null;
			while((line = br.readLine()) != null) {
				String[] splitline = line.split(",");
				if(typesAndDomains.contains(splitline[1].trim()) || typesAndDomains.contains(splitline[3].trim())) {
					//System.out.println(line);
					continue;
				}
				bw.write(line + "\n");
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			
		} catch(Exception e) {
			
		}
	}
	
	public static void main(String[] args) {
		RemoveTypeAndDomainNodesFromFreebaseDatagraph rt = new RemoveTypeAndDomainNodesFromFreebaseDatagraph();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			//rt.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				rt.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				//rt.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				//rt.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		rt.removeTypeAndDomainNodes();
		//PadAndAlignDatagraph pad = new PadAndAlignDatagraph();
		//pad.generateSortedDatagraphFile(rt.conf.getInputFilePath(PropertyKeys.datagraphWithoutTypesDomainsFile));
		System.out.println("Done");
	}
}
