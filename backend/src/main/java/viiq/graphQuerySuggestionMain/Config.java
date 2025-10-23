package viiq.graphQuerySuggestionMain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.PropertyConfigurator;

public class Config extends PropertiesConfiguration 
{
	Properties props = null;
	
	public Config(String propertiesFile) throws ConfigurationException, IOException 
	{
		props = new Properties();
		File pf = new File(propertiesFile);
		FileInputStream fis = new FileInputStream(pf);
		props.load(fis);
		fis.close();
		PropertyConfigurator.configure(props);
		//setThrowExceptionOnMissing(true);
	}
	
	public String getProp(String key)
	{
		return props.getProperty(key);
	}
	
	public String getAbsolutePath(String key)
	{
		return props.getProperty(key);
	}
	
	public String getInputFilePath(String key)
	{
		String path = props.getProperty("baseInputDir");
		return (path+props.getProperty(key));
	}
	
	public String getOutputFilePath(String key)
	{
		String path = props.getProperty("baseOutputDir");
		return (path+props.getProperty(key));
	}
}
