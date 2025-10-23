package viiq.graphQuerySuggestionMain;

public class DestNode {
	private int dest;
	// the node ID used in the GUI. it is -1, if it was a grey node (or a node that is basically not in partial query graph, but is in
	// rejected graph only.
	private int destGraphNodeID;
	private boolean forwardEdge;
	
	public int getDest() {
		return dest;
	}

	public void setDest(int dest) {
		this.dest = dest;
	}

	public Integer getDestGraphNodeID() {
		return destGraphNodeID;
	}

	public void setDestGraphNodeID(Integer dest) {
		this.destGraphNodeID = dest;
	}

	public boolean isForwardEdge() {
		return forwardEdge;
	}

	public void setForwardEdge(boolean forwardEdge) {
		this.forwardEdge = forwardEdge;
	}

	public DestNode()
	{
		// Label of the destination node. If we decide to represent each node in the DB as integers, change the type of "dest" to int..
		destGraphNodeID = -1;
		dest = 0;
		forwardEdge = true;
	}

	public DestNode(DestNode dn)
	{
		destGraphNodeID = dn.getDestGraphNodeID();
		dest = dn.getDest();
		forwardEdge = dn.isForwardEdge();
	}
}
