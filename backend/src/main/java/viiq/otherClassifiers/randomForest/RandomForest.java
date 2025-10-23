/**
 * This class only learns various random trees and stores each model as a file. This does not do any classification.
 */
package viiq.otherClassifiers.randomForest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;

public class RandomForest {
	Config conf = null;
	Random randomNumberGenerator = null;
	RandomForestHelper rfh;
	//final Logger logger = Logger.getLogger(getClass());

	public RandomForest(Config conf) {
		this.conf = conf;
		randomNumberGenerator = new Random(System.currentTimeMillis());
	}

	public RandomForest() {
		randomNumberGenerator = new Random(System.currentTimeMillis());
	}

	private ArrayList<Integer> get_random(Integer req_size) {
		ArrayList<Integer> random_numbers = new ArrayList<Integer>();
		for (int i=0; i<req_size; i++) {
			random_numbers.add(randomNumberGenerator.nextInt(req_size));
		}
		return random_numbers;
	}
	
	private int generateFeatureIndexAndAllAttributes(FastVector allAttributes, HashMap<Integer, Integer> featuresIndex) {
		int count = 0;
		String userLogInputFile = conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile);
		int training_data_size = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(userLogInputFile));
			String line;

			while ((line = br.readLine()) != null) {
				training_data_size++;
				String[] nodesList = line.split(",");
				ArrayList<Integer> training_data_row = new ArrayList<Integer>();
				for (int i=0; i<nodesList.length-2; i++) {
					training_data_row.add(Integer.parseInt(nodesList[i]));
				}

				for (int j=0; j< training_data_row.size(); j++){
					int node = training_data_row.get(j);
					if (featuresIndex.get(node) == null){
						FastVector feature = new FastVector(2);
						feature.addElement("False");
						feature.addElement("True");
						Attribute att_feature = new Attribute(""+training_data_row.get(j), feature);
						allAttributes.addElement(att_feature);
						featuresIndex.put(node, count++);
					}
				}
			}
			br.close();

			FastVector class_label = new FastVector();
			Iterator it = featuresIndex.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				class_label.addElement("C"+pair.getKey());
			}
			Attribute att_class_label = new Attribute("class", class_label);
			allAttributes.addElement(att_class_label);

			// write to files. We don't have to write these everytime!
			saveDataFeatureIndex(featuresIndex);
			saveallAttributes(allAttributes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return training_data_size;
	}

	@SuppressWarnings({"unchecked", "deprecation", "rawtypes" })
	private RandomTree buildClassifier(FastVector allAttributes, HashMap<Integer, Integer> featuresIndex, int trainingDataSize) throws Exception {
		Instances trainingSet = null;
		RandomTree rt = null;
		String userLogInputFile = conf.getOutputFilePath(PropertyKeys.trainingDataWithIDFile);


		trainingSet = new Instances("Data", allAttributes, trainingDataSize);
		int num_attributes = trainingSet.numAttributes();
		trainingSet.setClassIndex(num_attributes - 1);
		int training_data_count = 0;
		ArrayList<Integer> random_numbers = get_random(trainingDataSize);

		BufferedReader br = new BufferedReader(new FileReader(userLogInputFile));
		String line;
		int line_num = 0;
		while ((line = br.readLine()) != null) {
			for (int num_rept=0; num_rept < Collections.frequency(random_numbers, line_num); num_rept++) {
				SparseInstance training_row = new SparseInstance(num_attributes);
				ArrayList<Integer> training_data_row = new ArrayList<Integer>();

				String[] nodesList = line.split(",");
				for (int i=0; i<nodesList.length-2; i++) {
					training_data_row.add(Integer.parseInt(nodesList[i]));
				}

				for (int j=0; j< training_data_row.size(); j++) {
					for (int k=0; k< training_data_row.size(); k++) {
						int node = training_data_row.get(k);
						int feature_index = featuresIndex.get(node);
						training_row.setValue((Attribute)allAttributes.elementAt(feature_index), "True");
					}
					try {
						training_row.setValue((Attribute)allAttributes.elementAt(j), "False");
					} catch (Exception e) {
						e.printStackTrace();
					}
					training_row.setValue((Attribute)allAttributes.elementAt(num_attributes-1), "C"+training_data_row.get(j));
					trainingSet.add(training_row);
					training_data_count++;
				}
			}
			line_num++;
		}
		rt = new RandomTree();
		String[] options = weka.core.Utils.splitOptions("-depth 50");

/*		for (int temp = 0; temp < options.length; temp++) {
			System.out.println(options[temp]);
		}*/
		rt.setOptions(options);
//		System.out.println("Done with manupilating data");
//		System.out.println("Total number of lines = " + training_data_count);
		/*      ArffSaver saver = new ArffSaver();
        saver.setInstances(trainingSet);
        saver.setFile(new File("./data/test.arff"));
        saver.setDestination(new File("./data/test.arff"));   // **not** necessary in 3.5.4 and later
        saver.writeBatch();*/
		rt.buildClassifier(trainingSet);
		return rt;
	}

	private void saveClassifier(RandomTree rt, int num) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(rfh.getTreeOutputFileName(num)));
		oos.writeObject(rt);
		oos.flush();
		oos.close();
	}

	/*private void saveDataIndexFeatures(ArrayList<Integer> index_features) throws FileNotFoundException, IOException, ClassNotFoundException {
		//saving index_features ArrayList
		FileOutputStream fos_index_features = new FileOutputStream("./data/index_features.out");
		ObjectOutputStream oos_index_features = new ObjectOutputStream(fos_index_features);
		oos_index_features.writeObject(index_features);
		oos_index_features.close();
		fos_index_features.close();
	}*/

	private void saveDataFeatureIndex(HashMap<Integer, Integer> features_index) throws FileNotFoundException, IOException, ClassNotFoundException {
		//saving features_index HashMap
		String featuresIndexFileName = rfh.getTempOutputFileName(conf.getProp(PropertyKeys.rfFeaturesIndexFile));
		FileOutputStream fos_features_index = new FileOutputStream(featuresIndexFileName);
		ObjectOutputStream oos_features_index = new ObjectOutputStream(fos_features_index);
		oos_features_index.writeObject(features_index);
		oos_features_index.close();
		fos_features_index.close();
	}

	private void saveallAttributes(FastVector allAttributes) throws FileNotFoundException, IOException, ClassNotFoundException {
		String allAttributesFileName = rfh.getTempOutputFileName(conf.getProp(PropertyKeys.rfAllAttributesFile));
		ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(allAttributesFileName));
		oos.writeObject(allAttributes);
		oos.flush();
		oos.close();
	}

	private void saveNumClassifiers(int num_classifiers) throws FileNotFoundException, IOException, ClassNotFoundException {
		String numClassifiersFileName = rfh.getTempOutputFileName(conf.getProp(PropertyKeys.rfNumClassifiersFile));
		ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(numClassifiersFileName));
		oos.writeObject(num_classifiers);
		oos.flush();
		oos.close();
	}

	private void buildClassifiers(int numOfClassifiers) throws FileNotFoundException, IOException, ClassNotFoundException {
		RandomTree rt = null;
		try {
			System.out.println("Calling build Classifier");
			FastVector allAttributes = new FastVector();
			HashMap<Integer, Integer> featuresIndex = new HashMap<Integer, Integer>();
			int trainingDataSize = generateFeatureIndexAndAllAttributes(allAttributes, featuresIndex);
			for(int i=0; i< numOfClassifiers; i++) {
				rt = buildClassifier(allAttributes, featuresIndex, trainingDataSize);
				saveClassifier(rt, i);
				rt = null;
				//System.gc();
			}
			System.out.println("Done with building the RF classifier");
		} catch (Exception e) {
			e.printStackTrace();
		}
		saveNumClassifiers(numOfClassifiers);
	}

	public static void main(String[] args) throws Exception {
		RandomForest rf = new RandomForest();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				rf.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		rf.rfh = new RandomForestHelper(rf.conf.getOutputFilePath(PropertyKeys.randomForestTreesOutputFolder), 
				rf.conf.getOutputFilePath(PropertyKeys.randomForestTempFolder));
		int numOfClassifiers = Integer.parseInt(rf.conf.getProp(PropertyKeys.numberOfTreesInRandomForest));
		rf.buildClassifiers(numOfClassifiers);
	}
}
