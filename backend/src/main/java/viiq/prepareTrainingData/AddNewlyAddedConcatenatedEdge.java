package viiq.prepareTrainingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

/**
 * - This class takes in newly created concatenated edges list (along with its edge ID mapping), and get the type information for the two
 * ends of the new edge. It will also create necessary files, which will have to be merged with existing files containing info regarding
 * the original list of edges.
 * - This will also add a new edge label (concatenated) for the new edge formed.
 * @author nj
 *
 */
public class AddNewlyAddedConcatenatedEdge {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	// key=edge originally from freebase, value = they types of the edge
	HashMap<Integer, EdgeEndType> originalEdgeTypeInfo = new HashMap<Integer, EdgeEndType>();
	// key=edge originally from freebase, value = label of the edge
	HashMap<Integer, String> originalEdgeLabelInfo = new HashMap<Integer, String>();
	
	// key=new edge ID, value = ends of the new edge
	HashMap<Integer, EdgeEndType> newEdgeTypeInfo = new HashMap<Integer, EdgeEndType>();
	// key=new edge ID, value = concatenated label of the edges that form this new edge.
	HashMap<Integer, String> newEdgeLabelInfo = new HashMap<Integer, String>();
	
	HashSet<Integer> intermediateProperties = new HashSet<Integer>();
	
	public AddNewlyAddedConcatenatedEdge() {
	}
	
	public AddNewlyAddedConcatenatedEdge(Config conf) {
		this.conf = conf;
	}
	
	public static void main(String[] args) {
		AddNewlyAddedConcatenatedEdge anace = new AddNewlyAddedConcatenatedEdge();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			anace.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				anace.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				anace.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				anace.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		anace.runNewConcatenatedEdges();
	}
	
	public void runNewConcatenatedEdges() {
		readOriginalEdgeLabelList();
		readOriginalEdgeTypeList();
		populateNewEdgeInfo();
		printNewEdgeType();
		printNewEdgeLabel();
	}
	
	private void populateNewEdgeInfo() {
		try {
			FileReader fr = new FileReader(conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseNewConcatenatedPropertiesFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] edgeMap = line.split("\t");
				String[] origEdges = edgeMap[0].split(",");
				int oldEdge1 = Integer.parseInt(origEdges[0]);
				int oldEdge2 = Integer.parseInt(origEdges[1]);
				intermediateProperties.add(oldEdge1);
				intermediateProperties.add(oldEdge2);
				int newEdgeId = Integer.parseInt(edgeMap[1]);
				String newLabel = originalEdgeLabelInfo.get(oldEdge1) + "-" + originalEdgeLabelInfo.get(oldEdge2);
				newEdgeLabelInfo.put(newEdgeId, newLabel);
				
				EdgeEndType eet1 = originalEdgeTypeInfo.get(oldEdge1);
				EdgeEndType eet2 = originalEdgeTypeInfo.get(oldEdge2);
				EdgeEndType neweet = new EdgeEndType();
				
				if(eet1 == null || eet2 == null) {
					System.out.println("ERROR edge : " + oldEdge1 + " , " + oldEdge2);
					continue;
				}
				
				if(eet1.srcType == eet2.srcType || eet1.srcType == eet2.destType) {
					// eet.srcType is an intermediate node's type.
					neweet.srcType = eet1.destType;
					if(eet2.srcType == eet1.srcType)
						neweet.destType = eet2.destType;
					else
						neweet.destType = eet2.srcType;
				}
				else {
					// ett1.srcType is NOT an intermediate node.
					neweet.srcType = eet1.srcType;
					if(eet2.srcType == eet1.destType)
						neweet.destType = eet2.destType;
					else
						neweet.destType = eet2.srcType;
				}
				newEdgeTypeInfo.put(newEdgeId, neweet);
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
	}
	
	private void readOriginalEdgeLabelList() {
		try {
			//FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.propertiesMapFile));
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.originalPropertiesMapFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] proplabel = line.split("\t");
				originalEdgeLabelInfo.put(Integer.parseInt(proplabel[0]), proplabel[1].trim());
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
	}
	
	private void readOriginalEdgeTypeList() {
		try {
			//FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.edgeTypeFile));
			FileReader fr = new FileReader(conf.getInputFilePath(PropertyKeys.originalEdgeTypeFile));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				String[] edgetype = line.split(",");
				EdgeEndType eet = new EdgeEndType();
				eet.srcType = Integer.parseInt(edgetype[0]);
				int prop = Integer.parseInt(edgetype[1]);
				eet.destType = Integer.parseInt(edgetype[2]);
				originalEdgeTypeInfo.put(prop, eet);
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
	}
	
	private void printNewEdgeType() {
		try {
			//FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.newConcatenatedEdgeTypeFile));
			FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.edgeTypeFile));
			BufferedWriter bw = new BufferedWriter(fw);
			writeEdgeType(bw, originalEdgeTypeInfo);
			writeEdgeType(bw, newEdgeTypeInfo);
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
	private void writeEdgeType(BufferedWriter bw, HashMap<Integer, EdgeEndType> hm) throws IOException {
		Iterator<Integer> iter = hm.keySet().iterator();
		while(iter.hasNext()) {
			int newEdgeId = iter.next();
			if(!intermediateProperties.contains(newEdgeId)) {
				EdgeEndType eet = hm.get(newEdgeId);
				bw.write(eet.srcType + "," + newEdgeId + "," + eet.destType + "\n");
			}
		}
	}
	
	private void printNewEdgeLabel() {
		try {
			//FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.newConcatenatedPropertiesMapFile));
			FileWriter fw = new FileWriter(conf.getInputFilePath(PropertyKeys.propertiesMapFile));
			BufferedWriter bw = new BufferedWriter(fw);
			FileWriter fw1 = new FileWriter(conf.getInputFilePath(PropertyKeys.distinctPropertiesList));
			BufferedWriter bw1 = new BufferedWriter(fw1);
			writeLabel(bw, bw1, originalEdgeLabelInfo);
			writeLabel(bw, bw1, newEdgeLabelInfo);
			bw.close();
			bw1.close();
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
	private void writeLabel(BufferedWriter bw, BufferedWriter bw1, HashMap<Integer, String> hm) throws IOException {
		Iterator<Integer> iter = hm.keySet().iterator();
		while(iter.hasNext()) {
			int newEdgeId = iter.next();
			if(!intermediateProperties.contains(newEdgeId))
				bw.write(newEdgeId + "\t" + hm.get(newEdgeId) + "\n");
				bw1.write(newEdgeId + "\n");
		}
	}
}

final class EdgeEndType {
	int srcType;
	int destType;
}
