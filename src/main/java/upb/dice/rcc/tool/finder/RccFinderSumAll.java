package upb.dice.rcc.tool.finder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import upb.dice.rcc.tool.RccUtil;

/**
 * Class to help find the closest research field for a given publication
 * 
 * @author nikitsrivastava
 *
 */
public class RccFinderSumAll extends RccFinder {

	public RccFinderSumAll(Word2VecModel genModel, GenWord2VecModel memModel) throws IOException {
		super(genModel, memModel);
	}

	/**
	 * Method to find the closest research field for the given publication
	 * 
	 * @param pubFile - file for the given publication
	 * @throws IOException
	 */
	public RccNounPhrasePair findClosestResearchField(File pubFile) throws IOException {
		Map<String, List<String>> fldWordsMap = wordSetExtractor.extractPublicationWordSet(pubFile);
		List<String> wordList = new ArrayList<>();
		for (List<String> wordEntries : fldWordsMap.values()) {
			wordList.addAll(wordEntries);
		}
		float[] sumVec = RccUtil.getSumVector(wordList, genModel);
		float[] normSumVec = Word2VecMath.normalize(sumVec);
		String idStr = memModel.getClosestEntry(normSumVec);
		Double cosSim = Word2VecMath.cosineSimilarityNormalizedVecs(normSumVec, memModel.getW2VMap().get(idStr));
		RccNounPhrasePair tmpPair = new RccNounPhrasePair(pubFile.getName(), idStr, cosSim);
		return tmpPair;
	}

}
