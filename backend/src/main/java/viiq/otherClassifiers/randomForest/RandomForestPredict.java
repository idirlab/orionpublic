package viiq.otherClassifiers.randomForest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;

public class RandomForestPredict {
	Config conf = null;
	RandomForestHelper rfh;
	
	public RandomForestPredict(Config conf) {
		this.conf = conf;
		rfh = new RandomForestHelper(conf.getOutputFilePath(PropertyKeys.randomForestTreesOutputFolder), conf.getOutputFilePath(PropertyKeys.randomForestTempFolder));
	}
	public RandomForestPredict() {
		
	}
	
	static RandomTree[] all_trees = null;
	static FastVector allAttributes = null;
	static HashMap<Integer, Integer> features_index = null;
	static int num_classifiers = 0;

	private void readAllAttributes() throws FileNotFoundException, IOException, ClassNotFoundException {
		String allAttributesFileName = rfh.getTempOutputFileName(conf.getProp(PropertyKeys.rfAllAttributesFile));
		ObjectInputStream ois = new ObjectInputStream(
				new FileInputStream(allAttributesFileName));
		allAttributes = (FastVector) ois.readObject();
		ois.close();
	}

	@SuppressWarnings("unchecked")
	private void readFeaturesIndex() throws FileNotFoundException, IOException, ClassNotFoundException {
		String featuresIndexFileName = rfh.getTempOutputFileName(conf.getProp(PropertyKeys.rfFeaturesIndexFile));
		ObjectInputStream ois_features_index = new ObjectInputStream(
				new FileInputStream(featuresIndexFileName));
		features_index = (HashMap<Integer, Integer>) ois_features_index.readObject();
		ois_features_index.close();
	}

	@SuppressWarnings("unchecked")
	private void readNumClasses() throws FileNotFoundException, IOException, ClassNotFoundException {
		String numClassifiersFileName = rfh.getTempOutputFileName(conf.getProp(PropertyKeys.rfNumClassifiersFile));
		ObjectInputStream ois_features_index = new ObjectInputStream(
				new FileInputStream(numClassifiersFileName));
		num_classifiers = (Integer) ois_features_index.readObject();
		ois_features_index.close();
	}

	private RandomTree readClassifier(int num) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(
				new FileInputStream(rfh.getTreeOutputFileName(num)));
		RandomTree rt = (RandomTree) ois.readObject();
		ois.close();
		return rt;
	}

	private RandomTree[] readClassifiers() throws FileNotFoundException, IOException, ClassNotFoundException{
		all_trees = new RandomTree[num_classifiers];
		for (int i=0; i<num_classifiers; i++){
			all_trees[i] = readClassifier(i);
		}
		return all_trees;
	}

	private ArrayList<Integer> classifyData(ArrayList<Integer> data, ArrayList<String> classes) throws Exception {
		SparseInstance test_row = new SparseInstance(allAttributes.size());
		for (int i=0; i<data.size(); i++) {
			int node = data.get(i);
			int feature_index = features_index.get(node);
			test_row.setValue((Attribute)allAttributes.elementAt(feature_index), "True");
		}
		Instances testSet = new Instances("Test", allAttributes, 1);
		int num_attributes = testSet.numAttributes();
		testSet.setClassIndex(num_attributes - 1);
		testSet.add(test_row);

		double[] res = null;
		for (int cl=0; cl< num_classifiers; cl++) {
			double[] _res = all_trees[cl].distributionForInstance(testSet.instance(0));
			if (res == null) {
				res = _res;
			} else {
				for (int res_i=0; res_i<_res.length; res_i++){
					res[res_i] = _res[res_i] + res[res_i];
				}
			}
		}

		for (int i = 0; i < classes.size() - 1; i++) {
			int index = i;
			for (int j = i + 1; j < classes.size(); j++) {
				int _t1 = Integer.parseInt(classes.get(j).substring(1, classes.get(j).length()));
				int _t2 = Integer.parseInt(classes.get(index).substring(1, classes.get(index).length()));
				int c1_index = features_index.get(_t1);
				int c2_index = features_index.get(_t2);

				if (res[c1_index] > res[c2_index])
					index = j;
			}
			String output = classes.get(index);
	//		int biggerNumber = Integer.parseInt(output.substring(1, output.length())); 
			classes.set(index, classes.get(i));
			classes.set(i, output);
		}
		ArrayList<Integer> rankedEdges = new ArrayList<Integer>();
		for(String edge : classes) {
			rankedEdges.add(Integer.parseInt(edge.substring(1, edge.length())));
		}
		return rankedEdges;
	}

	private void loadData() throws FileNotFoundException, IOException, ClassNotFoundException {
		readNumClasses();
		readFeaturesIndex();
		readAllAttributes();
		readClassifiers();
	}

	public int findBestEdge(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		int bestEdge = -1;
		ArrayList<String> candidates = new ArrayList<String>();
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext())
			candidates.add(iter.next()+"");

		ArrayList<Integer> rankedEdges = null;
		try {
			rankedEdges = classifyData(history, candidates);
		} catch(Exception e) {
			e.printStackTrace();
		}

		if(rankedEdges != null && !rankedEdges.isEmpty())
			return rankedEdges.get(0);
		return bestEdge;
	}

	public ArrayList<Integer> rankCandidateEdges(ArrayList<Integer> history, HashSet<Integer> candidateEdges) {
		ArrayList<String> candidates = new ArrayList<String>();
		Iterator<Integer> iter = candidateEdges.iterator();
		while(iter.hasNext())
			candidates.add(iter.next()+"");

		ArrayList<Integer> rankedEdges = null;
		try {
			rankedEdges = classifyData(history, candidates);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rankedEdges;
	}

	public void learnModel() {
		// code to read all decision trees and prepare the model for random forest.
		try {
			loadData();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		//loading required data
		RandomForestPredict rfp = new RandomForestPredict();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
//			gqs.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				rfp.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
//				gqs.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
//				gqs.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		rfp.rfh = new RandomForestHelper(rfp.conf.getOutputFilePath(PropertyKeys.randomForestTreesOutputFolder), 
				rfp.conf.getOutputFilePath(PropertyKeys.randomForestTempFolder));
		rfp.loadData();

		//preparing test data
		int[] temp_data = new int[]{-20409,-20182,-20181,-20082,27869,27874,28181,31732,30724,30720,30275,30723,-30141};
		int[] temp_classes = new int[]{27869,27874,28181,31732,30724,30720,30275,30723,27388,27392,27399,27407,30002,27386,30648,30129};
		ArrayList<Integer> temp_in_data = new ArrayList<Integer>();
		ArrayList<String> temp_in_classes = new ArrayList<String>();
		for (int i = 0; i<temp_data.length; i++)
			temp_in_data.add(temp_data[i]);
		for (int i = 0; i<temp_classes.length; i++)
			temp_in_classes.add("C"+temp_classes[i]);
		for (int i = 0; i<temp_in_classes.size(); i++)
			System.out.println(temp_in_classes.get(i));

		//calling ranking function
		ArrayList<Integer> output_order = rfp.classifyData(temp_in_data, temp_in_classes);

		//printing result
		for (int i = 0; i<output_order.size(); i++)
			System.out.println(output_order.get(i));
	}
}