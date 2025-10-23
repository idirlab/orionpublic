package viiq.utils;

public class PropertyKeys
{
	// Input paths...
	public static final String barcelonaCorpusFolder = "QuerySuggestion.Input.BarcelonaCorpusFolder";
	public static final String testPartialAndTargetQueryFiles= "QuerySuggestion.Input.TestPartialAndTargetQueryFilesFolder";

	// Input files...
	public static final String stopwordsFile = "QuerySuggestion.Input.Stopwords";
	public static final String specialCharsFile = "QuerySuggestion.Input.specialCharacters";
	public static final String intermediateNodesFile = "QuerySuggestion.Input.IntermediateNodesList";
	public static final String edgeTypeFile = "QuerySuggestion.Input.EdgeTypeFile";
	public static final String originalEdgeTypeFile = "QuerySuggestion.Input.OriginalEdgeTypeFile";

	public static final String datasetToWikipediaEntityMapFile = "QuerySuggestion.Input.DatasetToWikipediaEntityMap";
	public static final String datagraphFile = "QuerySuggestion.Input.Datagraph";
	public static final String datagraphFileOnlyEntityIntermediate = "QuerySuggestion.Input.DatagraphOnlyEntityIntermediate";
	public static final String datagraphWithoutTypesDomainsFile = "QuerySuggestion.Input.DatagraphWithoutTypesDomainsFile";

	public static final String typesListFile = "QuerySuggestion.Input.TypesListFile";
	public static final String domainsListFile = "QuerySuggestion.Input.DomainsListFile";

	public static final String originalPropertiesMapFile = "QuerySuggestion.Input.OriginalPropertiesMapFile";
	public static final String propertiesMapFile = "QuerySuggestion.Input.PropertiesMapFile";
	public static final String distinctPropertiesList = "QuerySuggestion.Input.DistinctPropertiesList";
	public static final String distinctPropertiesListMapping = "QuerySuggestion.Input.DistinctPropertiesListMapping";

	public static final String datasetFlag = "QuerySuggestion.Input.Dataset";
//	public static final String domainToTypeIndexFile = "QuerySuggestion.Input.DomainToTypeIndex";
//	public static final String typeToEdgeTypeIndexFile = "QuerySuggestion.Input.TypeToEdgeTypeIndex";

	public static final String typeLangEn = "QuerySuggestion.Input.EdgeTypeLangEn";
	public static final String entityLangEn = "QuerySuggestion.Input.EntityLangEn";
	public static final String domainLangEn = "QuerySuggestion.Input.DomainLangEn";
	public static final String propertiesLangEn = "QuerySuggestion.Input.PropertiesLangEn";
	public static final String instanceCountForTypes = "QuerySuggestion.Input.InstanceCountForTypes";
	public static final String instanceCountForEdges = "QuerySuggestion.Input.InstanceCountForEdges";

//	public static final String typeLangEn = "QuerySuggestion.Input.TypeLangEng";
//	public static final String domainLangEn = "QuerySuggestion.Input.DomainLangEng";
//	public static final String edgeTypeLangEn = "QuerySuggestion.Input.EdgeTypeLangEng";
//	public static final String propertiesLangEng = "QuerySuggestion.Input.PropertiesLangEng";
//	public static final String entityLangEn = "QuerySuggestion.Input.EntityLangEng";
	public static final String domainTypesFolder = "QuerySuggestion.Input.DomainTypesFolder";

	// Output paths...
	public static final String barcelonaCorpusParsedFolder = "QuerySuggestion.Output.BarcelonaCorpusParsedFolder";
	public static final String barcelonaCorpusParsedEntityListFolder = "QuerySuggestion.Output.BarcelonaCorpusParsedEntitiesListFolder";

	public static final String barcelonaToDatasetEntityMapping = "QuerySuggestion.Output.BarcelonaToDatasetEntityMappingFolder";
	public static final String barcelonaToDatasetPropertyFolder = "QuerySuggestion.Output.BarcelonaToDatasetPropertyFolder";
	public static final String barcelonaToDatasetMergedPropertyFolder = "QuerySuggestion.Output.barcelonaToDatasetMergedPropertyFolder";

