package viiq.barcelonaCorpus;

public class ParseYahooWikiFilesConstants 
{
	/*final Logger logger;
	public ParseYahooWikiFilesConstants()
	{
		logger = Logger.getLogger(getClass());
	}*/
	
	/*
	 * The Yahoo Barcelona dataset has the following columns:
	 * token	POS		lemma	CONL	WNSS	WSJ		ana		head	deplabel	link
	 */
	public static final int TOKEN = 0;
	public static final int POS = 1;
	public static final int LEMMA = 2;
	public static final int CONL = 3;
	public static final int WNSS = 4; 
	public static final int WSJ = 5;
	public static final int ANA = 6;
	public static final int HEAD = 7;
	public static final int DELPABEL = 8;
	public static final int LINK = 9;
	
	// delimiters and constants used in Barcelona corpus..
	public static final String META_MARKER = "%%#";
	public static final String DOC_MARKER = "%%#DOC";
	public static final String PAGE_MARKER = "%%#PAGE";
	public static final String SENTENCE_MARKER = "%%#SEN";
	public static final String COLUMN_DELIMITER = "\t";
	public static final String LINK_MARKER = "B-/wiki/";
	public static final String LINK_CONTINUE_MARKER = "I-/wiki/";
	public static final String NON_LINK_MARKER = "0";
	
	// **** IMP USAGE INFORMATION *****
	// remember to use categor_link_marker with "contains()" instead of startsWith.
	// These can either start with "B-/wiki/Categories:" or "I-/wiki/Categories:"...
	public static final String CATEGORY_LINK_MARKER = "/wiki/Category:";
	// Use this too has "contains"..
	public static final String NON_WIKI_LINK_MARKER = "http://";
	public static final String WIKI_MAINTENANCE_LINK_MARKER = "/wiki/Wikipedia:";
	
	
	public static final int numberOfColumns = 10;
	
	// delimiters and constants to be used in the output files created by our program.
	public static final String emptyContext = "%%#";
	public static final String sentenceNumValueDelimiter = "\t";
	public static final char contextEntityDelimiter = ',';
	// delimiter for the entitiesListSentence.. comma separated close entities.. ; separated far away entities.
	public static final String closeEntityDelimiter = ",";
	public static final String farEntityDelimiter = ";";
	
	// Assuming maximum non-stopword tokens in a sentence is 25. It could be higher, in which case this has to be changed.
	public static final int MAX_TOKENS_PER_SENTENCE = 60;
	// Assuming maximum number of entities in a sentence is equal to 15. This is a very generous estimate I guess.
	public static final int MAX_ENTITIES_PER_SENTENCE = 20;
}
