package upb.dice.rcc.tool.vocab.extractor;
/**
 * Wrapper class for method resources extracted from dbpedia
 * @author nikitsrivastava
 *
 */
public class Methodology {
	private int id;
	private String uri;
	private String label;
	private String abs;

	public Methodology(int id, String uri, String label, String abs) {
		super();
		this.id = id;
		this.uri = uri;
		this.label = label;
		this.abs = abs;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getAbs() {
		return abs;
	}

	public void setAbs(String abs) {
		this.abs = abs;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Methodology [id=" + id + ", uri=" + uri + ", label=" + label + ", abs=" + abs + "]";
	}

}