	public static final String barcelonaToFreebaseNewConcatenatedPropertiesFile = "QuerySuggestion.Output.BarcelonaToFreebaseNewConcatenatedPropertiesFile";

	public static final String newConcatenatedPropertiesMapFile = "QuerySuggestion.Input.NewConcatenatedPropertiesMapFile";
	public static final String newConcatenatedEdgeTypeFile = "QuerySuggestion.Input.NewConcatenatedEdgeTypeFile";

	public static final String datagraphMergedPropertiesFile = "QuerySuggestion.Input.DatagraphMergedPropertiesFile";

	public static final String datagraphFrequentPropertiesFile = "QuerySuggestion.Output.DatagraphFrequentPropertiesList";
	public static final String datagraphFrequentPseudoPropertiesFile = "QuerySuggestion.Output.DatagraphFrequentPseudoPropertiesList";
	public static final String datagraphFrequentPropertiesMappedToValueFile = "QuerySuggestion.Output.DatagraphMappedFrequentPropertiesToValueFile";
	public static final String datagraphTrainigDataWithIDFile = "QuerySuggestion.Output.DatagraphTrainingDataWithNegativeEdgesIDs";
	public static final String datagraphTrainingDataWithPseudoIDFile = "QuerySuggestion.Output.DatagraphTrainingDataWithNegativePseudoEdgesIDs";
	public static final String datagraphTrainigDataWithValueFile = "QuerySuggestion.Output.DatagraphTrainingDataWithNegativeEdgesValues";
	public static final String datagraphTrainigDataWithDuplicatesIDFile = "QuerySuggestion.Output.DatagraphTrainingDataWithDuplicatesNegativeEdgesIDs";

	public static final String barcelonaToDatasetMergedPropertiesFile = "QuerySuggestion.Output.BarcelonaToDatasetMergedProperties";
	public static final String frequentPropertiesFile = "QuerySuggestion.Output.FrequentPropertiesList";
	public static final String frequentPseudoPropertiesFile = "QuerySuggestion.Output.FrequentPseudoPropertiesList";
	public static final String frequentPropertiesMappedToValueFile = "QuerySuggestion.Output.MappedFrequentPropertiesToValueFile";
	public static final String trainingDataWithIDFile = "QuerySuggestion.Output.TrainingDataWithNegativeEdgesIDs";
	public static final String trainingDataWithPseudoIDFile = "QuerySuggestion.Output.TrainingDataWithNegativePseudoEdgesIDs";
	public static final String trainigDataWithValueFile = "QuerySuggestion.Output.TrainingDataWithNegativeEdgesValues";
	public static final String trainigDataWithDuplicatesIDFile = "QuerySuggestion.Output.TrainingDataWithDuplicatesNegativeEdgesIDs";
	public static final String edgeSuggestionOutputFolder = "QuerySuggestion.Output.EdgeSuggestionOutputFolder";
	public static final String trainingDataSize = "QuerySuggestion.Output.TrainingDataSize";

	public static final String bayesianPriorProbabilities = "QuerySuggestion.Output.BayesianPriorProbabilities";

	public static final String numberOfContextTokens = "QuerySuggestion.NumberOfContextTokens";
	public static final String numberOfLinesToMerge = "QuerySuggestion.NumberOfConsecutiveLinesToMerge";
	public static final String frequentPropsSetThreshod = "QuerySuggestion.FrequentPropsSupport";
	public static final String oneItemPropsSetThreshod = "QuerySuggestion.Oneitem.FrequentPropsSupport";
	public static final String modelToUse = "QuerySuggestion.ModelToUse";
	public static final String ignoreNegativeEdges = "QuerySuggestion.IgnoreNegativeEdgesInHistory";
	public static final String NumberOfSuggestionsThreshold = "QuerySuggestion.NumberOfSuggestionsThreshold";
	public static final String concatenateIntermediatNodeEdges = "QuerySuggestion.ConcatenateIntermediatNodeEdges";
	public static final String loadDataGraphFlag = "QuerySuggestion.LoadDataGraph";

