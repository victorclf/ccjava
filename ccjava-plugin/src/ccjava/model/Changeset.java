package ccjava.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ccjava.diffparser.UnifiedDiffRegion;

public class Changeset {
	private Map<String, SourceFile> sourceFiles;

	public Changeset(Map<String, SourceFile> sourceFiles) {
		Validate.notNull(sourceFiles);
		
		this.sourceFiles = sourceFiles;
	}
	
	public boolean isEmpty() {
		return this.sourceFiles.isEmpty();
	}
	
	public void addDiffRegions(List<UnifiedDiffRegion> udrList) {
		Validate.notNull(udrList);
		
		for (UnifiedDiffRegion udr : udrList) {
			SourceFile sf = getSourceFile(udr.getPath());
			if (sf != null) { // if the whole file was deleted, there might be a .patch file without a matching sourcefile
				sf.addDiffRegion(udr);
			}
		}
	}
	
	public Set<DiffRegion> getDiffRegions() {
		Set<DiffRegion> drs = new HashSet<DiffRegion>();
		for (SourceFile sf : getSourceFiles()) {
			for (DiffRegion d : sf.getDiffRegions()) {
				drs.add(d);
			}
		}
		return drs;
	}
	
	public SourceFile getSourceFile(String s) {
		Validate.notBlank(s);
		
		return this.sourceFiles.get(s);
	}
	
	public Collection<SourceFile> getSourceFiles() {
		return Collections.unmodifiableCollection(this.sourceFiles.values());
	}
	
	public Set<Definition> getDefinitions() {
		Set<Definition> defs = new HashSet<Definition>();
		for (SourceFile sf : getSourceFiles()) {
			for (Definition d : sf.getDefinitions()) {
				defs.add(d);
			}
		}
		return defs;
	}
	
	public Set<Use> getUses() {
		Set<Use> uses = new HashSet<Use>();
		for (SourceFile sf : getSourceFiles()) {
			for (Use u : sf.getUses()) {
				uses.add(u);
			}
		}
		return uses;
	}
}
