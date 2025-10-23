package viiq.commons;

public class GuiEdgeStringInfo {
	// this is the type/entity id for the node, which is the label displayed in the GUI.
	public String source;
	// this is the id used by the GUI for a node (this does NOT correspond to the node label).
	public int graphSource;
	public boolean isSourceType;
	public boolean isObjectType;
	public String edge;
	public String object;
	public int graphObject;
	// The actual types of the source and object of this particular edge.
	public int actualSourceType;
	public int actualObjectType;
	public double score;
}