	// Parameters for random subsets based edge suggestion.
	public static final String randomSubsetStrategy = "QuerySuggestion.RandomSubsets.RandomSubsetStrategy";
	public static final String numberOfRandomSubsets = "QuerySuggestion.RandomSubsets.NumberOfRandomSubsets";
	public static final String weightedAverage = "QuerySuggestion.RandomSubsets.WeightedAverage";
	public static final String isRandRules = "QuerySuggestion.RandomSubsets.IsRandRules";
	public static final String isWeightedConf = "QuerySuggestion.RandomSubsets.IsWeightedConf";
	public static final String topkRules = "QuerySuggestion.RandomSubsets.TopkRules";
	public static final String countCondition = "QuerySuggestion.RandomSubsets.CountCondition";



	

	// Parameters for decision forest (random correlation path algorithm)
	public static final String userLogSizeThreshold = "QuerySuggestion.DecisionForest.RandomCorrelationPathsCandidateUserLogSizeThreshold";
	public static final String userLogSizeThresholdDecayFactorMultiplier = "QuerySuggestion.DecisionForest.RandomCorrelationPathsCandidateUserLogSizeThresholdDecayFactorMultiplier";
	public static final String numeratorPower = "QuerySuggestion.DecisionForest.NumeratorPower";

	// Parameters for random forest
	public static final String numberOfTreesInRandomForest = "QuerySuggestion.RandomForest.NumberOfClassifiers";
	public static final String randomForestTreesOutputFolder = "QuerySuggestion.Output.RandomForest.RandomForestTreesFolder";
	public static final String randomForestTempFolder = "QuerySuggestion.Output.RandomForest.RandomForestTempFolder";
	public static final String rfAllAttributesFile = "QuerySuggestion.Output.RandomForest.RandomForestFiles.AllAttributes";
	public static final String rfFeaturesIndexFile = "QuerySuggestion.Output.RandomForest.RandomForestFiles.FeaturesIndex";
	public static final String rfNumClassifiersFile = "QuerySuggestion.Output.RandomForest.RandomForestFiles.NumberOfClassifiers";

	// parameters to align the data graph content to the same number of characters in each line. required for random file access.
	public static final String datagraphAlignmentLength = "QuerySuggestion.Input.DatagraphAlignmentLength";
	public static final String datagraphNumberOfLines = "QuerySuggestion.Input.DatagraphNumberOfLines";
	public static final String datagraphAlignedFile = "QuerySuggestion.Input.DatagraphAlignedFile";
	public static final String datagraphSourceAlignedFile = "QuerySuggestion.Input.Datagraph.SourceSorted";
	public static final String datagraphObjectAlignedFile = "QuerySuggestion.Input.Datagraph.ObjectSorted";

