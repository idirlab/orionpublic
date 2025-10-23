package viiq.barcelonaToFreebase;

import viiq.graphQuerySuggestionMain.Config;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;
import viiq.barcelonaCorpus.ParseYahooWikiFilesConstants;

/**
 * This class started off as mapping a wikipedia UID found in the barcelona corpus
 * to an integer ID of the corresponding entitiy in Freebase (stored in DB).
 * But then this mapping will change based on the dataset (Freebase, yago, dbpedia)..
 * Although the name of this class is a misnomer, this handles other datasets
 * too, taking the exact dataset from the properties file.
 * @author nj
 *
 */
public class ConvertWikiToFreebase 
{
	Config conf = null;
	final Logger logger;
	
	public ConvertWikiToFreebase(Config conf)
	{
		this.conf = conf;
		this.logger = Logger.getLogger(getClass());
		//cwh = new ConvertWikiToFreebaseHelper();
	}
	
	public void convertWikiToFreebaseEntities(File inputFolderPath)
	{
		/**
		 * IMPROTANT NOTE:
		 * the input file and the output folder mentioned here is dataset specific.. Change both of them accordingly...
		 */
		//String outFileFolderParsedEntityList = conf.getOutputFilePath(PropertyKeys.barcelonaCorpusParsedEntityListFolder);
		//cwh.loadFreebaseEntities(conf.getInputFilePath(PropertyKeys.freebaseWikipediaEntityMapFile));
		//String outFileFolderMapped = conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseEntityMapping);
		String outFileFolderMapped = conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetEntityMapping);
		System.out.println("starting loading property map.");
		ConvertWikiToFreebaseHelper cwh = new ConvertWikiToFreebaseHelper();
		cwh.loadDatasetEntities(conf.getInputFilePath(PropertyKeys.datasetToWikipediaEntityMapFile));
		System.out.println("DONE loading property map.");
		File[] listOfWikiFiles = inputFolderPath.listFiles();
		for(int i=0; i<listOfWikiFiles.length; i++)
		{
			if(listOfWikiFiles[i].isFile())
			{
				String outFileName = listOfWikiFiles[i].getName();
				String inputFilePath = listOfWikiFiles[i].getAbsolutePath();
				String mappedOutputFilePath = outFileFolderMapped + outFileName;
				mapWikiToFreebaseEntity(inputFilePath, mappedOutputFilePath);
			}
		}
	}
	
	private void mapWikiToFreebaseEntity(String inputFilePath, String outputFilePath)
	{
		try
		{
			FileWriter fw = new FileWriter(outputFilePath);
			BufferedWriter bw = new BufferedWriter(fw);
			FileReader fr = new FileReader(inputFilePath);
			BufferedReader br = new BufferedReader(fr);
			
			String line = null;
			// If an entity ID corresponding to a ##%PAGE is not found in freebase, skip the entire page and its sentences..
			boolean skipPage = false;
			while((line = br.readLine()) != null)
			{
				MutableString freebaseLine = new MutableString();
				if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER))
				{
					// This indicates the beginning of a new page/doc.. The entity link here DOES NOT start with "B-".
					String[] split = line.split(BarcelonaToFreebaseConstants.pageValueDelimiter);
					MutableString link = null;
					try
					{
						//link = new MutableString(URLDecoder.decode(BarcelonaToFreebaseConstants.dbpediaLinkStart + split[1].trim(), "UTF-8"));
						link = new MutableString(URLDecoder.decode(BarcelonaToFreebaseConstants.wikiLinkStart + split[1].trim(), "UTF-8"));
						if(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.containsKey(link.toString()))
						{
							freebaseLine.append(split[0].trim()).append(BarcelonaToFreebaseConstants.pageValueDelimiter);
							freebaseLine.append(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.get(link.toString())).append("\n");
							bw.write(freebaseLine.toString());
							skipPage = false;
							continue;
						}
						else
						{
							logger.warn("Skipping entity:: " + link);
							skipPage = true;
						}
					}
					catch(UnsupportedEncodingException use)
					{
						logger.error("Unsupported encoding URL CONVERSION : " + link);
						//use.printStackTrace();
					}
					catch(Exception e)
					{
						logger.error("No idea what URL CONVERSION" + link);
					}
					
				}
				else if(skipPage)
					continue;
				else
				{
					String[] split = line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
					freebaseLine.append(split[0].trim()).append(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
					String[] wikiFarEnts = split[1].trim().split(ParseYahooWikiFilesConstants.farEntityDelimiter);
					MutableString splitValue = null;
					for(int i=0; i<wikiFarEnts.length; i++)
					{
						//splitValue = null;
						String[] wikiNearEnts = wikiFarEnts[i].split(ParseYahooWikiFilesConstants.closeEntityDelimiter);
						int j = 0;
						for(j=0; j<wikiNearEnts.length; j++)
						{
							//String ent = wikiNearEnts[j].replace(ParseYahooWikiFilesConstants.LINK_MARKER, BarcelonaToFreebaseConstants.dbpediaLinkStart);
							String ent = wikiNearEnts[j].replace(ParseYahooWikiFilesConstants.LINK_MARKER, BarcelonaToFreebaseConstants.wikiLinkStart);
							try
							{
								ent = URLDecoder.decode(ent, "UTF-8");
							}
							catch(UnsupportedEncodingException use)
							{
								logger.debug("Unsupported encoding URL CONVERSION : " + ent);
								//use.printStackTrace();
							}
							catch(Exception e)
							{
								logger.debug("No idea what URL CONVERSION" + ent);
							}
							if(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.containsKey(ent))
							{
								if(splitValue == null)
									splitValue = new MutableString();
								splitValue.append(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.get(ent)).append(ParseYahooWikiFilesConstants.closeEntityDelimiter);
								/*if(j < wikiNearEnts.length - 1)
								{
									splitValue.append(ParseYahooWikiFilesConstants.closeEntityDelimiter);
								}*/
							}
							else
							{
								logger.warn("Skipping entity:: " + ent);
							}
						}
						// add the last entity in this close range...
						/*if(cwh.freebaseEntitiesMap.containsKey(wikiNearEnts[j]) && splitValue != null)
						{
							String ent = wikiNearEnts[j].replace(ParseYahooWikiFilesConstants.LINK_MARKER, BarcelonaToFreebaseConstants.wikiLinkStart);
							if(!splitValue.endsWith(ParseYahooWikiFilesConstants.closeEntityDelimiter))
								splitValue.append(ParseYahooWikiFilesConstants.closeEntityDelimiter);
							splitValue.append(cwh.freebaseEntitiesMap.get(ent));
						}*/
						if(splitValue != null)
							splitValue.append(ParseYahooWikiFilesConstants.farEntityDelimiter);
					}
					if(splitValue != null)
					{
						freebaseLine.append(splitValue).append("\n");
						bw.write(freebaseLine.toString());
					}
				}
			}
			br.close();
			bw.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
