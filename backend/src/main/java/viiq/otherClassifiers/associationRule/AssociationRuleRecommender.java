package viiq.otherClassifiers.associationRule;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRules;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRule;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.lang.Math;

public class AssociationRuleRecommender {
    AssocRules associationRules = null;
	ArrayList<Rule> rulesList = null;
    int databaseSize = 0;
    
    public AssociationRuleRecommender() {
    }
    public void learnModel() {
        int maxConsequentLength = 1;
		int maxAntecedentLength = 40;

		// STEP 1: Applying the FP-GROWTH algorithm to find frequent itemsets
		double minsupp = 0.4;
		AlgoFPGrowth fpgrowth = new AlgoFPGrowth();
		fpgrowth.setMaximumPatternLength(maxAntecedentLength + maxConsequentLength);
		Itemsets patterns = null;
		try {
			patterns = fpgrowth.runAlgorithm("/mounts/[server_name]/data/orion/data_all/output/trainingDataFreebase_3_50_700-NOconcat-newProp_itemset", null, minsupp);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		databaseSize = fpgrowth.getDatabaseSize();

		System.out.println(patterns.getItemsetsCount()+" frequent itemsets generated! now finding association rules ...");
		
		// STEP 2: Generating all rules from the set of frequent itemsets (based on Agrawal & Srikant, 94)
		double  minconf = 0.01;

		AlgoAgrawalFaster94 algoAgrawal = new AlgoAgrawalFaster94();
		algoAgrawal.setMaxConsequentLength(maxConsequentLength);
		algoAgrawal.setMaxAntecedentLength(maxAntecedentLength);
		try {
			associationRules = algoAgrawal.runAlgorithm(patterns, null, databaseSize, minconf);
			associationRules.sortByConfidence();
			String result = associationRules.toString(databaseSize);
			rulesList = stringToRules(result);
			System.out.println(rulesList.size()+" association rules generated!");
			// for(Rule rule: rulesList) {
			// 	System.out.println(rule.a.toString()+" => "+rule.c.toString());
			// }

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
    }

	private ArrayList<Rule> stringToRules(String str) {
		String[] lines = str.split("\n");
		ArrayList<Rule> rules = new ArrayList<Rule>();
		for(String line : lines) {
			if(!line.startsWith("   rule")) continue;
			String[] antecedent = line.split("==>")[0].split(":")[1].trim().split("\\s+");
			String[] consequent = line.split("==>")[1].split("support")[0].trim().split("\\s+");
			String[] temp = line.trim().split("\\s+");
			Rule rule = new Rule();
			for(String e : antecedent) {
				rule.a.add(Integer.parseInt(e));
			}
			for(String e : consequent) {
				rule.c.add(Integer.parseInt(e));
			}
			rule.conf = Double.parseDouble(temp[temp.length-1]);
			//System.out.println(rule.conf);
			rules.add(rule);
		}
		return rules;
	}

    public int findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		// int k = 5; //pick top-k rules that satisfy the condition. for top=1, this is the algorithm from http://proceedings.mlr.press/v19/rudin11a/rudin11a.pdf
		// HashMap<Integer, ArrayList<Double>> edgeScore = new HashMap<Integer, ArrayList<Double>>();
        // for(Rule rule : rulesList) {
        //     boolean notFound = false;
        //     for(int e : rule.a) {
        //         if(!history.contains(e)) notFound = true;
        //     }
        //     if(!candidateEdges.contains(rule.c.get(0))) notFound = true;
        //     if(!notFound) {
		// 		if(!edgeScore.containsKey(rule.c.get(0))) {
		// 			ArrayList<Double> firstScore = new ArrayList<Double>();
		// 			firstScore.add(rule.conf);
		// 			edgeScore.put(rule.c.get(0), firstScore);
		// 		} else if(edgeScore.get(rule.c.get(0)).size() < k) {
		// 			edgeScore.get(rule.c.get(0)).add(rule.conf);
		// 		}
        //     }
        // }
		// int maxEdge = -1;
		// double maxScore = -1.0;

		// for(Map.Entry<Integer, ArrayList<Double>> set : edgeScore.entrySet()) {
		// 	ArrayList<Double> scores = set.getValue();
		// 	double totalScore = 0;
		// 	for(double s : scores) {
		// 		totalScore += s;
		// 	}
		// 	if(totalScore > maxScore) {
		// 		maxScore = totalScore;
		// 		maxEdge = set.getKey();
		// 	}
		// }

		int N = 50; //number of random subsets
		HashMap<Integer, Double> edgeScore = new HashMap<Integer, Double>();
		//System.out.println("history = "+history);
		while(N > 0) {
			ArrayList<Integer> historyRandomSubset = new ArrayList<Integer>();
			for(int i = 0; i < history.size(); i++) {
				if(Math.random() < 0.5) historyRandomSubset.add(history.get(i));
			}
			//System.out.println("historyRandomSubset = "+historyRandomSubset);
			for(Rule rule : rulesList) {
				boolean notFound = false;
				for(int e : rule.a) {
					if(!historyRandomSubset.contains(e)) notFound = true;
				}
				if(!candidateEdges.contains(rule.c.get(0))) notFound = true;
				if(!notFound) {
					edgeScore.put(rule.c.get(0), edgeScore.getOrDefault(rule.c.get(0), 0.0) + rule.conf);
				}
			}

			N--;
		}

		int maxEdge = -1;
		double maxScore = -1.0;

		for(Map.Entry<Integer, Double> set : edgeScore.entrySet()) {
			if(set.getValue() > maxScore) {
				maxScore = set.getValue();
				maxEdge = set.getKey();
			}
		}
        return maxEdge;
	}
}

class Rule {
	ArrayList<Integer> a = new ArrayList<Integer>();
	ArrayList<Integer> c = new ArrayList<Integer>();
	double conf = 0.0;
}