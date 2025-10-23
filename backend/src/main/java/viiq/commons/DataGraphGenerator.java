package viiq.commons;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import viiq.commons.EdgeTypeInfo;
import viiq.commons.ObjNodeIntProperty;

public class DataGraphGenerator {
	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public void loadDataGraph(String filePath, HashMap<Integer, ArrayList<ObjNode>> srcDataGraph, 
			HashMap<Integer, ArrayList<ObjNode>> objDataGraph)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					long prop = Long.parseLong(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraph(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
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
	
	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public void loadDataGraphIntProperty(String filePath, HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph, 
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraph(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
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
	
	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public void loadDataGraph(String filePath, HashMap<Integer, ArrayList<ObjNode>> srcDataGraph, 
			HashMap<Integer, ArrayList<ObjNode>> objDataGraph, HashMap<Long, EdgeTypeInfo> edgeType, 
			HashMap<Integer, HashSet<Integer>> nodeTypes)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					long prop = Long.parseLong(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraph(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
					if(edgeType.containsKey(prop)) {
						EdgeTypeInfo eti = edgeType.get(prop);
						// add the types associated with the source.
						if(nodeTypes.containsKey(src)) {
							HashSet<Integer> nt = nodeTypes.get(src);
							nt.add(eti.source_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.source_type);
							nodeTypes.put(src, nt);
						}
						// add the types associated with the object.
						if(nodeTypes.containsKey(dest)) {
							HashSet<Integer> nt = nodeTypes.get(dest);
							nt.add(eti.object_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.object_type);
							nodeTypes.put(dest, nt);
						}
					}
					else {
						//System.out.println("This property is not present in edge type info! -> " + prop);
					}
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
	
	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public int loadDataGraphIntProperty(String filePath, HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph, 
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph, HashMap<Integer, EdgeTypeInfo> edgeType, 
			HashMap<Integer, HashSet<Integer>> nodeTypes, boolean loadDataGraphToMemory)
	{
		int numOfLines = 0;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
					numOfLines++;
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					if(loadDataGraphToMemory)
						addEdgeToDataGraphIntProperty(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
					if(edgeType.containsKey(prop)) {
						EdgeTypeInfo eti = edgeType.get(prop);
						// add the types associated with the source.
						if(nodeTypes.containsKey(src)) {
							HashSet<Integer> nt = nodeTypes.get(src);
							nt.add(eti.source_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.source_type);
							nodeTypes.put(src, nt);
						}
						// add the types associated with the object.
						if(nodeTypes.containsKey(dest)) {
							HashSet<Integer> nt = nodeTypes.get(dest);
							nt.add(eti.object_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.object_type);
							nodeTypes.put(dest, nt);
						}
					}
					else {
						//System.out.println("This property is not present in edge type info! -> " + prop);
					}
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
		return numOfLines;
	}
	
	/**
	 * File containing the data dump:
	 * tupleId, source, property, object, src_obj_Count
	 * @param filePath
	 */
	public void loadDataGraphIntProperty(String filePath, HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph, 
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph, HashMap<Integer, EdgeTypeInfo> edgeType, 
			HashMap<Integer, HashSet<Integer>> nodeTypes)
	{
		//int numOfLines = 0;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			try
			{
				String line = null;
				while((line = br.readLine()) != null)
				{
				//	numOfLines++;
					String[] tokens = line.split(",");
					int tupleId = Integer.parseInt(tokens[0].trim());
					int src = Integer.parseInt(tokens[1].trim());
					int prop = Integer.parseInt(tokens[2].trim());
					int dest = Integer.parseInt(tokens[3].trim());
					addEdgeToDataGraphIntProperty(tupleId, src, prop, dest, srcDataGraph, objDataGraph);
					if(edgeType.containsKey(prop)) {
						EdgeTypeInfo eti = edgeType.get(prop);
						// add the types associated with the source.
						if(nodeTypes.containsKey(src)) {
							HashSet<Integer> nt = nodeTypes.get(src);
							nt.add(eti.source_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.source_type);
							nodeTypes.put(src, nt);
						}
						// add the types associated with the object.
						if(nodeTypes.containsKey(dest)) {
							HashSet<Integer> nt = nodeTypes.get(dest);
							nt.add(eti.object_type);
						}
						else {
							HashSet<Integer> nt = new HashSet<Integer>();
							nt.add(eti.object_type);
							nodeTypes.put(dest, nt);
						}
					}
					else {
						//System.out.println("This property is not present in edge type info! -> " + prop);
					}
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
		//return numOfLines;
	}
	
	/**
	 * For every line in the data graph file, an entry into data structures that are supposed to hold the data graph in memory are
	 * populated. An entry for src->obj is made, and its corresponding back entry is made to objDataGraph.
	 * @param tupleId
	 * @param src
	 * @param prop
	 * @param obj
	 */
	private void addEdgeToDataGraph(int tupleId, int src, long prop, int obj,
			HashMap<Integer, ArrayList<ObjNode>> srcDataGraph, HashMap<Integer, ArrayList<ObjNode>> objDataGraph)
	{
		ObjNode son = new ObjNode();
		son.tupleId = tupleId;
		son.dest = obj;
		son.prop = prop;
		if(srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNode> tuple = srcDataGraph.get(src);
			tuple.add(son);
		}
		else
		{
			ArrayList<ObjNode> tuple = new ArrayList<ObjNode>();
			tuple.add(son);
			srcDataGraph.put(src, tuple);
		}
		
		// Add this same edge in the opposite direction..
		ObjNode oon = new ObjNode();
		oon.tupleId = tupleId;
		oon.dest = src;
		oon.prop = prop;
		if(objDataGraph.containsKey(obj))
		{
			ArrayList<ObjNode> tuple = objDataGraph.get(obj);
			tuple.add(oon);
		}
		else
		{
			ArrayList<ObjNode> tuple = new ArrayList<ObjNode>();
			tuple.add(oon);
			objDataGraph.put(obj, tuple);
		}
	}
	
	/**
	 * For every line in the data graph file, an entry into data structures that are supposed to hold the data graph in memory are
	 * populated. An entry for src->obj is made, and its corresponding back entry is made to objDataGraph.
	 * @param tupleId
	 * @param src
	 * @param prop
	 * @param obj
	 */
	private void addEdgeToDataGraph(int tupleId, int src, int prop, int obj,
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph, HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph)
	{
		ObjNodeIntProperty son = new ObjNodeIntProperty();
		son.tupleId = tupleId;
		son.dest = obj;
		son.prop = prop;
		if(srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNodeIntProperty> tuple = srcDataGraph.get(src);
			tuple.add(son);
		}
		else
		{
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(son);
			srcDataGraph.put(src, tuple);
		}
		
		// Add this same edge in the opposite direction..
		ObjNodeIntProperty oon = new ObjNodeIntProperty();
		oon.tupleId = tupleId;
		oon.dest = src;
		oon.prop = prop;
		if(objDataGraph.containsKey(obj))
		{
			ArrayList<ObjNodeIntProperty> tuple = objDataGraph.get(obj);
			tuple.add(oon);
		}
		else
		{
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(oon);
			objDataGraph.put(obj, tuple);
		}
	}
	
	/**
	 * For every line in the data graph file, an entry into data structures that are supposed to hold the data graph in memory are
	 * populated. An entry for src->obj is made, and its corresponding back entry is made to objDataGraph.
	 * @param tupleId
	 * @param src
	 * @param prop
	 * @param obj
	 */
	private void addEdgeToDataGraphIntProperty(int tupleId, int src, int prop, int obj,
			HashMap<Integer, ArrayList<ObjNodeIntProperty>> srcDataGraph, HashMap<Integer, ArrayList<ObjNodeIntProperty>> objDataGraph)
	{
		ObjNodeIntProperty son = new ObjNodeIntProperty();
		son.tupleId = tupleId;
		son.dest = obj;
		son.prop = prop;
		if(srcDataGraph.containsKey(src))
		{
			ArrayList<ObjNodeIntProperty> tuple = srcDataGraph.get(src);
			tuple.add(son);
		}
		else
		{
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(son);
			srcDataGraph.put(src, tuple);
		}
		
		// Add this same edge in the opposite direction..
		ObjNodeIntProperty oon = new ObjNodeIntProperty();
		oon.tupleId = tupleId;
		oon.dest = src;
		oon.prop = prop;
		if(objDataGraph.containsKey(obj))
		{
			ArrayList<ObjNodeIntProperty> tuple = objDataGraph.get(obj);
			tuple.add(oon);
		}
		else
		{
			ArrayList<ObjNodeIntProperty> tuple = new ArrayList<ObjNodeIntProperty>();
			tuple.add(oon);
			objDataGraph.put(obj, tuple);
		}
	}
}
