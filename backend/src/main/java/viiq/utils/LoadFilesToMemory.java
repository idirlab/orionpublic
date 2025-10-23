package viiq.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.log4j.Logger;

public class LoadFilesToMemory {
	Config conf = null;
//	final Logger logger = Logger.getLogger(getClass());
	
	public LoadFilesToMemory() {
		
	}
	public LoadFilesToMemory(Config conf) {
		this.conf = conf;
	}
	
	public void loadIntermediateNodesList(String inputFilePath, HashSet<Integer> intermediateNodes) {
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
	
	public void loadConcatedPropertiesList(String inputFilePath, HashMap<String, Integer> concatenatedEdgesToNewEdgeIdMap) {
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
					concatenatedEdgesToNewEdgeIdMap.put(split[0].trim(), newPropID);
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
}
