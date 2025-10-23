package viiq.TestWikipedia;

import viiq.graphQuerySuggestionMain.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class CheckIntermediateProperties {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	ArrayList<String> lhsLines = new ArrayList<String>();
	ArrayList<String> rhsLines = new ArrayList<String>();
	ArrayList<String> lhsrhsLines = new ArrayList<String>();
	ArrayList<String> noneLines = new ArrayList<String>();
	
	public CheckIntermediateProperties() {
		
	}
	public CheckIntermediateProperties(Config conf) {
		this.conf = conf;
	}
	
	public void checkIntermediateEdgesDetails() {
		System.out.println("Starting ");
		try {
			HashSet<Integer> correlatedProps = new HashSet<Integer>();
			//FileReader fr2 = new FileReader(conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetMergedPropertiesFile));
			FileReader fr2 = new FileReader(conf.getOutputFilePath(PropertyKeys.frequentPropertiesFile));
			BufferedReader br2 = new BufferedReader(fr2);
			readCorrelatedEdges(br2, correlatedProps);
			
			FileReader fr1 = new FileReader(conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseNewConcatenatedPropertiesFile));
			BufferedReader br1 = new BufferedReader(fr1);
			readConcatenatedEdgeList(br1, correlatedProps);
			
			printLinesToFile(lhsLines, "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/output/inter/lhslines");
			printLinesToFile(rhsLines, "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/output/inter/rhslines");
			printLinesToFile(lhsrhsLines, "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/output/inter/lhsrhslines");
			printLinesToFile(noneLines, "/mounts/[server_name]/proj/nj/graphQuerySuggestionProject/graphQuerySuggestion/source_code/GraphQuerySuggestion/data/output/inter/nonelines");
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("End!");
	}
	
	private void printLinesToFile(ArrayList<String> list, String filePath) {
		try {
			FileWriter fw = new FileWriter(filePath);
			BufferedWriter bw = new BufferedWriter(fw);
			for(String line : list) {
				bw.write(line + "\n");
			}
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
	
	private void readCorrelatedEdges(BufferedReader br, HashSet<Integer> correlatedProps) throws IOException {
		String line = null;
		while((line = br.readLine()) != null) {
			String[] props = line.split(",");
			for(int i=0; i<props.length-2; i++) {
				correlatedProps.add(Integer.parseInt(props[i]));
			}
		}
		br.close();
	}
	
	private void readConcatenatedEdgeList(BufferedReader br, HashSet<Integer> correlatedProps) throws IOException {
		String line = null;
		while((line = br.readLine()) != null) {
			String[] splitline = line.split("\t");
			String[] lhsprops = splitline[0].split(",");
			boolean containslhs = false;
			boolean containsrhs = false;
			if(correlatedProps.contains(Integer.parseInt(lhsprops[0])) || correlatedProps.contains(Integer.parseInt(lhsprops[1])))
				containslhs = true;
			if(correlatedProps.contains(Integer.parseInt(splitline[1])))
				containsrhs = true;
			
			if(containslhs && containsrhs)
				lhsrhsLines.add(line);
			else if(!containslhs && !containsrhs)
				noneLines.add(line);
			else if(containslhs && !containsrhs)
				lhsLines.add(line);
			else
				rhsLines.add(line);
		}
		br.close();
	}
	
	public static void main(String[] args) {
		CheckIntermediateProperties cip = new CheckIntermediateProperties();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			cip.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				cip.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				cip.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				cip.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		cip.checkIntermediateEdgesDetails();
	}
}
