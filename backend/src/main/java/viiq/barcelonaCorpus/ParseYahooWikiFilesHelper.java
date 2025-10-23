package viiq.barcelonaCorpus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;

public class ParseYahooWikiFilesHelper 
{
	final Logger logger;
	
	HashSet<String> stopwords = new HashSet<String>();
	HashSet<String> specialCharacters = new HashSet<String>();
	
	public ParseYahooWikiFilesHelper()
	{
		logger = Logger.getLogger(getClass());
	}
	
	public void loadStopWords(String fileName)
	{
		loadLinesFromFileToHashset(fileName, stopwords);
	}
	
	public void loadSpecialCharactersList(String fileName)
	{
		loadLinesFromFileToHashset(fileName, specialCharacters);
	}
	
	private void loadLinesFromFileToHashset(String filePath, HashSet<String> loadHash)
	{
		try
		{
			FileReader filer = new FileReader(filePath);
			BufferedReader br = new BufferedReader(filer);
			String line = null;
			while((line = br.readLine()) != null)
			{
				loadHash.add(line);
			}
			br.close();
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
	
	public boolean isStopWord(String token)
	{
		return stopwords.contains(token) ? true : false;
	}
	
	public boolean isSpecialCharacter(String token)
	{
		return specialCharacters.contains(token) ? true : false;
	}
}
