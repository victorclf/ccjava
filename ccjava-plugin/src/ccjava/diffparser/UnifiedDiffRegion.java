package ccjava.diffparser;

import org.apache.commons.lang3.Validate;

public class UnifiedDiffRegion {
	private String path;
	private LineInterval lines;
	
	public UnifiedDiffRegion(String path, LineInterval lines) {
		Validate.notBlank(path);
		Validate.notNull(lines);
		
		this.path = path;
		this.lines = lines;
	}

	public String getPath() {
		return path;
	}

	public LineInterval getLines() {
		return lines;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lines == null) ? 0 : lines.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UnifiedDiffRegion))
			return false;
		UnifiedDiffRegion other = (UnifiedDiffRegion) obj;
		if (lines == null) {
			if (other.lines != null)
				return false;
		} else if (!lines.equals(other.lines))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ %s", this.lines, this.path);
	}
}
