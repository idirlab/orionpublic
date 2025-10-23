package viiq.otherClassifiers.naiveBayesian;

import viiq.graphQuerySuggestionMain.Config;
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
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class PreparePriorProbabilities {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	/* DS to hold the prior probabilities: a, b, c, d, cnt1, cnt2
	 * count(a AND d) = <d, <a, (cnt1,cnt2)>>
	 * the hashmap used: 
	 * key1 = the last positive edge added (d)
	 * key2 = the previous edge added (positive or negative -> a/b/c)
	 * value = the two counts. not decided which one to use yet, so maintaining both = count(a AND d)
	 */
	HashMap<Long, HashMap<Long, Counts>> condCounts = new HashMap<Long, HashMap<Long,Counts>>();
	HashMap<Long, Counts> totalCounts = new HashMap<Long, Counts>();
	int totalFullCnt = 0;
	int totalSpecificCnt = 0;
	
	public static void main(String[] args){
		PreparePriorProbabilities ppp = new PreparePriorProbabilities();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			ppp.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				ppp.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				ppp.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ppp.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		String trainingDataFile = ppp.conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile);
		//ppp.computePriorProbabilitiesForDuplicateTrainData(trainingDataFile);
		//ppp.printPriorProbabilities();
		ppp.computePriorProbabilities(trainingDataFile);
		ppp.printPriorCounts();
		System.out.println("Done generating counts for bayesian classifier");
	}
	
	/**
	 * This method computes the probabilities when the input training data does not have duplicates. So this iterates thru positive edges
	 * in each line accordingly. In duplicated training data, the counts will get exaggerated. So I am not sure if it will even be correct.
	 * @param trainingDataFilePath
	 */
	private void computePriorProbabilities(String trainingDataFilePath) {
		try{
			FileReader fr = new FileReader(trainingDataFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				HashSet<Long> seenProps = new HashSet<Long>();
				String[] propsCount = line.split(",");
				int newEdgeIndex = propsCount.length - 2 - 1;
				
				int tcnt = Integer.parseInt(propsCount[propsCount.length - 2]);
				int scnt = Integer.parseInt(propsCount[propsCount.length - 1]);
				totalFullCnt += tcnt;
				totalSpecificCnt += scnt;
				
				ArrayList<Long> negEdges = new ArrayList<Long>();
				ArrayList<Long> posEdges = new ArrayList<Long>();
				if(propsCount.length > 3) {
					// this means there is more than one property in this training instance. note that the individual properties
					// are anyway taken care of with the else statement.
					for(int i=0; i<= newEdgeIndex; i++) {
						long prop = Long.parseLong(propsCount[i]);
						if(prop < 0) {
							negEdges.add(prop);
						}
						else {
							posEdges.add(prop);
						}
						if(!seenProps.contains(prop)) {
							// first update the total counts.
							updateTotalCount(prop, scnt, tcnt);
						}
						else
							seenProps.add(prop);
					}
					for(int i=0; i<posEdges.size(); i++) {
						long newEdge = posEdges.get(i);
						HashMap<Long, Counts> condProbEdge;
						if(condCounts.containsKey(newEdge)) {
							condProbEdge = condCounts.get(newEdge);
						}
						else {
							condProbEdge = new HashMap<Long, Counts>();
						}
						for(int j=0; j<posEdges.size(); j++) {
							if(j != i) {
								long edge = posEdges.get(j);
								updateConditionalCount(tcnt, scnt, condProbEdge, edge);
							}
						}
						for(int j=0; j<negEdges.size(); j++) {
							long edge = negEdges.get(j);
							updateConditionalCount(tcnt, scnt, condProbEdge, edge);
						}
						condCounts.put(newEdge, condProbEdge);
					}
				}
				else {
					long newEdge = Long.parseLong(propsCount[newEdgeIndex]);
					updateTotalCount(newEdge, scnt, tcnt);
				}
			}
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private void updateConditionalCount(int tcnt, int scnt, HashMap<Long, Counts> condProbEdge, long edge) {
		Counts cnt;
		if(condProbEdge.containsKey(edge)) {
			cnt = condProbEdge.get(edge);
			cnt.specificCnt += scnt;
			cnt.totalCnt += tcnt;
		}
		else {
			cnt = new Counts();
			cnt.specificCnt = scnt;
			cnt.totalCnt = tcnt;
		}
		condProbEdge.put(edge, cnt);
	}
	
	private void updateTotalCount(long edge, int scnt, int tcnt) {
		Counts totalCnt;
		if(totalCounts.containsKey(edge)) {
			totalCnt = totalCounts.get(edge);
			totalCnt.specificCnt += scnt;
			totalCnt.totalCnt += tcnt;
		}
		else {
			totalCnt = new Counts();
			totalCnt.specificCnt = scnt;
			totalCnt.totalCnt = tcnt;
		}
		totalCounts.put(edge, totalCnt);
	}
	
	private void printPriorCounts() {
		try {
			FileWriter fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.bayesianPriorProbabilities));
			BufferedWriter bw = new BufferedWriter(fw);
			// first write the totalFullCount and specificFullCount as the first line.
			bw.write(totalFullCnt + "," + totalSpecificCnt + "\n");
			// then write individual probabilities, i.e., without any conditional stuff : p(e_1)
			Iterator<Long> it = totalCounts.keySet().iterator();
			while(it.hasNext()) {
				long e = it.next();
				MutableString s = new MutableString();
				s = s.append(e).append(",").append(totalCounts.get(e).totalCnt).append(",").append(totalFullCnt).append(",");
				s = s.append(totalCounts.get(e).specificCnt).append(",").append(totalSpecificCnt).append("\n");
				bw.write(s.toString());
			}
			// now write down the conditional probabilities : p(a|b)
			Iterator<Long> iter = condCounts.keySet().iterator();
			while(iter.hasNext()) {
				long edge = iter.next();
				int tDen = totalCounts.get(edge).totalCnt;
				int sDen = totalCounts.get(edge).specificCnt;
				HashMap<Long, Counts> pcount = condCounts.get(edge);
				Iterator<Long> iter1 = pcount.keySet().iterator();
				while(iter1.hasNext()) {
					long e = iter1.next();
					MutableString s = new MutableString();
					s = s.append(edge).append(",").append(e).append(",");
					s = s.append(pcount.get(e).totalCnt).append(",").append(tDen).append(",");
					s = s.append(pcount.get(e).specificCnt).append(",").append(sDen).append("\n");
					bw.write(s.toString());
				}
			}
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*private void printPriorProbabilities() {
		try {
			FileWriter fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.bayesianPriorProbabilities));
			BufferedWriter bw = new BufferedWriter(fw);
			// first write individual probabilities, i.e., without any conditional stuff : p(e_1)
			Iterator<Integer> it = totalCounts.keySet().iterator();
			while(it.hasNext()) {
				int e = it.next();
				double totalProb = totalCounts.get(e).totalCnt/totalFullCnt * 1.0;
				double specProb = totalCounts.get(e).specificCnt/totalSpecificCnt * 1.0;
				MutableString s = new MutableString();
				s = s.append(e).append(",").append(totalProb).append(",").append(specProb).append("\n");
				bw.write(s.toString());
			}
			// now write down the conditional probabilities : p(a|b)
			Iterator<Integer> iter = condCounts.keySet().iterator();
			while(iter.hasNext()) {
				int edge = iter.next();
				double tDen = totalCounts.get(edge).totalCnt * 1.0;
				double sDen = totalCounts.get(edge).specificCnt * 1.0;
				HashMap<Integer, Counts> pcount = condCounts.get(edge);
				Iterator<Integer> iter1 = pcount.keySet().iterator();
				while(iter1.hasNext()) {
					int e = iter1.next();
					double totalProb = pcount.get(e).totalCnt/tDen;
					double specProb = pcount.get(e).specificCnt/sDen;
					MutableString s = new MutableString();
					s = s.append(edge).append(",").append(e).append(",").append(totalProb).append(",").append(specProb).append("\n");
					bw.write(s.toString());
				}
			}
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
	
	/*private void computePriorProbabilitiesForDuplicateTrainData(String trainingDataFilePath) {
		try{
			FileReader fr = new FileReader(trainingDataFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null) {
				HashSet<Integer> seenProps = new HashSet<Integer>();
				String[] propsCount = line.split(",");
				int newEdgeIndex = propsCount.length - 2 - 1;
				int newEdge = Integer.parseInt(propsCount[newEdgeIndex]);
				HashMap<Integer, Counts> condProbEdge;
				if(condCounts.containsKey(newEdge)) {
					condProbEdge = condCounts.get(newEdge);
				}
				else {
					condProbEdge = new HashMap<Integer, Counts>();
				}
				int tcnt = Integer.parseInt(propsCount[propsCount.length - 2]);
				int scnt = Integer.parseInt(propsCount[propsCount.length - 1]);
				updateTotalCount(newEdge, scnt, tcnt);
				seenProps.add(newEdge);
				
				totalFullCnt += tcnt;
				totalSpecificCnt += scnt;
				
				for(int i=0; i<newEdgeIndex; i++) {
					int edge = Integer.parseInt(propsCount[i]);
					if(!seenProps.contains(edge)) {
						// first update the total counts.
						updateTotalCount(edge, scnt, tcnt);
					}
					// next update the conditional probability count.
					updateConditionalCount(tcnt, scnt, condProbEdge, edge);
					seenProps.add(edge);
				}
				condCounts.put(newEdge, condProbEdge);
			}
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
}

final class Counts {
	int totalCnt;
	int specificCnt;
}
