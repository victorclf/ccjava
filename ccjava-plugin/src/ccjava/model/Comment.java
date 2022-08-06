package ccjava.model;

import org.apache.commons.lang3.Validate;

public class Comment {
	private SourceFile sourceFile;
	private CharacterInterval position;
	
	public Comment(SourceFile sourceFile, CharacterInterval position) {
		Validate.notNull(sourceFile);
		Validate.notNull(position);
		
		this.position = position;
		this.sourceFile = sourceFile;
		this.sourceFile.addComment(this);
	}
	
	public SourceFile getSourceFile() {
		return this.sourceFile;
	}
	
	public CharacterInterval getPosition() {
		return this.position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((position == null) ? 0 : position.hashCode());
		result = prime * result
				+ ((sourceFile == null) ? 0 : sourceFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Comment))
			return false;
		Comment other = (Comment) obj;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("%s @ %s", position.toString(), sourceFile.getPath());
	}
}
