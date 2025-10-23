package viiq.prepareTrainingData;

import viiq.graphQuerySuggestionMain.Config;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.barcelonaToFreebase.FrequentCorrelatedPropertiesGenerator;

public class DatagraphFrequentCorrelatedPropertiesGenerator {
	Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	
	public DatagraphFrequentCorrelatedPropertiesGenerator() {
		
	}
	public DatagraphFrequentCorrelatedPropertiesGenerator(Config conf) {
		this.conf = conf;
	}
	
	public static void main(String[] args) {
		DatagraphFrequentCorrelatedPropertiesGenerator dfcp = new DatagraphFrequentCorrelatedPropertiesGenerator();
		System.out.println("start finding the correlated props in the data graph");
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			dfcp.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				dfcp.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce){
				System.out.println("Error in properties file configuration! Exiting program...");
				dfcp.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe){
				System.out.println("IO exception while reading the properties file! Exiting program...");
				dfcp.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		FrequentCorrelatedPropertiesGenerator fcpg = new FrequentCorrelatedPropertiesGenerator(dfcp.conf);
		fcpg.generateFrequentCorrelatedProperties(true);
		System.out.println("Done generating correlated props in the data graph");
	}
}
