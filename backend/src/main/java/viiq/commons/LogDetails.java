package viiq.commons;

import java.util.HashSet;

public class LogDetails {
	private HashSet<Integer> edges;
	private int totalCount;
	private int specificCount;
	
	public HashSet<Integer> getEdges() {
		return edges;
	}
	public void setEdges(HashSet<Integer> edges) {
		this.edges = edges;
	}
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	public int getSpecificCount() {
		return specificCount;
	}
	public void setSpecificCount(int specificCount) {
		this.specificCount = specificCount;
	}
}