	// properties for various GET requests
	public static final String typesSortedToInstancesIndexFile = "QuerySuggestion.Input.TypesSortedToInstancesIndex";
	public static final String typesSortedToInstancesIndexAlignmentLength = "QuerySuggestion.Input.TypesSortedToInstancesIndexAlignmentLength";
	public static final String typesSortedToInstancesIndexPaddedFile = "QuerySuggestion.Input.TypesSortedToInstancesIndexPadded";
	public static final String typesSortedToInstancesIndexNumberOfLines = "QuerySuggestion.Input.TypesSortedToInstancesIndexNumberOfLines";
	public static final String typesSortedToInstancesIndexFileCol2 = "QuerySuggestion.Input.TypesSortedToInstancesIndexCol2";
	public static final String typesSortedToInstancesIndexAlignmentLengthCol2 = "QuerySuggestion.Input.TypesSortedToInstancesIndexAlignmentLengthCol2";
	public static final String typesSortedToInstancesIndexPaddedFileCol2 = "QuerySuggestion.Input.TypesSortedToInstancesIndexPaddedCol2";
	public static final String typesSortedToInstancesIndexNumberOfLinesCol2 = "QuerySuggestion.Input.TypesSortedToInstancesIndexNumberOfLinesCol2";
	public static final String typesSortedToInstancesIndexFileCol3 = "QuerySuggestion.Input.TypesSortedToInstancesIndexCol3";
	public static final String typesSortedToInstancesIndexAlignmentLengthCol3 = "QuerySuggestion.Input.TypesSortedToInstancesIndexAlignmentLengthCol3";
	public static final String typesSortedToInstancesIndexPaddedFileCol3 = "QuerySuggestion.Input.TypesSortedToInstancesIndexPaddedCol3";
	public static final String typesSortedToInstancesIndexNumberOfLinesCol3 = "QuerySuggestion.Input.TypesSortedToInstancesIndexNumberOfLinesCol3";
	public static final String typesSortedToInstancesIndexFileCol4 = "QuerySuggestion.Input.TypesSortedToInstancesIndexCol4";
	public static final String typesSortedToInstancesIndexAlignmentLengthCol4 = "QuerySuggestion.Input.TypesSortedToInstancesIndexAlignmentLengthCol4";
	public static final String typesSortedToInstancesIndexPaddedFileCol4 = "QuerySuggestion.Input.TypesSortedToInstancesIndexPaddedCol4";
	public static final String typesSortedToInstancesIndexNumberOfLinesCol4 = "QuerySuggestion.Input.TypesSortedToInstancesIndexNumberOfLinesCol4";
	public static final String typesSortedToInstancesIndexFileCol5 = "QuerySuggestion.Input.TypesSortedToInstancesIndexCol5";
	public static final String typesSortedToInstancesIndexAlignmentLengthCol5 = "QuerySuggestion.Input.TypesSortedToInstancesIndexAlignmentLengthCol5";
	public static final String typesSortedToInstancesIndexPaddedFileCol5 = "QuerySuggestion.Input.TypesSortedToInstancesIndexPaddedCol5";
	public static final String typesSortedToInstancesIndexNumberOfLinesCol5 = "QuerySuggestion.Input.TypesSortedToInstancesIndexNumberOfLinesCol5";
	public static final String typesSortedToInstancesIndexFileCol6 = "QuerySuggestion.Input.TypesSortedToInstancesIndexCol6";
	public static final String typesSortedToInstancesIndexAlignmentLengthCol6 = "QuerySuggestion.Input.TypesSortedToInstancesIndexAlignmentLengthCol6";
	public static final String typesSortedToInstancesIndexPaddedFileCol6 = "QuerySuggestion.Input.TypesSortedToInstancesIndexPaddedCol6";
	public static final String typesSortedToInstancesIndexNumberOfLinesCol6 = "QuerySuggestion.Input.TypesSortedToInstancesIndexNumberOfLinesCol6";


	public static final String instancesSortedToTypesIndex = "QuerySuggestion.Input.InstancesSortedToTypesIndex";
	public static final String instancesSortedToTypesIndexAlignmentLength = "QuerySuggestion.Input.InstancesSortedToTypesIndexAlignmentLength";
	public static final String instancesSortedToTypesIndexPaddedFile = "QuerySuggestion.Input.InstancesSortedToTypesIndexPadded";
	public static final String instancesSortedToTypesIndexNumberOfLines = "QuerySuggestion.Input.InstancesSortedToTypesIndexNumberOfLines";

