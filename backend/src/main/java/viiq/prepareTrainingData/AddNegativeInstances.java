package viiq.prepareTrainingData;

import viiq.graphQuerySuggestionMain.Config;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class AddNegativeInstances {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	boolean isFreebaseDataset = false;
	
	public AddNegativeInstances(Config conf) {
		this.conf = conf;
	}
	
	public AddNegativeInstances() {
		
	}
	
	public static void main(String[] args){
		AddNegativeInstances ani = new AddNegativeInstances();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			ani.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				ani.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				ani.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				ani.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		ani.addNegativeEdgesToLog(false);
	}
	
	public void addNegativeEdgesToLog(boolean isProcessingDataGraph) {
		//int datasetFlag = Integer.parseInt(conf.getProp(PropertyKeys.datasetFlag));
		// It actually looks like I don't have to process dbpedia and freebase as separate stuff. the same code might actually work!
		AddNegativeInstancesFreebase anif = new AddNegativeInstancesFreebase(conf);
		anif.addNegativeEdgesToLog(isProcessingDataGraph);
		/*if(datasetFlag == 0)
			isFreebaseDataset = true;
		if(isFreebaseDataset) {
			// The only difference between this method and the Dbpedia based method in the else is:
			// The properties are assumed to all start from 0. So there is no mapping and inverted index maintained here.
			AddNegativeInstancesFreebase anif = new AddNegativeInstancesFreebase(conf);
			anif.addNegativeEdgesToLog(isProcessingDataGraph);
		}
		else {
			// If all properties in Dbpedia are also starting from 0 correctly, we don't need this else at all. we can use the freebase
			// method. will have to do it and create a single class which processes for both dbpedia and freebase.
			AddNegativeInstancesDbpedia anid = new AddNegativeInstancesDbpedia(conf);
			anid.addNegativeEdgesToLog();
		}*/
	}
}
