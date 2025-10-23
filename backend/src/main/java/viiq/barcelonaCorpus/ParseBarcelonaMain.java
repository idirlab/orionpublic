package viiq.barcelonaCorpus;

import java.io.File;
import java.io.IOException;

import viiq.graphQuerySuggestionMain.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import viiq.utils.PropertyKeys;

/**
 * 
 * @author nj
 * This package viiq.deals with the Yahoo Barcelona Wikipedia dataset. It generates
 * the bunch of entities that appear together in consecutive line(s). It generates
 * two outputs. One which lists these entities with context, another without context.
 */
public class ParseBarcelonaMain 
{
	final Logger logger = Logger.getLogger(getClass());
	Config conf = null;
	public static void main(String[] args)
	{
		ParseBarcelonaMain pbm = new ParseBarcelonaMain();
		if(args.length < 1)
		{
			System.out.println("Need an input properties file! Exiting program...");
			pbm.logger.error("Need an input properties file! Exiting program...");
			return;
		}
		else
		{
			try
			{
				pbm.conf = new Config(args[0]);
			}
			catch(ConfigurationException ce)
			{
				System.out.println("Error in properties file configuration! Exiting program...");
				pbm.logger.error("Error in properties file configuration! Exiting program...");
				ce.printStackTrace();
				return;
			}
			catch(IOException ioe)
			{
				System.out.println("IO exception while reading the properties file! Exiting program...");
				pbm.logger.error("IO exception while reading the properties file! Exiting program...");
				ioe.printStackTrace();
				return;
			}
		}
		File barcelonaCorpusInputFolder = new File(pbm.conf.getInputFilePath(PropertyKeys.barcelonaCorpusFolder));
		ParseYahooWikiFiles pywf = new ParseYahooWikiFiles(pbm.conf);
		pywf.parseWikiFiles(barcelonaCorpusInputFolder);
	}
}
