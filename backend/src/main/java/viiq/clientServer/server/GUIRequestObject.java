package viiq.clientServer.server;

import java.util.ArrayList;

import viiq.commons.CandidateEdgeEnds;
import viiq.commons.GuiEdgeInfo;

public class GUIRequestObject {
	public ArrayList<GuiEdgeInfo> partialGraph = new ArrayList<GuiEdgeInfo>();
	public ArrayList<GuiEdgeInfo> rejectedGraph = new ArrayList<GuiEdgeInfo>();
	public CandidateEdgeEnds activeEdgeEnds = new CandidateEdgeEnds();
	// -1=active mode for naive GUI, 0=active mode for fancy Orion GUI, 1=passive, 2=click on orange node AND refresh suggestions
	public int mode;
	public int dataGraphInUse;
	// number of top k suggestions to send back.
	public int topk;
	// adding a new feature where we click on an orange node and hit Refresh button.
	// we are supposed to get new suggestions incident only on that selected node.
	public int refreshGraphNode;
	public String sessionId;
	public String systemName;
	public int noOfIteration;
}
