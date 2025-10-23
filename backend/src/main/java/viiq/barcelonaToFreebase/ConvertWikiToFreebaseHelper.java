package viiq.barcelonaToFreebase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ConvertWikiToFreebaseHelper 
{
	final Logger logger;
	public static HashMap<String, Integer> freebaseEntitiesMap = new HashMap<String, Integer>();
	
	public ConvertWikiToFreebaseHelper()
	{
		logger = Logger.getLogger(getClass());
	}
	
	public void loadDatasetEntities(String datasetEntitiesFilePath)
	{
		try
		{
			FileReader filer = new FileReader(datasetEntitiesFilePath);
			BufferedReader br = new BufferedReader(filer);
			String line = null;
			while((line = br.readLine()) != null)
			{
				String[] splitLine = line.split(BarcelonaToFreebaseConstants.freebaseToWikiDelimiter);
				String url = splitLine[1].trim();
				try {
					String url1 = URLDecoder.decode(url, "UTF-8").toLowerCase();
					if(freebaseEntitiesMap.containsKey(url1)) {
						System.out.println("Already seen URL, not adding again: " + url1);
					} else {
						freebaseEntitiesMap.put(url1, Integer.parseInt(splitLine[0].trim()));
					}
				} catch(UnsupportedEncodingException use) {
		//			logger.debug("Unsupported encoding URL CONVERSION : " + url);
					//use.printStackTrace();
				}
				catch(Exception e) {
		//			logger.debug("No idea what URL CONVERSION" + url);
				}
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
}
