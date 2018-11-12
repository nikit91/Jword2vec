package upb.dice.rcc.tool.finder;

import java.io.File;
import java.io.IOException;

import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import upb.dice.rcc.tool.PublicationWordSetExtractor;

/**
 * Class to help find the closest research field for a given publication
 * 
 * @author nikitsrivastava
 *
 */
public abstract class RccFinder {

	protected PublicationWordSetExtractor wordSetExtractor;
	protected Word2VecModel genModel;
	protected GenWord2VecModel memModel;

	public RccFinder(Word2VecModel genModel, GenWord2VecModel memModel)
			throws IOException {
		this.genModel = genModel;
		this.memModel = memModel;
		this.wordSetExtractor = new PublicationWordSetExtractor(genModel);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	abstract public RccNounPhrasePair findClosestResearchField(File pubFile) throws IOException;

}
