package upb.dice.rcc.tool.vocab.extractor;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TopicNode {
	
	private String resourceUri;
	private String label;
	private String description;
	private TopicNode parentTopic;
	private Set<TopicNode> childrenTopics;
	
	
	public TopicNode(String resourceUri, String label, String description, TopicNode parentTopic) {
		super();
		this.resourceUri = resourceUri;
		this.label = label;
		this.description = description;
		this.parentTopic = parentTopic;
		this.childrenTopics = new HashSet<>();
	}


	public String getLabel() {
		return label;
	}


	public void setLabel(String label) {
		this.label = label;
	}

	@JsonIgnore
	public TopicNode getParentTopic() {
		return parentTopic;
	}


	public void setParentTopic(TopicNode parentTopic) {
		this.parentTopic = parentTopic;
	}


	public Set<TopicNode> getChildrenTopics() {
		return childrenTopics;
	}


	public void setChildrenTopics(Set<TopicNode> childrenTopics) {
		this.childrenTopics = childrenTopics;
	}


	public String getResourceUri() {
		return resourceUri;
	}


	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}
	
	public void addChildren(TopicNode childNode) {
		this.childrenTopics.add(childNode);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((resourceUri == null) ? 0 : resourceUri.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TopicNode other = (TopicNode) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (resourceUri == null) {
			if (other.resourceUri != null)
				return false;
		} else if (!resourceUri.equals(other.resourceUri))
			return false;
		return true;
	}
	
}
