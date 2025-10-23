package viiq.barcelonaToFreebase;

import viiq.graphQuerySuggestionMain.Config;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;
import viiq.barcelonaCorpus.ParseYahooWikiFilesConstants;

public class MergeProperties {
	static File[] entityMappedFiles;
	static int nthreads = 8;
	static Config conf = null;
	final Logger logger = Logger.getLogger(getClass());
	static int numOfLinesToMerge = -1;
	
	public MergeProperties(Config config)
	{
		conf = config;
	}
	
	public MergeProperties()
	{
		
	}
    
	public void mergePropertiesFromMultipleFiles() {
		numOfLinesToMerge = Integer.parseInt(conf.getProp(PropertyKeys.numberOfLinesToMerge));
		try {
			FileWriter fw = new FileWriter(conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetMergedPropertiesFile));
			BufferedWriter bw = new BufferedWriter(fw);
			File inputFolder = new File(conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetPropertyFolder));
			//File outFilePath = new File(conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetMergedPropertiesFile));
			entityMappedFiles = inputFolder.listFiles();
			for(int i=0; i<entityMappedFiles.length; i++) {
				if(entityMappedFiles[i].isFile()) {
					String inputFilePath = entityMappedFiles[i].getAbsolutePath();
					mergePropertiesOfFile(inputFilePath, bw);
				}
			}
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void mergePropertiesOfFile(String inputFilePath, BufferedWriter outWriter) throws IOException, Exception {
		FileReader fr = new FileReader(inputFilePath);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		int numOfLinesInQueue = 0;
		int lastLineSeen = -1;
		int[] lineIndex = new int[numOfLinesToMerge];
		CircularQueue queue = new CircularQueue(numOfLinesToMerge);
		////////// the first line in the file is EXPECTED to be "%%#PAGE"...
		while((line = br.readLine()) != null) {
			if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER))
				break;
		}
		while((line = br.readLine()) != null) {
			if(numOfLinesToMerge == 1) {
				if(!line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER))
					outWriter.write(removeDuplicates(new MutableString(line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter)[1])) + "\n");
				continue;
			}
			if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER)) {
				while(queue.numOfActiveElements > 0) {
					int currentLastIndex = findLongestSequence(lineIndex);
					if(currentLastIndex == -1)
						break;
					if(lineIndex[currentLastIndex] > lastLineSeen) {
						// merge entities from beginning of the queue to currentLastIndex...
						MutableString mergedProps = queue.seek(currentLastIndex+1);
						mergedProps = removeDuplicates(mergedProps);
						if(mergedProps != null) {
							outWriter.write(mergedProps.append("\n").toString());
						}
						lastLineSeen = lineIndex[currentLastIndex];
					}
					queue.dequeue();
					rollbackIndexArray(lineIndex);
				}
				numOfLinesInQueue = 0;
				lastLineSeen = -1;
				lineIndex = new int[numOfLinesToMerge];
				queue = new CircularQueue(numOfLinesToMerge);
			} else {
				if(numOfLinesInQueue < numOfLinesToMerge) {
					String[] splitLine = line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
					lineIndex[numOfLinesInQueue] = Integer.parseInt(splitLine[0]);
					queue.enqueue(splitLine[1]);
					numOfLinesInQueue++;
				} else {
					int currentLastIndex = findLongestSequence(lineIndex);
					if(lineIndex[currentLastIndex] > lastLineSeen) {
						// merge entities from beginning of the queue to currentLastIndex...
						MutableString mergedProps = queue.seek(currentLastIndex+1);
						mergedProps = removeDuplicates(mergedProps);
						if(mergedProps != null) {
							outWriter.write(mergedProps.append("\n").toString());
						}
						lastLineSeen = lineIndex[currentLastIndex];
					}
					queue.dequeue();
					rollbackIndexArray(lineIndex);
					numOfLinesInQueue--;
					String[] splitLine = line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
					lineIndex[numOfLinesInQueue] = Integer.parseInt(splitLine[0]);
					queue.enqueue(splitLine[1]);
					numOfLinesInQueue++;
				}
			}
		}
		while(queue.numOfActiveElements > 0) {
			int currentLastIndex = findLongestSequence(lineIndex);
			if(currentLastIndex == -1)
				break;
			if(lineIndex[currentLastIndex] > lastLineSeen) {
				// merge entities from beginning of the queue to currentLastIndex...
				MutableString mergedProps = queue.seek(currentLastIndex+1);
				mergedProps = removeDuplicates(mergedProps);
				if(mergedProps != null) {
					outWriter.write(mergedProps.append("\n").toString());
				}
				lastLineSeen = lineIndex[currentLastIndex];
			}
			queue.dequeue();
			rollbackIndexArray(lineIndex);
		}
		br.close();
	}
	
	private MutableString removeDuplicates(MutableString propList)
	{
		/*
		 * dbpediaOntologyProps is ONLY for dbpedia dataset!!!!! NOT for other datasets...
		 */
		HashSet<Integer> dbpediaOntologyProps = new HashSet<Integer>(4);
		int datasetFlag = Integer.parseInt(conf.getProp(PropertyKeys.datasetFlag));
		if(datasetFlag == 1) {
			// flag to indicate the dataset used is dbpedia.
			/*dbpediaOntologyProps.add(1L);
			dbpediaOntologyProps.add(2L);
			dbpediaOntologyProps.add(3L);
			dbpediaOntologyProps.add(4L);
			// there are a few dbpedia:owl based props.. there are corresponding normal dbpedia props for the same too. so deleting the owl props.
			// birthPlace
			dbpediaOntologyProps.add(1401L);
			// deathPlace
			dbpediaOntologyProps.add(3075L);
			// deathDate
			dbpediaOntologyProps.add(3073L);
			// birthDate
			dbpediaOntologyProps.add(1397L);*/
		}
		// the following four property ids are some ontology based props.. the only 4 such props in dbpedia..
		MutableString dupeFreeList = new MutableString();
		String[] props = propList.toString().split(BarcelonaToFreebaseConstants.interPropertyDelimiter);
		ArrayList<Integer> sortedList = new ArrayList<Integer>();
		HashSet<Integer> seenProps = new HashSet<Integer>();
		for(int i=0; i<props.length; i++)
		{
			int prop = Integer.parseInt(props[i]);
			// do the following "if" only for dbpedia dataset.
			if(datasetFlag == 1 && dbpediaOntologyProps.contains(prop))
				continue;
			if(!seenProps.contains(prop))
			{
				seenProps.add(prop);
				sortedList.add(prop);
			}
		}
		if(seenProps.size() < 2)
		{
			dupeFreeList = null;
		}
		else
		{
			Collections.sort(sortedList);
			//Iterator<Integer> iter = sortedList
			for(int i=0; i<sortedList.size(); i++)
			{
				dupeFreeList.append(sortedList.get(i)).append(BarcelonaToFreebaseConstants.interPropertyDelimiter);
			}
		}
		return dupeFreeList;
	}
	
	private void rollbackIndexArray(int[] lineIndex)
	{
		int i = 1;
		for(i=1; i<numOfLinesToMerge; i++)
		{
			lineIndex[i-1] = lineIndex[i];
		}
		lineIndex[i-1] = -1;
	}
	
	private int findLongestSequence(int[] lineIndex)
	{
		int j = -1;
		int i = 0;
		if(lineIndex[0] > -1 && lineIndex[1] == -1)
			j = 0;
		else
		{
			for(i=1; i<numOfLinesToMerge && lineIndex[i] != -1; i++)
			{
				j++;
				if(lineIndex[i] - lineIndex[0] >= numOfLinesToMerge)
				{
				//	j++;
					break;
				}
			}
			if(i ==  numOfLinesToMerge)
				j = numOfLinesToMerge -1;
		}
		return j;
	}
	
	public static void main(String[] args)
	{
		MergeProperties mp = new MergeProperties();
		if(args.length < 1)
		{
			System.out.println("Need an input properties file! Exiting program...");
			mp.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else
		{
			try
			{
				conf = new Config(args[0]);
			}
			catch(ConfigurationException ce)
			{
				System.out.println("Error in properties file configuration! Exiting program...");
				mp.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe)
			{
				System.out.println("IO exception while reading the properties file! Exiting program...");
				mp.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		mp.mergePropertiesFromMultipleFiles();
	}
}
