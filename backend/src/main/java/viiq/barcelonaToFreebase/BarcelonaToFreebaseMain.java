package viiq.barcelonaToFreebase;

import viiq.graphQuerySuggestionMain.Config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.prepareTrainingData.AddNegativeInstances;
import viiq.prepareTrainingData.AddNewlyAddedConcatenatedEdge;
import viiq.prepareTrainingData.CreatePseudoPropertyFrequenItemset;

import viiq.utils.PropertyKeys;

/**
 * @author nj
 * This package viiq.does a lot of stuff.
 * - It first reads the co-occurring entities found in Wikipedia (for whatever number of consecutive sentences).
 * - cwtf.convertWikiToFreebaseEntities() puts in the integer entity ID found in the corresponding dataset instead of the Wikipedia URL found
 * - dp.identifyProperties() identifies properties present in the dataset between entities found from wikipedia.
 * - mp.mergePropertiesFromMultipleFiles() merges properties from consecutive lines. All these merged properties are the co-occurring props.
 * - fcpg.generateFrequentCorrelatedProperties() generates frequently co-occurring properties, with their counts.
 * - ani.addNegativeEdgesToLog() adds negative edge instances to the user log - to simulate actual query sessions!
 */
public class BarcelonaToFreebaseMain 
{
	final Logger logger = Logger.getLogger(getClass());
	Config conf = null;
	public static void main(String[] args)
	{
		BarcelonaToFreebaseMain btf = new BarcelonaToFreebaseMain();
		if(args.length < 1)
		{
			System.out.println("Need an input properties file! Exiting program...");
			btf.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else
		{
			try
			{
				btf.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce)
			{
				System.out.println("Error in properties file configuration! Exiting program...");
				btf.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe)
			{
				System.out.println("IO exception while reading the properties file! Exiting program...");
				btf.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		boolean isFreebaseDataset = false;
		int datasetFlag = Integer.parseInt(btf.conf.getProp(PropertyKeys.datasetFlag));
		if(datasetFlag == 0)
			isFreebaseDataset = true;
		/**
		 * SUPER important boolean flag to set. Please set it according to what you are processing. If you are processing this for
		 * wikipedia based correlated properties list, set it to FALSE. 
		 * If you are processing this for the data graph based correlated properties, set it to TRUE;
		 */
		boolean isProcessingDataGraph = false;
		int isProcessingDataGraphFlag = Integer.parseInt(btf.conf.getProp(PropertyKeys.dataGraphBasedQueryLog));
		if(isProcessingDataGraphFlag == 1)
			isProcessingDataGraph = true;
		// Some of these steps are to be performed only if it is NOT done for the data graph (ie., if isProcessingDataGraph is false).
		/*if(!isProcessingDataGraph) {
			File barcelonaCorpusInputFolder = new File(btf.conf.getOutputFilePath(PropertyKeys.barcelonaCorpusParsedEntityListFolder));
			//ConvertWikiToFreebase cwtf = new ConvertWikiToFreebase(btf.conf);
			//cwtf.convertWikiToFreebaseEntities(barcelonaCorpusInputFolder);
			ConvertWikiToFreebaseThread cwtf = new ConvertWikiToFreebaseThread(btf.conf);
			cwtf.convertWikiToFreebaseEntitiesThread(barcelonaCorpusInputFolder);
			cwtf = null;
			System.out.println("Done converting wikipedia entities to dataset specific entities");
			btf.logger.info("Done converting wikipedia entities to dataset specific entities");
			
		//	DetermineProperties dp = new DetermineProperties(btf.conf);
		//	dp.identifyProperties(btf.conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetEntityMapping), 
		//			btf.conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetPropertyFolder));
			DeterminePropertiesThread dp = new DeterminePropertiesThread(btf.conf);
			dp.identifyPropertiesThread(btf.conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetEntityMapping), 
					btf.conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetPropertyFolder));
			dp = null;
			System.out.println("Done determining properties between entitites. This is done for each line");
			btf.logger.info("Done determining properties between entitites. This is done for each line");
			
			MergeProperties mp = new MergeProperties(btf.conf);
			mp.mergePropertiesFromMultipleFiles();
			mp = null;
			WARNING: The multi threaded version generates multiple files, they have to be combined into one, since the output of 
			**** merge properties is supposed to be a single file.
			MergePropertiesThread mp = new MergePropertiesThread(btf.conf);
			mp.mergePropertiesFromMultipleFilesThreads();
			System.out.println("Done merging properties of consecutive lines.. This is based on # of lines to merge parameter");
			btf.logger.info("Done merging properties of consecutive lines.. This is based on # of lines to merge parameter");
			
			*//**
			 * IMPORTANT for FREEBASE -> intermediate node based new edge creation.
			 * If new properties are created, by running DetermineProperties again, then we also need to update
			 * the new distinct property list. This has to be updated. 
			 * ************************ IMP **************************
			 * If any new property is created, RUN prepareTrainingData.AddNewlyAddedConcatenatedEdge() AGAIN!!!!
			 *//*
			if(isFreebaseDataset) {
				AddNewlyAddedConcatenatedEdge anac = new AddNewlyAddedConcatenatedEdge(btf.conf);
				anac.runNewConcatenatedEdges();
			}
		}*/
		
		//FrequentCorrelatedPropertiesGenerator fcpg = new FrequentCorrelatedPropertiesGenerator(btf.conf);
		FrequentCorrelatedPropertiesGeneratorThread fcpg = new FrequentCorrelatedPropertiesGeneratorThread(btf.conf);
		fcpg.generateFrequentCorrelatedProperties(isProcessingDataGraph);
		fcpg.freeglobals();
		fcpg = null;
		System.out.println("Done generating frequent property sets.. This is based on the support parameter");
		btf.logger.info("Done generating frequent property sets.. This is based on the support parameter");
		
		/**
		 * IMPORTANT:
		 * If DetermineProperties is run anew, then the new properties created may have changed.
		 * If MergeProperties or FrequentCorrelatedPropertiesGenerator have run again, the input
		 * file to AddNegativeInstances may have changed again:
		 * ************************* IMP *************************
		 * In the above cases, RUN prepareTrainingData.CreatePseudoPropertyFrequentItems() AGAIN!!!!!!!! 
		 */
		CreatePseudoPropertyFrequenItemset cdpm = new CreatePseudoPropertyFrequenItemset(btf.conf);
		cdpm.createPseudoPropertyListAndFrequentItemset(isProcessingDataGraph);
				
		AddNegativeInstances ani = new AddNegativeInstances(btf.conf);
		ani.addNegativeEdgesToLog(isProcessingDataGraph);
		System.out.println("Done adding negative edge instances to the user log file");
		btf.logger.info("Done adding negative edge instances to the user log file");
	}
}