	public static final String instancesLangEnSortedFile = "QuerySuggestion.Input.InstancesLangEnSorted";
	public static final String instancesLangEnSortedAlignmentLength = "QuerySuggestion.Input.InstancesLangEnSortedAlignmentLength";
	public static final String instancesLangEnSortedPaddedFile = "QuerySuggestion.Input.InstancesLangEnSortedPadded";
	public static final String instancesLangEnSortedNumberOfLines = "QuerySuggestion.Input.InstancesLangEnSortedNumberOfLines";
	public static final String instancesLangEnSortedFileCol2 = "QuerySuggestion.Input.InstancesLangEnSortedCol2";
	public static final String instancesLangEnSortedAlignmentLengthCol2 = "QuerySuggestion.Input.InstancesLangEnSortedAlignmentLengthCol2";
	public static final String instancesLangEnSortedPaddedFileCol2 = "QuerySuggestion.Input.InstancesLangEnSortedPaddedCol2";
	public static final String instancesLangEnSortedNumberOfLinesCol2 = "QuerySuggestion.Input.InstancesLangEnSortedNumberOfLinesCol2";
	public static final String instancesLangEnSortedFileCol3 = "QuerySuggestion.Input.InstancesLangEnSortedCol3";
	public static final String instancesLangEnSortedAlignmentLengthCol3 = "QuerySuggestion.Input.InstancesLangEnSortedAlignmentLengthCol3";
	public static final String instancesLangEnSortedPaddedFileCol3 = "QuerySuggestion.Input.InstancesLangEnSortedPaddedCol3";
	public static final String instancesLangEnSortedNumberOfLinesCol3 = "QuerySuggestion.Input.InstancesLangEnSortedNumberOfLinesCol3";
	public static final String instancesLangEnSortedFileCol4 = "QuerySuggestion.Input.InstancesLangEnSortedCol4";
	public static final String instancesLangEnSortedAlignmentLengthCol4 = "QuerySuggestion.Input.InstancesLangEnSortedAlignmentLengthCol4";
	public static final String instancesLangEnSortedPaddedFileCol4 = "QuerySuggestion.Input.InstancesLangEnSortedPaddedCol4";
	public static final String instancesLangEnSortedNumberOfLinesCol4 = "QuerySuggestion.Input.InstancesLangEnSortedNumberOfLinesCol4";
	public static final String instancesLangEnSortedFileCol5 = "QuerySuggestion.Input.InstancesLangEnSortedCol5";
	public static final String instancesLangEnSortedAlignmentLengthCol5 = "QuerySuggestion.Input.InstancesLangEnSortedAlignmentLengthCol5";
	public static final String instancesLangEnSortedPaddedFileCol5 = "QuerySuggestion.Input.InstancesLangEnSortedPaddedCol5";
	public static final String instancesLangEnSortedNumberOfLinesCol5 = "QuerySuggestion.Input.InstancesLangEnSortedNumberOfLinesCol5";
	public static final String instancesLangEnSortedFileCol6 = "QuerySuggestion.Input.InstancesLangEnSortedCol6";
	public static final String instancesLangEnSortedAlignmentLengthCol6 = "QuerySuggestion.Input.InstancesLangEnSortedAlignmentLengthCol6";
	public static final String instancesLangEnSortedPaddedFileCol6 = "QuerySuggestion.Input.InstancesLangEnSortedPaddedCol6";
	public static final String instancesLangEnSortedNumberOfLinesCol6 = "QuerySuggestion.Input.InstancesLangEnSortedNumberOfLinesCol6";

	public static final String instancesIdsortedLangEn = "QuerySuggestion.Input.InstancesIdSortedLang";
	public static final String instancesIdsortedLangEnAlignmentLength = "QuerySuggestion.Input.InstancesIdSortedLangEnAlignmentLength";
	public static final String instancesIdsortedLangEnNumberOfLines = "QuerySuggestion.Input.InstancesIdSortedLangEnNumberOfLines";
	public static final String instancesIdsortedLangEnPaddedFile = "QuerySuggestion.Input.InstancesIdSortedLangEnPadded";

