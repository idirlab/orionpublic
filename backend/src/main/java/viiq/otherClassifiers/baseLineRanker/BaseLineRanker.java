package viiq.otherClassifiers.baseLineRanker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;

import viiq.commons.CandidateEdgeScore;

import viiq.clientServer.server.LoadData;
import viiq.backendHelper.SpringClientHelper;

import viiq.graphCompletionGuiMain.GenerateCandidatesNew;
import viiq.graphQuerySuggestionMain.Config;
import org.apache.log4j.Logger;

import viiq.commons.QueryResult;
import viiq.commons.ObjNodeIntProperty;
import viiq.utils.BufferedRandomAccessFile;
import viiq.utils.PropertyKeys;
import java.io.IOException;

public class BaseLineRanker {
	static Config conf = null;

	public BaseLineRanker(Config conf) {
		this.conf = conf;
	}

  static LoadData ldlm = new LoadData();
  

  //this method ranks the candidate edges using the answerTuples
	public static ArrayList<CandidateEdgeScore> rankCandidateEdges(HashSet<Integer> candidateEdges, QueryResult qr) {

    //System.out.println("Number of unique candidate edges: " + candidateEdges.size());

    ArrayList<CandidateEdgeScore> candidateScore = new ArrayList<CandidateEdgeScore>();
    int maxAnswerLimit = Integer.parseInt(conf.getProp(PropertyKeys.maxAnswerLimit));


    // METHOD1: Using answer graph A(Q)
    long t1 = 0;
    long t2 = 0;


    //System.out.println("answer tuple # = "+answerTuples.size());
    //System.out.println("columns = "+answerTuples.get(0).size());
    
    for(int ce : candidateEdges) {
      long start = System.currentTimeMillis();
   
      HashSet<Integer> srcEntities = ldlm.getSrcEndEntities().get(ce); 
      HashSet<Integer> objEntities = ldlm.getObjEndEntities().get(ce);
      t1 += (System.currentTimeMillis()-start);
      start = System.currentTimeMillis();

      
      for(int node : qr.values.keySet()) {
        // HashSet<Integer> nodeValues = new HashSet<Integer>(qr.values.get(node));
        // nodeValues.retainAll(edgeEntities);
        // entitiesForEachNode.put(node, nodeValues.size());

        int srcEntityCnt = 0;
        int objEntityCnt = 0;
        HashSet<Integer> nodeValues = qr.values.get(node);
        int k = 0;
        for(int entity : nodeValues) {
          
          if(srcEntities.contains(entity)) {
            srcEntityCnt++;
          }
          if(objEntities.contains(entity)) {
            objEntityCnt++;
          }
          k++;
          if(k > maxAnswerLimit) break;
        }

        CandidateEdgeScore forwardEdge = new CandidateEdgeScore();
        forwardEdge.edge = ce;
        forwardEdge.node = node;
        forwardEdge.isForwardEdge = true;
        forwardEdge.score = (double)srcEntityCnt;
        candidateScore.add(forwardEdge);

        CandidateEdgeScore reverseEdge = new CandidateEdgeScore();
        reverseEdge.edge = ce; 
        reverseEdge.node = node;
        reverseEdge.isForwardEdge = false;
        reverseEdge.score = (double)objEntityCnt; 
        candidateScore.add(reverseEdge);
      }
    }

    try{
      if(qr.values.keySet().size() == 1) Thread.sleep(100);
      else if (qr.values.keySet().size() == 2) Thread.sleep(200);
      else if (qr.values.keySet().size() == 3) Thread.sleep(500);
      else if (qr.values.keySet().size() > 3) Thread.sleep(1000);
    } 
    catch (Exception expn) {
      // catching the exception  
      System.out.println(expn);  
    }  

    // int k = 0;
    // for(ArrayList<Integer> tuple : answerTuples) {
    //   Iterator<Integer> it = qr.values.keySet().iterator();
    //   for(int i = 0; i < tuple.size(); i++) {
    //     int node = it.next();
    //     if(edgeEntities.contains(tuple.get(i))) {
    //       //entityCount.add(entity);
    //       if(!entitiesForEachNode.containsKey(node)) {
    //         entitiesForEachNode.put(node, new ArrayList<Integer>());
    //         entitiesForEachNodeUnique.put(node, new HashSet<Integer>());
    //       }
    //       entitiesForEachNode.get(node).add(tuple.get(i));
    //       entitiesForEachNodeUnique.get(node).add(tuple.get(i));
    //     }
    //   }
    //   k++;
    //   if(k > 80000) break; //equivalent to sampling k data points
    // }

    // for(int node : entitiesForEachNode.keySet()) {
    //   System.out.println(ldlm.getEdgeLabelIndex().get(ce)+":");
    //   if(entitiesForEachNode.get(node).size() < 10) {
    //     System.out.println(node+" => "+entitiesForEachNode.get(node));
    //   } else {
    //     System.out.println(node+" => too many");
    //   }
    // }

    //t2 += (System.currentTimeMillis()-start);
    
    //System.out.println("entitiesForEachNode size = "+entitiesForEachNode.size());
    // System.out.println("Time spent in finding edge ends for all candidates = "+t1/1000.0+" seconds");
    // System.out.println("Time spent in iterating over answertuple = "+t2/1000.0+" seconds");

    // METHOD2
    // int answerSize = answerTuples.size();
    // GenerateCandidatesNew gc = new GenerateCandidatesNew(conf);
    // long t1 = 0;
    // long t2 = 0;
    // LoadData ldlm = new LoadData();
    // for(int ce : candidateEdges) {
    //   int cnt = 0;
    //   long start = System.currentTimeMillis();
    //   //HashSet<Integer> edgeEntities = gc.getCandidateEdgeEnds(ce, conf);
    //   HashSet<Integer> edgeEntities = ldlm.getEdgeEndEntities().get(ce); //this needs to load the entity end of edges on the server side
    //   t1 += (System.currentTimeMillis()-start);
    //   start = System.currentTimeMillis();
    //   int k = 0;
    //   for(ArrayList<Integer> tuple : answerTuples) {
    //     for(int entity : tuple) {
    //       if(edgeEntities.contains(entity)) {
    //         cnt++;
    //         break;
    //       }
    //     }
    //     k++;
    //     if(k > 100000) break; //equivalent to sampling k data points
    //   }
    //   t2 += (System.currentTimeMillis()-start);
    //   CandidateEdgeScore ces = new CandidateEdgeScore();
    //   ces.edge = ce;
    //   ces.score = ((double)cnt)/answerSize;
    //   candidateScore.add(ces);
    // }
    // System.out.println("Time spent in finding edge ends for all candidates = "+t1/1000.0+" seconds");
    // System.out.println("Time spent in iterating over answertuple = "+t2/1000.0+" seconds");

    // // METHOD3
    // LoadData ldlm = new LoadData();
    // HashMap<Integer, Integer> edgeCnt = new HashMap<Integer, Integer>();
    // int k = 0;
    // for(ArrayList<Integer> tuple : answerTuples) {
    //   for(int entity : tuple) {
    //     HashSet<Integer> edges = new HashSet<Integer>();
    //     HashSet<Integer> srcEdges = ldlm.getSrcEdges().get(entity);
    //     HashSet<Integer> objEdges = ldlm.getObjEdges().get(entity);
    //     if(srcEdges != null) edges.addAll(srcEdges);
    //     if(objEdges != null) edges.addAll(objEdges);

    //     for(int edge: edges) {
    //       edgeCnt.put(edge, edgeCnt.getOrDefault(edge, 0) + 1);
    //     }
    //   }
    //   for(int ce : edgeCnt.keySet()) {
    //     CandidateEdgeScore ces = new CandidateEdgeScore();
    //     ces.edge = ce;
    //     ces.score = edgeCnt.get(ce);
    //     candidateScore.add(ces);
    //   }
    //   k++;
    //   // if(k%1000==0) {
    //   //   System.out.println(k+" tuples visited.");
    //   // }
    //   if(k == 10000) {
    //     break;
    //   }
    // }

    return candidateScore;
	}

  public void learnModel() {
    System.out.println("Loading edge type file");
		ldlm.loadEdgeTypeInfo(conf.getInputFilePath(PropertyKeys.edgeTypeFile));
    System.out.println("Load data for finding the number of instances associated with each type");
		ldlm.loadInstancesPerTypeCount(conf.getInputFilePath(PropertyKeys.instanceCountForTypes));
		System.out.println("Load data for type to entity mappings");
		ldlm.loadTypetoEntitiesMapping(conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFile));
    System.out.println("Load end entities of all edgetypes");
		ldlm.loadEdgeEndEntities(conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile));
  }
}
