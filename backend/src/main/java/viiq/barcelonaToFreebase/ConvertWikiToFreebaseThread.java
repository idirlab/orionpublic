package viiq.barcelonaToFreebase;

import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import viiq.barcelonaCorpus.ParseYahooWikiFilesConstants;
import viiq.graphQuerySuggestionMain.Config;
import viiq.utils.PropertyKeys;

class MyConvertWikiToFreebaseThread implements Runnable {
	/*Logger logger = null;
	
	private synchronized void instantiateLogger() {
		if(logger == null) {
			logger = Logger.getLogger(getClass());
		}
	}*/
	
	private void mapWikiToFreebaseEntity(String inputFilePath, String outputFilePath) {
		try {
			FileWriter fw = new FileWriter(outputFilePath);
			BufferedWriter bw = new BufferedWriter(fw);
			FileReader fr = new FileReader(inputFilePath);
			BufferedReader br = new BufferedReader(fr);
			
			String line = null;
			// If an entity ID corresponding to a ##%PAGE is not found in freebase, skip the entire page and its sentences..
			boolean skipPage = false;
			while((line = br.readLine()) != null) {
				MutableString freebaseLine = new MutableString();
				if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER)) {
					// This indicates the beginning of a new page/doc.. The entity link here DOES NOT start with "B-".
					String regex = "\\s+|"+BarcelonaToFreebaseConstants.pageValueDelimiter;
					String[] split = line.split(regex);
					//link = new MutableString(URLDecoder.decode(BarcelonaToFreebaseConstants.dbpediaLinkStart + split[1].trim(), "UTF-8"));
					String linkapp = BarcelonaToFreebaseConstants.wikiLinkStart + split[1];
					try {
						String link = URLDecoder.decode(linkapp.trim(), "UTF-8").toLowerCase();
						if(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.containsKey(link)) {
							freebaseLine.append(split[0].trim()).append(BarcelonaToFreebaseConstants.pageValueDelimiter);
							freebaseLine.append(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.get(link)).append("\n");
							bw.write(freebaseLine.toString());
							skipPage = false;
							continue;
						} else {
							//System.out.println("Skipping entity:: " + link);
							skipPage = true;
						}
					} catch(UnsupportedEncodingException use) {
						System.out.println("Unsupported encoding URL CONVERSION : " + linkapp);
						//use.printStackTrace();
					} catch(Exception e) {
						System.out.println("No idea what URL CONVERSION" + linkapp);
					}
				} else if(skipPage) {
					continue;
				} else {
					String[] split = line.split(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
					freebaseLine.append(split[0].trim()).append(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
					String[] wikiFarEnts = split[1].trim().split(ParseYahooWikiFilesConstants.farEntityDelimiter);
					MutableString splitValue = null;
					for(int i=0; i<wikiFarEnts.length; i++) {
						//splitValue = null;
						String[] wikiNearEnts = wikiFarEnts[i].split(ParseYahooWikiFilesConstants.closeEntityDelimiter);
						int j = 0;
						for(j=0; j<wikiNearEnts.length; j++) {
							//String ent = wikiNearEnts[j].replace(ParseYahooWikiFilesConstants.LINK_MARKER, BarcelonaToFreebaseConstants.dbpediaLinkStart);
							String ent = wikiNearEnts[j].replace(ParseYahooWikiFilesConstants.LINK_MARKER, BarcelonaToFreebaseConstants.wikiLinkStart);
							try {
								ent = URLDecoder.decode(ent.trim(), "UTF-8").toLowerCase();
							} catch(UnsupportedEncodingException use) {
								System.out.println("Unsupported encoding URL CONVERSION : " + ent);
								//use.printStackTrace();
							} catch(Exception e) {
								System.out.println("No idea what URL CONVERSION" + ent);
							}
							if(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.containsKey(ent)) {
								if(splitValue == null)
									splitValue = new MutableString();
								splitValue.append(ConvertWikiToFreebaseHelper.freebaseEntitiesMap.get(ent)).append(ParseYahooWikiFilesConstants.closeEntityDelimiter);
								/*if(j < wikiNearEnts.length - 1)
								{
									splitValue.append(ParseYahooWikiFilesConstants.closeEntityDelimiter);
								}*/
							} else {
								//System.out.println("Skipping entity:: " + ent);
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
					if(splitValue != null) {
						freebaseLine.append(splitValue).append("\n");
						bw.write(freebaseLine.toString());
					}
				}
			}
			br.close();
			bw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		//instantiateLogger();
		int tid = Integer.parseInt(Thread.currentThread().getName());
		for(int i=tid; i<ConvertWikiToFreebaseThread.listOfWikiFiles.length; i=i+ConvertWikiToFreebaseThread.nthreads) {
			if(ConvertWikiToFreebaseThread.listOfWikiFiles[i].isFile())	{
				String outFileName = ConvertWikiToFreebaseThread.listOfWikiFiles[i].getName();
				String inputFilePath = ConvertWikiToFreebaseThread.listOfWikiFiles[i].getAbsolutePath();
				String mappedOutputFilePath = ConvertWikiToFreebaseThread.outFileFolderMapped + outFileName;
				mapWikiToFreebaseEntity(inputFilePath, mappedOutputFilePath);
			}
		}
	}
}

public class ConvertWikiToFreebaseThread {
	static Config conf = null;
	static File[] listOfWikiFiles;
	static String outFileFolderMapped;
	static int nthreads = 8;
	
	public ConvertWikiToFreebaseThread(Config config) {
		conf = config;
	}
	
	public ConvertWikiToFreebaseThread() {
	}
	
	public void convertWikiToFreebaseEntitiesThread(File inputFolderPath) {
		/**
		 * IMPROTANT NOTE:
		 * the input file and the output folder mentioned here is dataset specific.. Change both of them accordingly...
		 */
		nthreads = Integer.parseInt(conf.getProp(PropertyKeys.numberOfThreads));
		//String outFileFolderParsedEntityList = conf.getOutputFilePath(PropertyKeys.barcelonaCorpusParsedEntityListFolder);
		//String outFileFolderMapped = conf.getOutputFilePath(PropertyKeys.barcelonaToFreebaseEntityMapping);
		outFileFolderMapped = conf.getOutputFilePath(PropertyKeys.barcelonaToDatasetEntityMapping);
		System.out.println("starting loading property map.");
		ConvertWikiToFreebaseHelper cwh = new ConvertWikiToFreebaseHelper();
		cwh.loadDatasetEntities(conf.getInputFilePath(PropertyKeys.datasetToWikipediaEntityMapFile));
		System.out.println("DONE loading property map.");
		listOfWikiFiles = inputFolderPath.listFiles();
		try {
			ArrayList<Thread> at = new ArrayList<Thread>();
			MyConvertWikiToFreebaseThread mt = new MyConvertWikiToFreebaseThread();
			for(int i=0; i<nthreads; i++) {
				Thread thr = new Thread(mt);
				at.add(thr);
				thr.setName(i+"");
				thr.start();
			}
			for(int i=0; i<nthreads; i++) {
				at.get(i).join();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