	public static final String typesLangEnSortedFile = "QuerySuggestion.Input.TypesLangEnSorted";
	public static final String typesLangEnSortedAlignmentLength = "QuerySuggestion.Input.TypesLangEnSortedAlignmentLength";
	public static final String typesLangEnSortedPaddedFile = "QuerySuggestion.Input.TypesLangEnSortedPadded";
	public static final String typesLangEnSortedNumberOfLines = "QuerySuggestion.Input.TypesLangEnSortedNumberOfLines";
	public static final String typesLangEnSortedFileCol2 = "QuerySuggestion.Input.TypesLangEnSortedCol2";
	public static final String typesLangEnSortedAlignmentLengthCol2 = "QuerySuggestion.Input.TypesLangEnSortedAlignmentLengthCol2";
	public static final String typesLangEnSortedPaddedFileCol2 = "QuerySuggestion.Input.TypesLangEnSortedPaddedCol2";
	public static final String typesLangEnSortedNumberOfLinesCol2 = "QuerySuggestion.Input.TypesLangEnSortedNumberOfLinesCol2";
	public static final String typesLangEnSortedFileCol3 = "QuerySuggestion.Input.TypesLangEnSortedCol3";
	public static final String typesLangEnSortedAlignmentLengthCol3 = "QuerySuggestion.Input.TypesLangEnSortedAlignmentLengthCol3";
	public static final String typesLangEnSortedPaddedFileCol3 = "QuerySuggestion.Input.TypesLangEnSortedPaddedCol3";
	public static final String typesLangEnSortedNumberOfLinesCol3 = "QuerySuggestion.Input.TypesLangEnSortedNumberOfLinesCol3";
	public static final String typesLangEnSortedFileCol4 = "QuerySuggestion.Input.TypesLangEnSortedCol4";
	public static final String typesLangEnSortedAlignmentLengthCol4 = "QuerySuggestion.Input.TypesLangEnSortedAlignmentLengthCol4";
	public static final String typesLangEnSortedPaddedFileCol4 = "QuerySuggestion.Input.TypesLangEnSortedPaddedCol4";
	public static final String typesLangEnSortedNumberOfLinesCol4 = "QuerySuggestion.Input.TypesLangEnSortedNumberOfLinesCol4";
	public static final String typesLangEnSortedFileCol5 = "QuerySuggestion.Input.TypesLangEnSortedCol5";
	public static final String typesLangEnSortedAlignmentLengthCol5 = "QuerySuggestion.Input.TypesLangEnSortedAlignmentLengthCol5";
	public static final String typesLangEnSortedPaddedFileCol5 = "QuerySuggestion.Input.TypesLangEnSortedPaddedCol5";
	public static final String typesLangEnSortedNumberOfLinesCol5 = "QuerySuggestion.Input.TypesLangEnSortedNumberOfLinesCol5";
	public static final String typesLangEnSortedFileCol6 = "QuerySuggestion.Input.TypesLangEnSortedCol6";
	public static final String typesLangEnSortedAlignmentLengthCol6 = "QuerySuggestion.Input.TypesLangEnSortedAlignmentLengthCol6";
	public static final String typesLangEnSortedPaddedFileCol6 = "QuerySuggestion.Input.TypesLangEnSortedPaddedCol6";
	public static final String typesLangEnSortedNumberOfLinesCol6 = "QuerySuggestion.Input.TypesLangEnSortedNumberOfLinesCol6";

	public static final String typesIdSortedLangEn = "QuerySuggestion.Input.TypesIdSortedLangEn";
	public static final String typesIdSortedLangEnAlignmentLength = "QuerySuggestion.Input.TypesIdSortedLangEnAlignmentLength";
	public static final String typesIdSortedLangEnNumberOfLines = "QuerySuggestion.Input.TypesIdSortedLangEnNumberOfLines";
	public static final String typesIdSortedLangEnPaddedFile = "QuerySuggestion.Input.TypesIdSortedLangEnPadded";

