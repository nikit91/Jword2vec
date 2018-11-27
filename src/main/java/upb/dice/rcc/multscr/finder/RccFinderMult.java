package upb.dice.rcc.multscr.finder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;

/**
 * Abstract Class to help find the closest research field for a given
 * publication
 * 
 * @author nikitsrivastava
 *
 */
public abstract class RccFinderMult {

	/**
	 * WordsetExtractor instance for publications
	 */
	protected PublicationWordSetExtractor wordSetExtractor;
	/**
	 * General Non-Normalized Word2Vec Model
	 */
	protected Word2VecModel genModel;
	/**
	 * Custom Word2Vec Model
	 */
	protected GenWord2VecModel memModel;

	/**
	 * Contructor to initialize instance of {@link RccFinder} using the given
	 * general Normalized word model and a Word2Vec custom model
	 * 
	 * @param genModel - {@link #genModel}
	 * @param memModel - {@link #memModel}
	 * @throws IOException
	 */
	/*public RccFinder(Word2VecModel genModel, GenWord2VecModel memModel) throws IOException {
		this.genModel = genModel;
		this.memModel = memModel;
		this.wordSetExtractor = new PublicationWordSetExtractor(genModel);
	}*/

	/**
	 * Contructor to initialize instance of {@link RccFinderMult} using the given
	 * general Normalized word model and a Word2Vec custom model
	 * 
	 * @param genModel         - {@link #genModel}
	 * @param memModel         - {@link #memModel}
	 * @param wordSetExtractor - {@link #wordSetExtractor}
	 * @throws IOException
	 */
	public RccFinderMult(Word2VecModel genModel, GenWord2VecModel memModel, PublicationWordSetExtractor wordSetExtractor)
			throws IOException {
		this.genModel = genModel;
		this.memModel = memModel;
		this.wordSetExtractor = wordSetExtractor;
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	abstract public List<RccNounPhraseLabelPair> findClosestResearchField(File pubFile) throws IOException;
	
	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	abstract public List<RccNounPhraseLabelPair> findClosestResearchField(File pubFile, PublicationWordSetExtractor wordSetExtractor) throws IOException;

}
