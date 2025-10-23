package viiq;

import java.io.IOException;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

import viiq.utils.PropertyKeys;
import viiq.clientServer.server.LoadData;
import viiq.decisionForest.LearnDecisionForest;

@ComponentScan
@EnableAutoConfiguration
public class SpringServer {
	final Logger logger = Logger.getLogger(getClass());
	static String propertiesFilePath;
	Config conf = null;

	public static void main(String[] args) {
		SpringServer ss = new SpringServer();
		if(args.length < 1) {
			System.out.println("Need an input properties file! Exiting program...");
			//btf.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else {
			try {
				ss.conf = new Config(args[0]);
				propertiesFilePath = args[0];
			}
			catch(ConfigurationException ce) {
				System.out.println("Error in properties file configuration! Exiting program...");
			//	btf.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe) {
				System.out.println("IO exception while reading the properties file! Exiting program...");
			//	btf.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		long start = System.currentTimeMillis();
		LoadData ldlm = new LoadData();
		System.out.println("Loading type to edge list map, for source types");
		ldlm.loadSourceTypeToEdgesMap(ss.conf.getInputFilePath(PropertyKeys.typeEdgesListSource));
		System.out.println("Loading type to edge list map, for object types");
		ldlm.loadObjectTypeToEdgesMap(ss.conf.getInputFilePath(PropertyKeys.typeEdgesListObject));
		System.out.println("Loading edge type file");
		ldlm.loadEdgeTypeInfo(ss.conf.getInputFilePath(PropertyKeys.edgeTypeFile));
		System.out.println("Populating intermediate nodes");
		ldlm.loadIntermediateNodesFromFile(ss.conf.getInputFilePath(PropertyKeys.intermediateNodesFile));
		System.out.println("loading the concatenated property mapping list");
		ldlm.loadConcatedPropertiesList(ss.conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseNewConcatenatedPropertiesFile));
		System.out.println("Load data for finding the number of instances associated with each type");
		ldlm.loadInstancesPerTypeCount(ss.conf.getInputFilePath(PropertyKeys.instanceCountForTypes));
		System.out.println("Load data for finding the number of instances associated with each edge");
		ldlm.loadInstancesPerEdgeCount(ss.conf.getInputFilePath(PropertyKeys.instanceCountForEdges));

    System.out.println("Load data for entity preview");
    ldlm.loadWikipediaPreview(ss.conf.getInputFilePath(PropertyKeys.wikipediaPreviewFile));

		System.out.println("Load data for edge preview");
    ldlm.loadEdgePreview(ss.conf.getInputFilePath(PropertyKeys.edgePreviewFile));

		System.out.println("Load data for type to entity mappings");
		ldlm.loadTypetoEntitiesMapping(ss.conf.getInputFilePath(PropertyKeys.typesSortedToInstancesIndexFile));

		System.out.println("Load data for types clique (if there is an actor who is singer, also there is another actor who is player; player and singer would be in same clique)");
    ldlm.loadTypesClique(ss.conf.getInputFilePath(PropertyKeys.typesClique), ss.conf.getInputFilePath(PropertyKeys.typesPairClique));

		/*
		 * The commented out loads are needed for GenerateCandidates.java (NOT GenerateCandidatesNew.java)
		System.out.println("starting to load the data graph");
		boolean loadDataGraph = false;
		if(Integer.parseInt(ss.conf.getProp(PropertyKeys.loadDataGraphFlag)) == 1)
			loadDataGraph = true;
		ldlm.loadDataGraphIntProperty(ss.conf.getInputFilePath(PropertyKeys.datagraphFile), loadDataGraph);
		System.out.println("Done loading data graph");
		ldlm.populateInvertedNodeType();*/

		/*System.out.println("loading domain to types index");
		ldlm.loadDomainToTypeIndex(ss.conf.getInputFilePath(PropertyKeys.domainToTypeIndexFile));
		System.out.println("loading types to edgeTypes index");
		ldlm.loadTypeToEdgeTypeIndex(ss.conf.getInputFilePath(PropertyKeys.typeToEdgeTypeIndexFile)); */

		System.out.println("Load edge labels");
		ldlm.loadEdgeLabels(ss.conf.getInputFilePath(PropertyKeys.propertiesLangEn));
		System.out.println("Load node labels");
		ldlm.loadAllNodeLabels(ss.conf.getInputFilePath(PropertyKeys.domainLangEn), ss.conf.getInputFilePath(PropertyKeys.typeLangEn), ss.conf.getInputFilePath(PropertyKeys.entityLangEn));

		System.out.println("Load candidate subject end types");
		ldlm.loadCandidateSubjectEndType(ss.conf.getInputFilePath(PropertyKeys.candidateSubjectEndType));

		System.out.println("Load candidate object end types");
		ldlm.loadCandidateObjectEndType(ss.conf.getInputFilePath(PropertyKeys.candidateObjectEndType));

		System.out.println("Load type domain mapping");
		ldlm.loadTypeDomainMap(ss.conf.getInputFilePath(PropertyKeys.domainsIdSortedTypesLangEnPadded));

		System.out.println("Load domain label mapping");
		ldlm.loadDomainLabelMap(ss.conf.getInputFilePath(PropertyKeys.domainLangEn));

		System.out.println("Load end entities of all edgetypes");
		ldlm.loadEdgeEndEntities(ss.conf.getInputFilePath(PropertyKeys.datagraphSourceAlignedFile));

		// System.out.println("Load property table");
		// ldlm.loadPropertyTable(ss.conf.getInputFilePath(PropertyKeys.datagraphPredicateSourceAlignedFile), true);
		// ldlm.loadPropertyTable(ss.conf.getInputFilePath(PropertyKeys.datagraphPredicateTargetAlignedFile), false);



		/******************************** End of Freebase specific things to load ************************************/

		/**
		 * USER Study specific things to do. Remove this after user study is done!
		 */
		/*ldlm.populateQueriesForUserStudy();
		System.out.println("Done populating queries for user study!");*/

		/**
		 * Now start learning the model for decision forests.
		 */
		System.out.println("learn decision forest model");
		LearnDecisionForest ldf = new LearnDecisionForest(ss.conf);
		ldf.learnModel(50, false, 5, 1, 0);
    	System.out.println("Done learning the decision forest model");

		System.out.println("Total time to load everything = "+(System.currentTimeMillis() - start)/1000.0 + " seconds.");

		//run the server
    SpringApplication.run(SpringServer.class, args);


  }
}
