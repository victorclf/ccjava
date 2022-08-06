package ccjava.javaparser;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ccjava.model.Definition;
import ccjava.model.SourceFile;
import ccjava.model.Use;

public class ParserResults {
	public Map<String, SourceFile> sourceFiles;
	public Set<Definition> definitions;
	public Set<Use> uses;
	
	public ParserResults(Map<String, SourceFile> sourceFiles, Set<Definition> definitions, Set<Use> use) {
		Validate.notNull(sourceFiles);
		Validate.notNull(definitions);
		Validate.notNull(use);
		
		this.sourceFiles = sourceFiles;
		this.definitions = definitions;
		this.uses = use;
	}
}
