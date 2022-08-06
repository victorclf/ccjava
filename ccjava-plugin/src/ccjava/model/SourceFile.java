package ccjava.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;

import ccjava.diffparser.UnifiedDiffRegion;

public class SourceFile {
	private String path;
	private String filePath;
	private LineToCharacterIntervalConverter lineToCharConverter;
	private Set<Definition> definitions;
	private Set<Use> uses;
	private Set<DiffRegion> diffRegions;
	private Set<Comment> comments;
	private List<String> contents;
	
	public SourceFile(String path, String filePath, LineToCharacterIntervalConverter lineToCharConverter) {
		Validate.notBlank(path);
		Validate.notBlank(filePath);
		Validate.notNull(lineToCharConverter);
		
		this.path = path;
		this.filePath = filePath;
		this.lineToCharConverter = lineToCharConverter;
		this.definitions = new LinkedHashSet<Definition>();
		this.uses = new LinkedHashSet<Use>();
		this.diffRegions = new LinkedHashSet<DiffRegion>();
		this.comments = new LinkedHashSet<Comment>();
	}
	
	public void addDefinitions(Collection<Definition> definitions) {
		Validate.notNull(definitions);
		
		for (Definition d : definitions) {
			addDefinition(d);
		}
	}
	
	public void addDefinition(Definition d) {
		Validate.notNull(d);
		
		this.definitions.add(d);
	}
	
	public void addUses(Collection<Use> uses) {
		Validate.notNull(uses);
		
		for (Use u : uses) {
			addUse(u);
		}
	}
	
	public void addUse(Use u) {
		Validate.notNull(u);
		
		this.uses.add(u);
	}
	
	public void addDiffRegions(Collection<UnifiedDiffRegion> uniDiffRegions) {
		for (UnifiedDiffRegion udr : uniDiffRegions) {
			addDiffRegion(udr);
		}
	}
	
	public void addDiffRegion(UnifiedDiffRegion uniDiffRegion) {
		UnifiedDiffRegionToDiffRegionConverter c = new UnifiedDiffRegionToDiffRegionConverter(
				uniDiffRegion, lineToCharConverter, this, definitions, uses);
		this.diffRegions.addAll(c.convert());
	}
	
	public void addComments(Collection<Comment> comments) {
		Validate.notNull(comments);
		
		for (Comment u : comments) {
			addComment(u);
		}
	}
	
	public void addComment(Comment u) {
		Validate.notNull(u);
		
		this.comments.add(u);
	}
	
	public LineToCharacterIntervalConverter getLineToCharConverter() {
		return lineToCharConverter;
	}

	public Set<Definition> getDefinitions() {
		return definitions;
	}

	public Set<Use> getUses() {
		return uses;
	}

	public Set<DiffRegion> getDiffRegions() {
		return Collections.unmodifiableSet(diffRegions);
	}
	
	public Set<Comment> getComments() {
		return Collections.unmodifiableSet(comments);
	}

	public String getPath() {
		return this.path;
	}
	
	public List<String> getContents() {
		if (contents == null) {
			contents = readContents();
		}
		return Collections.unmodifiableList(contents);
	}
	
	private List<String> readContents() {
		return readLines(new File(this.filePath));
	}
	
	private List<String> readLines(File f) {
		List<String> fileLines = null;
		try {
			fileLines = FileUtils.readLines(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return fileLines;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((path == null) ? 0 : path.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SourceFile))
			return false;
		SourceFile other = (SourceFile) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.path;
	}
}