	public static final String domainsIdSortedTypesLangEn = "QuerySuggestion.Input.DomainsIdSortedTypesLangEn";
	public static final String domainsIdSortedTypesLangEnAlignmentLength = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnAlignmentLength";
	public static final String domainsIdSortedTypesLangEnNumberOfLines = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnNumberOfLines";
	public static final String domainsIdSortedTypesLangEnPadded = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnPadded";
	public static final String domainsIdSortedTypesLangEnCol2 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnCol2";
	public static final String domainsIdSortedTypesLangEnAlignmentLengthCol2 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnAlignmentLengthCol2";
	public static final String domainsIdSortedTypesLangEnNumberOfLinesCol2 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnNumberOfLinesCol2";
	public static final String domainsIdSortedTypesLangEnPaddedCol2 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnPaddedCol2";
	public static final String domainsIdSortedTypesLangEnCol3 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnCol3";
	public static final String domainsIdSortedTypesLangEnAlignmentLengthCol3 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnAlignmentLengthCol3";
	public static final String domainsIdSortedTypesLangEnNumberOfLinesCol3 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnNumberOfLinesCol3";
	public static final String domainsIdSortedTypesLangEnPaddedCol3 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnPaddedCol3";
	public static final String domainsIdSortedTypesLangEnCol4 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnCol4";
	public static final String domainsIdSortedTypesLangEnAlignmentLengthCol4 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnAlignmentLengthCol4";
	public static final String domainsIdSortedTypesLangEnNumberOfLinesCol4 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnNumberOfLinesCol4";
	public static final String domainsIdSortedTypesLangEnPaddedCol4 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnPaddedCol4";
	public static final String domainsIdSortedTypesLangEnCol5 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnCol5";
	public static final String domainsIdSortedTypesLangEnAlignmentLengthCol5 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnAlignmentLengthCol5";
	public static final String domainsIdSortedTypesLangEnNumberOfLinesCol5 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnNumberOfLinesCol5";
	public static final String domainsIdSortedTypesLangEnPaddedCol5 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnPaddedCol5";
	public static final String domainsIdSortedTypesLangEnCol6 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnCol6";
	public static final String domainsIdSortedTypesLangEnAlignmentLengthCol6 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnAlignmentLengthCol6";
	public static final String domainsIdSortedTypesLangEnNumberOfLinesCol6 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnNumberOfLinesCol6";
	public static final String domainsIdSortedTypesLangEnPaddedCol6 = "QuerySuggestion.Input.DomainsIdSortedTypesLangEnPaddedCol6";

	public static final String domainsIdSortedInstancesLangEn = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEn";
	public static final String domainsIdSortedInstancesLangEnAlignmentLength = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnAlignmentLength";
	public static final String domainsIdSortedInstancesLangEnNumberOfLines = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnNumberOfLines";
	public static final String domainsIdSortedInstancesLangEnPadded = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnPadded";
	public static final String domainsIdSortedInstancesLangEnCol2 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnCol2";
	public static final String domainsIdSortedInstancesLangEnAlignmentLengthCol2 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnAlignmentLengthCol2";
	public static final String domainsIdSortedInstancesLangEnNumberOfLinesCol2 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnNumberOfLinesCol2";
	public static final String domainsIdSortedInstancesLangEnPaddedCol2 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnPaddedCol2";
	public static final String domainsIdSortedInstancesLangEnCol3 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnCol3";
	public static final String domainsIdSortedInstancesLangEnAlignmentLengthCol3 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnAlignmentLengthCol3";
	public static final String domainsIdSortedInstancesLangEnNumberOfLinesCol3 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnNumberOfLinesCol3";
	public static final String domainsIdSortedInstancesLangEnPaddedCol3 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnPaddedCol3";
	public static final String domainsIdSortedInstancesLangEnCol4 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnCol4";
	public static final String domainsIdSortedInstancesLangEnAlignmentLengthCol4 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnAlignmentLengthCol4";
	public static final String domainsIdSortedInstancesLangEnNumberOfLinesCol4 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnNumberOfLinesCol4";
	public static final String domainsIdSortedInstancesLangEnPaddedCol4 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnPaddedCol4";
	public static final String domainsIdSortedInstancesLangEnCol5 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnCol5";
	public static final String domainsIdSortedInstancesLangEnAlignmentLengthCol5 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnAlignmentLengthCol5";
	public static final String domainsIdSortedInstancesLangEnNumberOfLinesCol5 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnNumberOfLinesCol5";
	public static final String domainsIdSortedInstancesLangEnPaddedCol5 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnPaddedCol5";
	public static final String domainsIdSortedInstancesLangEnCol6 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnCol6";
	public static final String domainsIdSortedInstancesLangEnAlignmentLengthCol6 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnAlignmentLengthCol6";
	public static final String domainsIdSortedInstancesLangEnNumberOfLinesCol6 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnNumberOfLinesCol6";
	public static final String domainsIdSortedInstancesLangEnPaddedCol6 = "QuerySuggestion.Input.DomainsIdSortedInstancesLangEnPaddedCol6";


	public static final String typesIdSortedInstancesIdSorted = "QuerySuggestion.Input.TypesIdSortedInstancesIdSorted";
	public static final String typesIdSortedInstancesIdSortedAlingnmentLength = "QuerySuggestion.Input.TypesIdSortedInstancesIdSortedAlignmentLength";
	public static final String typesIdSortedInstancesIdSortedNumberOfLines = "QuerySuggestion.Input.TypesIdSortedInstancesIdSortedNumberOfLines";
	public static final String typesIdSortedInstancesIdSortedPadded = "QuerySuggestion.Input.TypesIdSortedInstancesIdSortedPadded";

