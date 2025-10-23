/**
 * This file is to generate the candidate edges given an edge for DBpedia. This is different from what we do in Freebase since that has
 * a much better concept of types while DBpedia's types are very granular. So for DBpedia, I am instead finding the candidates for a given
 * edge, instead of a node (type node in Freebase). We will look at all the edges that are incident on source and object nodes of a particular
 * edge and call them the corresponding source/ojbect edge candidates, given the original edge.
 */
package viiq.prepareTrainingData;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

public class CreateTypeToEdgesMappingListDBpedia {
	
	class DestinationNode {
		int dest;
		int prop;
	}
	
	Config conf = null;
	HashMap<Integer, ArrayList<DestinationNode>> srcDatagraph = new HashMap<Integer, ArrayList<DestinationNode>>();
	HashMap<Integer, ArrayList<DestinationNode>> objDatagraph = new HashMap<Integer, ArrayList<DestinationNode>>();
	// key = edge, value = set of edges that can be incident on the SOURCE node of the "key".
	HashMap<Integer, HashSet<Integer>> srcEdgeList = new HashMap<Integer, HashSet<Integer>>();
	// key = edge, value = set of edges that can be incident on the OBJECT node of the "key".
	HashMap<Integer, HashSet<Integer>> objEdgeList = new HashMap<Integer, HashSet<Integer>>();
	
	public void findCandidates() {
		System.out.println("Started");
		// read the dbpedia data graph.
		readDataGraph();
		System.out.println("Done loading graph");
		Iterator<Integer> iter = srcDatagraph.keySet().iterator();
		while(iter.hasNext()) {
			int node = iter.next();
			ArrayList<DestinationNode> neighbors = srcDatagraph.get(node);
			HashSet<Integer> nodeIncidentEdges = new HashSet<Integer>();
			getNeighborEdges(node, nodeIncidentEdges);
			for(DestinationNode dn : neighbors) {
				HashSet<Integer> srcCands = new HashSet<Integer>();
				if(srcEdgeList.containsKey(dn.prop)) 
					srcCands = srcEdgeList.get(dn.prop);
				HashSet<Integer> objCands = new HashSet<Integer>();
				if(objEdgeList.containsKey(dn.prop))
					objCands = objEdgeList.get(dn.prop);
				
				populateCandidates(dn.prop, node, srcCands, nodeIncidentEdges);
				populateCandidates(dn.prop, dn.dest, objCands, null);
				srcEdgeList.put(dn.prop, srcCands);
				objEdgeList.put(dn.prop, objCands);
			}
		}
		System.out.println("will write to files now");
		writeToFile();
		System.out.println("Done!");
	}
	
	private void readDataGraph() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(conf.getInputFilePath(PropertyKeys.datagraphFile)));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] spl = line.split(",");
				int src = Integer.parseInt(spl[1].trim());
				int prop = Integer.parseInt(spl[2].trim());
				int obj = Integer.parseInt(spl[3].trim());
				DestinationNode dn = new DestinationNode();
				dn.prop = prop;
				dn.dest = obj;
				if(srcDatagraph.containsKey(src))
					srcDatagraph.get(src).add(dn);
				else {
					ArrayList<DestinationNode> arr = new ArrayList<CreateTypeToEdgesMappingListDBpedia.DestinationNode>();
					arr.add(dn);
					srcDatagraph.put(src, arr);
				}
				
				DestinationNode revdn = new DestinationNode();
				revdn.prop = prop;
				revdn.dest = src;
				if(objDatagraph.containsKey(obj))
					objDatagraph.get(obj).add(revdn);
				else {
					ArrayList<DestinationNode> arr = new ArrayList<CreateTypeToEdgesMappingListDBpedia.DestinationNode>();
					arr.add(revdn);
					objDatagraph.put(obj, arr);
				}
			}
			br.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void populateCandidates(int edge, int node, HashSet<Integer> cands, HashSet<Integer> nodeIncidentEdges) {
		if(nodeIncidentEdges == null) {
			nodeIncidentEdges = new HashSet<Integer>();
			getNeighborEdges(node, nodeIncidentEdges);
		}
		Iterator<Integer> iter = nodeIncidentEdges.iterator();
		while(iter.hasNext()) {
			int cand = iter.next();
			if(cand != edge)
				cands.add(cand);
		}
	}
	
	private void getNeighborEdges(int node, HashSet<Integer> nodeIncidentEdges) {
		getEdges(node, nodeIncidentEdges, srcDatagraph.get(node), true);
		getEdges(node, nodeIncidentEdges, objDatagraph.get(node), false);
	}
	private void getEdges(int node, HashSet<Integer> nodeIncidentEdges, ArrayList<DestinationNode> neighbors, boolean isForward) {
		if(neighbors != null) {
			for(DestinationNode dn : neighbors) {
				if(isForward)
					nodeIncidentEdges.add(dn.prop);
				else
					nodeIncidentEdges.add(dn.prop*(-1));
			}
		}
	}
	
	private void writeToFile() {
		String srcFile = conf.getInputFilePath(PropertyKeys.typeEdgesListSource);
		writeToFile(srcFile, srcEdgeList);
		String objFile = conf.getInputFilePath(PropertyKeys.typeEdgesListObject);
		writeToFile(objFile, objEdgeList);
	}
	private void writeToFile(String filename, HashMap<Integer, HashSet<Integer>> edgeList) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			Iterator<Integer> iter = edgeList.keySet().iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				MutableString ms = new MutableString();
				ms.append(edge).append(":");
				HashSet<Integer> cands = edgeList.get(edge);
				if(cands == null || cands.isEmpty()) {
					System.out.println("No src/obj candidates for " + edge);
					continue;
				}
				Iterator<Integer> citer = cands.iterator();
				while(citer.hasNext()) {
					ms.append(citer.next()).append(",");
				}
				bw.write(ms.toString()+"\n");
			}
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		CreateTypeToEdgesMappingListDBpedia ctem = new CreateTypeToEdgesMappingListDBpedia();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				ctem.conf = new Config(args[0]);
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
		ctem.findCandidates();
	}
}
