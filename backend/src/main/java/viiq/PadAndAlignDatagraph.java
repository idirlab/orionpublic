package viiq;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

public class PadAndAlignDatagraph {
	final Logger logger = Logger.getLogger(getClass());
	Config conf = null;
	
	public void generateSortedDatagraphFile(String inputFilepath) {
		int maxLength = 0;
		TreeMap<Integer, ArrayList<NodeEdgeTuple>> srcDatagraph = new TreeMap<Integer, ArrayList<NodeEdgeTuple>>();
		TreeMap<Integer, ArrayList<NodeEdgeTuple>> objDatagraph = new TreeMap<Integer, ArrayList<NodeEdgeTuple>>();
		try {
			FileReader fr = new FileReader(inputFilepath);
			BufferedReader br = new BufferedReader(fr);
			
			String line = null;
			while((line = br.readLine()) != null) {
				if(line.length() > maxLength)
					maxLength = line.length();
				String[] tuple = line.split(",");
				int tupleID = Integer.parseInt(tuple[0].trim());
				int src = Integer.parseInt(tuple[1].trim());
				int prop = Integer.parseInt(tuple[2].trim());
				int obj = Integer.parseInt(tuple[3].trim());
				addToGraph(srcDatagraph, tupleID, src, prop, obj, true);
				addToGraph(objDatagraph, tupleID, src, prop, obj, false);
			}
			System.out.println("done reading data graph");
			String srcOutputFilepath = conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile);
			FileWriter fw1 = new FileWriter(srcOutputFilepath);
			BufferedWriter bw1 = new BufferedWriter(fw1);
			writeToFile(srcDatagraph, maxLength, bw1);
			
			String objOutputFilepath = conf.getInputFilePath(PropertyKeys.datagraphObjectAlignedFile);
			FileWriter fw2 = new FileWriter(objOutputFilepath);
			BufferedWriter bw2 = new BufferedWriter(fw2);
			writeToFile(objDatagraph, maxLength, bw2);
			System.out.println(maxLength);
			br.close();
			bw1.close();
			bw2.close();
		} catch(IOException ioe) {
			
		} catch(Exception e) {
			
		}
	}
	
	private void addToGraph(TreeMap<Integer, ArrayList<NodeEdgeTuple>> datagraph, int tupleID, int src, int prop, int obj, boolean srcSort) {
		NodeEdgeTuple net = new NodeEdgeTuple();
		net.tupleID = tupleID;
		net.edge = prop;
		int vertex;
		if(srcSort) {
			net.node = obj;
			vertex = src;
		}
		else {
			net.node = src;
			vertex = obj;
		}
		if(datagraph.containsKey(vertex)) {
			datagraph.get(vertex).add(net);
		}
		else {
			ArrayList<NodeEdgeTuple> arr = new ArrayList<NodeEdgeTuple>();
			arr.add(net);
			datagraph.put(vertex, arr);
		}
	}
	
	private void writeToFile(TreeMap<Integer, ArrayList<NodeEdgeTuple>> datagraph, int maxLength, BufferedWriter bw) throws IOException {
		Iterator<Integer> iter = datagraph.keySet().iterator();
		while(iter.hasNext()) {
			int src = iter.next();
			ArrayList<NodeEdgeTuple> arr = datagraph.get(src);
			for(NodeEdgeTuple net : arr) {
				MutableString line = new MutableString();
				line = line.append(net.tupleID).append(",").append(src).append(",").append(net.edge).append(",").append(net.node);
				int len = line.length();
				if(len < maxLength) {
					while(maxLength-len > 0) {
						line = line.append(" ");
						len++;
					}
				}
				String l = line.append("\n").toString();
				bw.write(l);
			}
		}
	}

	public static void main(String[] args) {
		PadAndAlignDatagraph pad = new PadAndAlignDatagraph();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			pad.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				pad.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				pad.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				pad.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		//String inputFilepath = pad.conf.getInputFilePath(PropertyKeys.datagraphFile);
		String inputFilepath = pad.conf.getInputFilePath(PropertyKeys.datagraphFileOnlyEntityIntermediate);
		pad.generateSortedDatagraphFile(inputFilepath);
		System.out.println("Done!");
	}
}

class NodeEdgeTuple {
	int node;
	int edge;
	int tupleID;
}