	// Files required for creating edges associated with types
	public static final String typesIdSortedInstances = "QuerySuggestion.Input.TypesIdSortedInstances";
	public static final String concatenatedPropertiesMappingFile = "QuerySuggestion.Input.ConcatenatedPropertiesMappingFile";
	public static final String propertiesFile = "QuerySuggestion.Input.PropertiesFile";
	public static final String propertiesFileCardinality = "QuerySuggestion.Input.PropertiesCardinalityEstimate";
	public static final String typeEdgesListSource = "QuerySuggestion.Input.TypeEdgesListSource";
	public static final String typeEdgesListObject = "QuerySuggestion.Input.TypeEdgesListObject";
	public static final String typesWithCandidateEdges= "QuerySuggestion.Input.TypesWithCandidateEdges";

	public static final String numberOfThreads = "QuerySuggestion.Dataprocessing.NumberOfThreads";
	public static final String dataGraphBasedQueryLog = "QuerySuggestion.Input.DataGraphBasedQueryLog";

	public static final String freebaseToWikipediaMapping = "QuerySuggestion.Input.FreebaseWikiMapping";
	public static final String wikipediaPreviewFile = "QuerySuggestion.Input.WikipediaPreviewFile";
	public static final String edgePreviewFile = "QuerySuggestion.Input.edgePreviewFile";
	public static final String typeToEntitiesFile = "QuerySuggestion.Input.TypeToEntities";

	public static final String historyUpdate = "QuerySuggestion.Input.HistoryUpdate";
	public static final String decayFactorMultiplier = "QuerySuggestion.Input.DecayFactorMultiplier";
	public static final String decayFactorThreshold = "QuerySuggestion.Input.DecayFactorThreshold";
	public static final String edgeScoreThresholdBLR = "QuerySuggestion.Input.EdgeScoreThresholdBlr";
	public static final String edgeScoreThresholdRDP = "QuerySuggestion.Input.EdgeScoreThresholdRdp";
	public static final String candidateObjectEndType = "QuerySuggestion.Input.CandidateObjectEndType";
	public static final String candidateSubjectEndType = "QuerySuggestion.Input.CandidateSubjectEndType";
	public static final String typesClique = "QuerySuggestion.Input.TypesClique";
	public static final String typesPairClique ="QuerySuggestion.Input.TypesPairClique";
	public static final String entityEdgeCount = "QuerySuggestion.Input.EntityEdgeCount";
	public static final String entityEdgeCountAlignmentLength = "QuerySuggestion.Input.EntityEdgeCountAlignmentLength";
	public static final String entityEdgeCountNumberOfLines = "QuerySuggestion.Input.EntityEdgeCountNumberOfLines";

	public static final String datagraphPredicateAligned = "QuerySuggestion.Input.DatagraphPredicateAligned";
	public static final String datagraphPredicateAlignedLength = "QuerySuggestion.Input.DatagraphPredicateAlignedLength";
	public static final String datagraphPredicateAlignedNumberOfLines = "QuerySuggestion.Input.DatagraphPredicateAlignedNumberOfLines";

	public static final String datagraphPredicateSourceAlignedFile = "QuerySuggestion.Input.DatagraphPredicateSourceAlignedFile";
	public static final String datagraphPredicateTargetAlignedFile = "QuerySuggestion.Input.DatagraphPredicateTargetAlignedFile";

	public static final String sourceAlignedPropertyTable = "QuerySuggestion.Input.SourceAlignedPropertyTable";
	public static final String targetAlignedPropertyTable = "QuerySuggestion.Input.TargetAlignedPropertyTable";

	public static final String maxAnswerLimit = "QuerySuggestion.Input.MaxAnswerLimit";
	public static final String maxLogLoadIntoMemory = "QuerySuggestion.Input.MaxLogLoadIntoMemory";

}
