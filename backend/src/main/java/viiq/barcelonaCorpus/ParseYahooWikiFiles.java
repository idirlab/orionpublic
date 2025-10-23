package viiq.barcelonaCorpus;

import viiq.graphQuerySuggestionMain.Config;
import it.unimi.dsi.lang.MutableString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

public class ParseYahooWikiFiles 
{
	Config conf = null;
	final Logger logger;
	
	ParseYahooWikiFilesHelper pyhelper = new ParseYahooWikiFilesHelper();
	//ParseYahooWikiFilesConstants pyconstants = new ParseYahooWikiFilesConstants();
	int numberOfContextTokens;
	
	public ParseYahooWikiFiles(Config conf)
	{
		this.conf = conf;
		this.logger = Logger.getLogger(getClass());
		numberOfContextTokens = Integer.parseInt(conf.getProp(PropertyKeys.numberOfContextTokens));
	}
	
	public void parseWikiFiles(File barcelonaCorpusInputFolder)
	{
		String outFileFolderParsed = conf.getOutputFilePath(PropertyKeys.barcelonaCorpusParsedFolder);
		String outFileFolderParsedEntityList = conf.getOutputFilePath(PropertyKeys.barcelonaCorpusParsedEntityListFolder);
		pyhelper.loadStopWords(conf.getInputFilePath(PropertyKeys.stopwordsFile));
		pyhelper.loadSpecialCharactersList(conf.getInputFilePath(PropertyKeys.specialCharsFile));
		
		File[] listOfWikiFiles = barcelonaCorpusInputFolder.listFiles();
		for(int i=0; i<listOfWikiFiles.length; i++)
		{
			if(listOfWikiFiles[i].isFile())
			{
				String outFileName = listOfWikiFiles[i].getName();
				String inputFilePath = listOfWikiFiles[i].getAbsolutePath();
				String parsedOutputFilePath = outFileFolderParsed + outFileName;
				String parsedEntityListOutputFilePath = outFileFolderParsedEntityList + outFileName;
				parseSingleWikiFile(inputFilePath, parsedOutputFilePath, parsedEntityListOutputFilePath);
			}
		}
	}
	
