package upb.dice.rcc.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.word2vec.Word2VecModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import upb.dice.rcc.tool.rfld.generator.RsrchFldMdlGnrtrCsv;
import upb.dice.rcc.tool.rfld.generator.RsrchFldMdlGnrtr_OldJson;

/**
 * Class to extract the wordset from the given publication file. <br>
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
	RsrchFldMdlGnrtrCsv researchFieldModelGenerator;
	private Word2VecModel w2vModel;

	public PublicationWordSetExtractor(Word2VecModel w2vModel) throws IOException {
		this.w2vModel = w2vModel;
		this.researchFieldModelGenerator = new RsrchFldMdlGnrtrCsv(w2vModel);
	}

	/**
	 * Method to extract the wordset from the given publication file
	 * 
	 * @param inputFile - the given publication file
	 * @return - set of the all the words in in the publication file
	 * @throws IOException
	 */
	public Map<String, List<String>> extractPublicationWordSet(File inputFile) throws IOException {
		// init wordset
		Map<String, List<String>> fldWordsMap = new HashMap<>();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(inputFile);
			// Read file into a json
			ObjectNode inpObj = (ObjectNode) RsrchFldMdlGnrtr_OldJson.OBJ_READER.readTree(fin);
			for (String fld : FLDS_TO_READ) {
				List<String> wordList = new ArrayList<>();
				JsonNode entryNode = inpObj.get(fld);
				if (entryNode == null) {
					continue;
				}
				ArrayNode phraseArr = (ArrayNode) entryNode;
				Iterator<JsonNode> phraseItr = phraseArr.iterator();
				while (phraseItr.hasNext()) {
					JsonNode phraseEntry = phraseItr.next();
					String line = phraseEntry.asText();
					wordList.addAll(RccUtil.fetchAllWordTokens(line, w2vModel));
				}
				fldWordsMap.put(fld, wordList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			fin.close();
		}
		return fldWordsMap;
	}

}
