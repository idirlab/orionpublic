package viiq.prepareTrainingData;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import viiq.graphQuerySuggestionMain.Config;

public class CreateEdgeExamplePairs {
	Config conf = null;
	
	public static void main(String[] args) {
		CreateEdgeExamplePairs cep = new CreateEdgeExamplePairs();
		if(args.length < 1)	{
			System.out.println("Need an input properties file! Exiting program...");
			return;
		}
		else{
			try	{
				cep.conf = new Config(args[0]);
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
		//cep.createEdgesForTypes();
		System.out.println("Done!");
	}
}