	private void parseSingleWikiFile(String inputFilePath, String parsedOutputFilePath, String parsedEntityListOutputFilePath)
	{
		/*
		 * The Yahoo Barcelona dataset has the following columns:
		 * token	POS		lemma	CONL	WNSS	WSJ		ana		head	deplabel	link
		 * 
		 * I would most likely be interested in "lemma" and "link". There is a value starting with B- if a lemma actually refers to
		 * an entity. If the mention for an entity has multiple tokens, every token has the link, but the second token onwards will
		 * start from I-. If there is no link, the value is 0.
		 * The following is an example snippet covering the three cases:
		 * of		IN	of		0		0				0			0	19	NMOD	0
		   Charles	NNP	charles	B-PER	B-noun.person	B-E:PERSON	0	20	PMOD	B-/wiki/Charles_the_Bald
		   the		DT	the		I-PER	I-noun.person	I-E:PERSON	0	23	NMOD	I-/wiki/Charles_the_Bald
		   Bald		NNP	bald	I-PER	I-noun.person	I-E:PERSON	0	19	NMOD	I-/wiki/Charles_the_Bald
		 */
		logger.info("Working on file: " + inputFilePath);
		try
		{
			FileWriter parsedfw = new FileWriter(parsedOutputFilePath);
			BufferedWriter parsedbw = new BufferedWriter(parsedfw);
			
			FileWriter parsedEntityfw = new FileWriter(parsedEntityListOutputFilePath);
			BufferedWriter parsedEntitybw = new BufferedWriter(parsedEntityfw);
			
			FileReader fr = new FileReader(inputFilePath);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			// skip the initial few lines which is the header information..
			while((line = br.readLine()) != null)
			{
				if(line.startsWith(ParseYahooWikiFilesConstants.DOC_MARKER))
					break;
			}
			// now continue reading lines to parse all documents in this file..
			int sentNum = 0;
			int docNum = 0;
		//	MutableString sentence = new MutableString();
			String[] tokensInSentence = new String[ParseYahooWikiFilesConstants.MAX_TOKENS_PER_SENTENCE];
			int[] entitiesPositionInSentence = new int[ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE];
			int tokenID = 0;
			int entityID = 0;
			String doc = new String();
			
			while((line = br.readLine()) != null)
			{
				if(line.startsWith(ParseYahooWikiFilesConstants.DOC_MARKER))
				{
					doc = line;
					continue;
				}
				else if(line.startsWith(ParseYahooWikiFilesConstants.PAGE_MARKER))
				{
					// beginning of a new document...
					docNum++;
					parsedbw.write(line + "\n");
					parsedEntitybw.write(line + "\n");
					sentNum = 0;
					tokenID = 0;
					entityID = 0;
				}
				else if(line.startsWith(ParseYahooWikiFilesConstants.SENTENCE_MARKER))
				{
					// beginning of a new sentence. Write out all that we have seen of the previous sentence and re-initialize.
					if(sentNum > 0)
					{
						// write out all that we have seen for the previous sentence. We should be losing it after this if condition.
						MutableString contextEntitySentence = new MutableString();
						MutableString entitiesListSentence = new MutableString();
						
						contextEntitySentence.append(sentNum).append(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
						entitiesListSentence.append(sentNum).append(ParseYahooWikiFilesConstants.sentenceNumValueDelimiter);
						// the array size can be bigger than necessary. -1 and null are placeholders to know when the array has ended.
						if(entityID < ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE && entityID > 0)
						{
							entitiesPositionInSentence[entityID] = -1;
							tokensInSentence[tokenID] = null;
							
							// call method to parse the sentence tokens and create the required parsed sentences.
							constructContextEntity(tokensInSentence, entitiesPositionInSentence, contextEntitySentence, entitiesListSentence);
							
							contextEntitySentence.append("\n");
							parsedbw.write(contextEntitySentence.toString());
							entitiesListSentence.append("\n");
							parsedEntitybw.write(entitiesListSentence.toString());
						}
						else
						{
							// It is very unlikely to find a valid sentence with so many entities in it!! This must be one of those
							// sentences that just lists many entities.
							// The other reason to be here is that there were no entities in this sentence.. so skipping this..
							logger.warn("OMITTED docID, doc, sentence number: " + docNum + ":\t" + ":\t" + doc + sentNum);
						}
					}
					tokensInSentence = null;
					entitiesPositionInSentence = null;
					tokensInSentence = new String[ParseYahooWikiFilesConstants.MAX_TOKENS_PER_SENTENCE];
					entitiesPositionInSentence = new int[ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE];
					tokenID = 0;
					entityID = 0;
					sentNum++;
				}
				else
				{
					if(sentNum == 0)
						continue;
					String[] splitLine = line.split(ParseYahooWikiFilesConstants.COLUMN_DELIMITER);
					if(splitLine.length == ParseYahooWikiFilesConstants.numberOfColumns)
					{
						if(!processLine(splitLine))
							continue;
						if(tokenID == ParseYahooWikiFilesConstants.MAX_TOKENS_PER_SENTENCE - 1)
						{
							logger.warn("Sentence has exceeded token limit: " + docNum + ":\t" + ":\t" + doc + sentNum);
							continue;
						}
						if(splitLine[ParseYahooWikiFilesConstants.LINK].startsWith(ParseYahooWikiFilesConstants.LINK_MARKER))
						{
							// this is a link (entity).
							tokensInSentence[tokenID] = splitLine[ParseYahooWikiFilesConstants.LINK];
							if(entityID == ParseYahooWikiFilesConstants.MAX_ENTITIES_PER_SENTENCE)
							{
								continue;
							}
							entitiesPositionInSentence[entityID] = tokenID;
							entityID++;
						}
						else if(splitLine[ParseYahooWikiFilesConstants.LINK].equals(ParseYahooWikiFilesConstants.NON_LINK_MARKER))
						{
							// this is a normal token.. part of the context..
							tokensInSentence[tokenID] = splitLine[ParseYahooWikiFilesConstants.LEMMA];
						}
						// NOTE: We are ignoring the mention associated with an entity (by ignoring tokens corresponding to "I-" links..
						tokenID++;
					}
				}
			}
			br.close();
			parsedbw.close();
			parsedEntitybw.close();
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
	
	private boolean processLine(String[] splitLine)
	{
		boolean process = true;
		if(pyhelper.isStopWord(splitLine[ParseYahooWikiFilesConstants.LEMMA]))
			process = false;
		else if(pyhelper.isSpecialCharacter(splitLine[ParseYahooWikiFilesConstants.LEMMA]))
			process = false;
		else if(splitLine[ParseYahooWikiFilesConstants.LINK].startsWith(ParseYahooWikiFilesConstants.LINK_CONTINUE_MARKER))
			process = false;
		
		else if(splitLine[ParseYahooWikiFilesConstants.LINK].contains(ParseYahooWikiFilesConstants.CATEGORY_LINK_MARKER))
			process = false;
		else if(splitLine[ParseYahooWikiFilesConstants.LINK].contains(ParseYahooWikiFilesConstants.NON_WIKI_LINK_MARKER))
			process = false;
		else if(splitLine[ParseYahooWikiFilesConstants.LINK].contains(ParseYahooWikiFilesConstants.WIKI_MAINTENANCE_LINK_MARKER))
			process = false;
		
		return process;
	}
	
	private void constructContextEntity(String[] tokensInSentence, int[] entitiesPositionInSentence, 
			MutableString contextEntitySentence, MutableString entitiesListSentence)
	{
		// if an element in tokensInSentence is null, there are no more tokens in the array.
		// if an element in entitiesPositionInSentence is -1, there are no more valid entries in the array.
		int currentTokenIndex = 0;
		int prevEntityIndex = 0;
		int nextEntityIndex = 0;
		int index = 0;
		for(index=0; index < entitiesPositionInSentence.length && entitiesPositionInSentence[index] != -1; index++)
		{
			nextEntityIndex = entitiesPositionInSentence[index];
			
			if(nextEntityIndex-currentTokenIndex <= numberOfContextTokens)
			{
				// next entity and current entities are "close" to each other. 
				// So, the left context for the next entity has to be absent!
				// Only the current entity gets its right context.
				if(currentTokenIndex > 0)
				{
					// We have already found at least one entity in the past. So the following is the index setting:
					/*
					 * currentTokenIndex is one index after the last entity that was found.
					 */
					// We can thus add the right context of the last entity that was found.
					// The left context of the new entity we just hit is null because these two entities are close to each other.
					MutableString rightContext = createContext(currentTokenIndex, nextEntityIndex, tokensInSentence);
					contextEntitySentence.append(rightContext).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
					// add an empty left context for the new entity found...
					contextEntitySentence.append(ParseYahooWikiFilesConstants.emptyContext).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
					// append the new entity found...
					contextEntitySentence.append(tokensInSentence[nextEntityIndex]).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
					
					/*
					 * Construct the entitiesListSentence string..
					 * We know that the previous entity was close to this entity.
					 */
					entitiesListSentence.append(ParseYahooWikiFilesConstants.closeEntityDelimiter).append(tokensInSentence[nextEntityIndex]);
				}
			}
			else
			{
				// The next entity and the current entity are separated by enough number of context tokens.. 
				// So both the next entity gets its left context and the current entity gets its right context.
				if(currentTokenIndex > 0)
				{
					// We have already found at least one entity in the past. So the following is the index setting:
					/*
					 * currentTokenIndex is one index after the last entity that was found.
					 */
					// We can thus add the right context of the last entity that was found.
					// We can also add the left context of the new entity found.
					int rightContextEndIndex = currentTokenIndex + numberOfContextTokens;
					MutableString rightContext = createContext(currentTokenIndex, rightContextEndIndex, tokensInSentence);
					contextEntitySentence.append(rightContext).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
					int leftContextStartIndex = nextEntityIndex - numberOfContextTokens;
					MutableString leftContext = createContext(leftContextStartIndex, nextEntityIndex, tokensInSentence);
					contextEntitySentence.append(leftContext).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
					contextEntitySentence.append(tokensInSentence[nextEntityIndex]).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
					
					/*
					 * Construct the entitiesListSentence string..
					 * We know that the previous entity was far away from to this entity.
					 */
					entitiesListSentence.append(ParseYahooWikiFilesConstants.farEntityDelimiter).append(tokensInSentence[nextEntityIndex]);
				}
			}
			if(currentTokenIndex == 0)
			{
				// This means we have just hit our first entity in the list. 
				// The number of tokens to the left of this entity is <= the number of context tokens allowed.
				// So we only have to add the left context of this entity..
				currentTokenIndex = (nextEntityIndex - numberOfContextTokens) > 0 ? (nextEntityIndex - numberOfContextTokens) : 0;
				MutableString leftContext = createContext(currentTokenIndex, nextEntityIndex, tokensInSentence);
				contextEntitySentence.append(leftContext).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
				// append the entity too..
				contextEntitySentence.append(tokensInSentence[nextEntityIndex]).append(ParseYahooWikiFilesConstants.contextEntityDelimiter);
				
				/*
				 * Construct the entitiesListSentence string..
				 */
				entitiesListSentence.append(tokensInSentence[nextEntityIndex]);
			}
			
			// currentTokenIndex is now pointing to "nextEntityIndex"...
			// **** Make currentTokenIndex to point to one index after the "nextEntityIndex" ****
			currentTokenIndex = nextEntityIndex + 1;
			prevEntityIndex = nextEntityIndex;
		}
		if(index > 0)
		{
			// add the right context for the last entity if at least one entity was found.. MAKE error checks!
			MutableString rightContext = new MutableString();
			int i = currentTokenIndex;
			for(i=currentTokenIndex; i<currentTokenIndex+numberOfContextTokens; i++)
			{
				if(tokensInSentence[i] == null)
				{
					break;
				}
				// add the right context elements...
				rightContext.append(tokensInSentence[i]).append(" ");
			}
			if(i == currentTokenIndex)
			{
				// there is not right context.. the entity was the last token in this sentence.
				rightContext.append(ParseYahooWikiFilesConstants.emptyContext);
			}
			else
			{
				rightContext.trim();
			}
			contextEntitySentence.append(rightContext);
		}
		entitiesListSentence.append(ParseYahooWikiFilesConstants.farEntityDelimiter);
	}
	
	private MutableString createContext(int currentTokenIndex, int nextEntityIndex, String[] tokensInSentence)
	{
		MutableString context = new MutableString();
		if(currentTokenIndex == nextEntityIndex)
			context.append(ParseYahooWikiFilesConstants.emptyContext);
		else
		{
			for(int i=currentTokenIndex; i<nextEntityIndex; i++)
			{
				context.append(tokensInSentence[i]).append(" ");
			}
		}
		return context.trim();
	}
}