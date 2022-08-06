package ccjava.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

public class Definition {
	private static int NEXT_ID = 1;
	private int id;
	private String name;
	private SourceFile sourceFile;
	private CharacterInterval position;
	private String bindingKey;
	private boolean methodDefinition;
	private boolean typeDefinition;
	private Set<DiffRegion> enclosingDiffRegions;
	
	public Definition(String name, SourceFile sourceFile, CharacterInterval position, String bindingKey) {
		Validate.notBlank(name);
		Validate.notNull(sourceFile);
		Validate.notNull(position);
		Validate.notBlank(bindingKey);
		
		this.name = name;
		this.position = position;
		this.bindingKey = bindingKey;
		this.methodDefinition = false;
		this.typeDefinition = false;
		
		this.sourceFile = sourceFile;
		this.sourceFile.addDefinition(this);
		
		this.enclosingDiffRegions = new HashSet<DiffRegion>();
	}
	
	public int getId() {
		if (id <= 0) {
			this.id = NEXT_ID++;
		}
		
		return this.id;
	}

	public String getName() {
		return this.name;
	}
	
	public SourceFile getSourceFile() {
		return this.sourceFile;
	}
	
	public CharacterInterval getPosition() {
		return this.position;
	}
	
	public String getBindingKey() {
		return this.bindingKey;
	}
	
	// for an OOP language like Java, if this is either a type or a method definition
	public boolean isOrganizationalUnit() {
		return this.methodDefinition || this.typeDefinition;
	}
	
	public boolean isMethodDefinition() {
		return this.methodDefinition;
	}

	public void setMethodDefinition(boolean methodDefinition) {
		this.methodDefinition = methodDefinition;
	}
	
	public boolean isTypeDefinition() {
		return this.typeDefinition;
	}

	public void setTypeDefinition(boolean typeDefinition) {
		this.typeDefinition = typeDefinition;
	}
	
	public Set<DiffRegion> getEnclosingDiffRegions() {
		return Collections.unmodifiableSet(this.enclosingDiffRegions);
	}
	
	public void addEnclosingDiffRegion(DiffRegion dr) {
		Validate.notNull(dr);
		
		this.enclosingDiffRegions.add(dr);
	}
	
	public boolean isInsideADiffRegion() {
		return !this.enclosingDiffRegions.isEmpty();
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
		if (!(obj instanceof Definition))
			return false;
		Definition other = (Definition) obj;
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
		return String.format("\"%s\" @ %s:%s | %s", name, sourceFile.getPath(), position.toString(), bindingKey);
	}
}
