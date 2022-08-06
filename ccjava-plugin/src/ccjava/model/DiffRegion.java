package ccjava.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ccjava.diffparser.LineInterval;

public class DiffRegion {
	private static int NEXT_ID = 1;
	private int id;
	private SourceFile sourceFile;
	private CharacterInterval characterSpan;
	private LineInterval lineSpan;
	private Set<Definition> definitionsInside;
	private Set<Use> usesInside;
	private Definition enclosingMethod;
	
	public DiffRegion(SourceFile sourceFile, CharacterInterval characterSpan, LineInterval lineSpan, Set<Definition> definitionsInside, Set<Use> usesInside) {
		Validate.notNull(sourceFile);
		Validate.notNull(characterSpan);
		Validate.notNull(lineSpan);
		this.sourceFile = sourceFile;
		this.characterSpan = characterSpan;
		this.lineSpan = lineSpan;
		setDefinitionsInside(definitionsInside);
		setUsesInside(usesInside);
		this.enclosingMethod = getOutermostEnclosingMethod();
	}
	
	public int getId() {
		if (id <= 0) {
			this.id = NEXT_ID++;
		}
		
		return this.id;
	}

	private void setDefinitionsInside(Set<Definition> definitionsInside) {
		Validate.notNull(definitionsInside);		
		this.definitionsInside = definitionsInside;
		for (Definition d : this.definitionsInside) {
			d.addEnclosingDiffRegion(this);
		}
	}
	
	private Definition getOutermostEnclosingMethod() {
		int outermostEnclosingMethodStartPosition = Integer.MAX_VALUE;
		Definition outermostEnclosingMethod = null;
		for (Definition d : this.definitionsInside) {
			if (d.isMethodDefinition()) {
				if (d.getPosition().getFirstCharacterPosition() < outermostEnclosingMethodStartPosition) {
					outermostEnclosingMethod = d;
					outermostEnclosingMethodStartPosition = d.getPosition().getFirstCharacterPosition();
				}
			}
		}
		return outermostEnclosingMethod;
	}

	private void setUsesInside(Set<Use> usesInside) {
		Validate.notNull(usesInside);
		this.usesInside = usesInside;
		for (Use u : this.usesInside) {
			u.addEnclosingDiffRegion(this);
		}
	}
	
	public Set<Definition> getDefinitionsInside() {
		return Collections.unmodifiableSet(definitionsInside);
	}

	public Set<Use> getUsesInside() {
		return Collections.unmodifiableSet(usesInside);
	}

	public boolean contains(Definition definition) {
		return this.definitionsInside.contains(definition);
	}
	
	public boolean contains(Use use) {
		return this.usesInside.contains(use);
	}

	public SourceFile getSourceFile() {
		return this.sourceFile;
	}

	public CharacterInterval getCharacterSpan() {
		return this.characterSpan;
	}
	
	public LineInterval getLineSpan() {
		return this.lineSpan;
	}

	public boolean sameEnclosingMethod(DiffRegion other) {
		Validate.notNull(other);
		
		if (this.enclosingMethod == null || other.enclosingMethod == null) {
			return false;
		}
		
		return this.enclosingMethod.equals(other.enclosingMethod);
	}
	
	public Definition getEnclosingMethod() {
		return this.enclosingMethod;
	}
	
	public List<String> getContents() {
		return this.sourceFile.getContents().subList(this.lineSpan.getFirstLineNumber() - 1,
				this.lineSpan.getLastLineNumber());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + ((characterSpan == null) ? 0 : characterSpan.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DiffRegion))
			return false;
		DiffRegion other = (DiffRegion) obj;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		if (characterSpan == null) {
			if (other.characterSpan != null)
				return false;
		} else if (!characterSpan.equals(other.characterSpan))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("%s @ %s", this.lineSpan, this.sourceFile);
	}
}
