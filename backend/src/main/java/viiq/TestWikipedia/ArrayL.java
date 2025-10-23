package viiq.TestWikipedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ArrayL {
	public static void main(String[] args){
		/*ArrayList<Integer> a = new ArrayList<Integer>(10);
		a.add(5, 0);
		for(int i=0; i<a.size(); i++)
			System.out.println(a.get(i));*/
		/*ArrayList<CandidateEdgeScore> rankedEdgesScore = new ArrayList<CandidateEdgeScore>();
		CandidateEdgeScore ce = new CandidateEdgeScore();
		ce.edge = 1;
		ce.score = 1.0;
		rankedEdgesScore.add(ce);
		CandidateEdgeScore ce1 = new CandidateEdgeScore();
		ce1.edge = 2;
		ce1.score = 3.5;
		rankedEdgesScore.add(ce1);
		CandidateEdgeScore ce2 = new CandidateEdgeScore();
		ce2.edge = 3;
		ce2.score = 3.0;
		rankedEdgesScore.add(ce2);
		for(CandidateEdgeScore c : rankedEdgesScore) {
			System.out.println(c.edge + " -> " + c.score);
		}
		
		Collections.sort(rankedEdgesScore, new Comparator<CandidateEdgeScore>(){
	
		  public int compare(CandidateEdgeScore o1, CandidateEdgeScore o2)
		  {
			  if(o1.score <= o2.score)
				  return 1;
			  else
				  return -1;
		  }
		});
		for(CandidateEdgeScore c : rankedEdgesScore) {
			System.out.println(c.edge + " -> " + c.score);
		}*/
		
		Pattern p = Pattern.compile("__\\d+>");
		String str = "<http://dbpedia.org/resource/Apollo_11> <http://dbpedia.org/ontology/soundRecording> <http://dbpedia.org/resource/Apollo_11__1>";
		String[] spl = str.split(" ");
		for(String s : spl) {
			Matcher m = p.matcher(s);
			if(m.find())
				System.out.println(s);
		}
		String q = "\"Andrei Tarkovsky\"@en";
		String delim = "\"";
		if(q.startsWith(delim))
				System.out.println(q);
		/*Matcher m = p.matcher(str);
		while (m.find()) {
		  System.out.println(m.group());
		}*/
		
		/*String abe = "1\t/abc/def/ghi";
		int lastIndex = abe.lastIndexOf("/");
		System.out.println(abe.substring(lastIndex+1, abe.length()));*/
	}
	
	private void sortchech() {
		ArrayList<CandidateEdgeScore> rankedEdgesScore = new ArrayList<CandidateEdgeScore>();
		CandidateEdgeScore ce = new CandidateEdgeScore();
		ce.edge = 1;
		ce.score = 5.0;
		rankedEdgesScore.add(ce);
		CandidateEdgeScore ce1 = new CandidateEdgeScore();
		ce1.edge = 2;
		ce1.score = 3.0;
		rankedEdgesScore.add(ce1);
		CandidateEdgeScore ce2 = new CandidateEdgeScore();
		ce2.edge = 3;
		ce2.score = 2.0;
		rankedEdgesScore.add(ce2);
		Collections.sort(rankedEdgesScore, new Comparator<CandidateEdgeScore>(){
	
		  public int compare(CandidateEdgeScore o1, CandidateEdgeScore o2)
		  {
			  if(o1.score <= o2.score)
				  return 1;
			  else
				  return -1;
		  }
		});
		for(CandidateEdgeScore c : rankedEdgesScore) {
			System.out.println(c.edge + " -> " + c.score);
		}
	}
}

final class CandidateEdgeScore {
	int edge;
	double score;
}
