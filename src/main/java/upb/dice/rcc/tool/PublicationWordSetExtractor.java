package upb.dice.rcc.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
/**
 * Class to extract the wordset from the given publication file.
 *<br>
 * Publication input file json format:
 * 
 * <pre>
 *{
 * "abstract": [...],
 * "title" : [...],
 * "keywords" : [...]
 *}
 * </pre>
 * <dl>
 * <dt>abstract</dt>
 * <dd>array of words in the abstract of the publication</dd>
 * <dt>title</dt>
 * <dd>array of title strings of the publication</dd>
 * <dt>keywords</dt>
 * <dd>array of keywords of the publication</dd>
 * </dl>
 * 
 * 
 * @author nikitsrivastava
 *
 */
public class PublicationWordSetExtractor {
	public static final String[] FLDS_TO_READ = { "abstract", "title", "keywords" };
	ResearchFieldModelGenerator researchFieldModelGenerator;

	public PublicationWordSetExtractor(Word2VecModel w2vModel, File stopWordsFile) throws IOException {
		this.researchFieldModelGenerator = new ResearchFieldModelGenerator(w2vModel);
		this.researchFieldModelGenerator.readStopWords(stopWordsFile);
	}
	/**
	 * Method to extract the wordset from the given publication file
	 * @param inputFile - the given publication file
	 * @return - set of the all the words in in the publication file
	 * @throws IOException
	 */
	public Set<String> extractPublicationWordSet(File inputFile) throws IOException {
		// init wordset
		Set<String> wordSet = new HashSet<>();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(inputFile);
			// Read file into a json
			ObjectNode inpObj = (ObjectNode) ResearchFieldModelGenerator.OBJ_READER.readTree(fin);
			for (String fld : FLDS_TO_READ) {
				JsonNode entryNode = inpObj.get(fld);
				if(entryNode == null) {
					continue;
				}
				ArrayNode phraseArr = (ArrayNode) entryNode;
				Iterator<JsonNode> phraseItr = phraseArr.iterator();
				while (phraseItr.hasNext()) {
					JsonNode phraseEntry = phraseItr.next();
					String line = phraseEntry.asText();
					wordSet.addAll(this.researchFieldModelGenerator.fetchAllWordTokens(line));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			fin.close();
		}
		return wordSet;
	}

}
