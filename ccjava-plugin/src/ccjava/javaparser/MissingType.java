package ccjava.javaparser;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class MissingType {
	private String packagePath;
	private String name;
	private MissingType parentType;
	private Set<MissingType> nestedTypes;
	private int numGenericParameters;

	public MissingType(String packagePath, String name) {
		Validate.notBlank(packagePath);
		Validate.notBlank(name);
		
		this.packagePath = packagePath;
		this.name = name;
		this.nestedTypes = new HashSet<MissingType>();
		this.parentType = null;
		this.numGenericParameters = 0;
	}
	
	public static String getId(String packagePath, String name) {
		if (!StringUtils.isBlank(packagePath)) {
			return String.format("%s.%s", packagePath, name);
		} else {
			return name;
		} 
	}
	
	public String getId() {
		 return MissingType.getId(packagePath, name);
	}

	public String getPackagePath() {
		return packagePath;
	}

	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isNested() {
		return parentType != null;
	}
	
	public void addChild(MissingType nestedType) {
		Validate.notNull(nestedType);
		
		this.nestedTypes.add(nestedType);
		nestedType.setParent(this);
	}
	
	private void setParent(MissingType parentType) {
		assert(this.parentType == null);
		this.parentType = parentType;
	}
	
	public void setNumGenericParameters(int numGenericParameters) {
		this.numGenericParameters = numGenericParameters;
	}
	
	public String generateCode() {
		StringBuilder code = new StringBuilder();
		
		if (isNested()) {
			code.append(generateNestedTypeCode());
		} else {
			if (!StringUtils.isBlank(this.packagePath)) {
				code.append(generatePackageDeclarationCode());
			}
			code.append(generateRootTypeCode());
		}
		
		return code.toString();
	}

	private String generatePackageDeclarationCode() {
		return String.format("package %s;%n%n", this.packagePath);
	}

	private String generateRootTypeCode() {
		return String.format("public class %s {%n%s}%n", generateName(), generateTypeBodyCode());
	}
	
	private String generateNestedTypeCode() {
		return String.format("public static class %s {%n%s}%n", generateName(), generateTypeBodyCode());
	}
	
	private String generateName() {
		if (this.numGenericParameters > 0) {
			StringBuilder params = new StringBuilder("<");
			for (int i = 0; i < numGenericParameters; ++i) {
				char paramLetter = (char) ('A' + (i % 26));
				String param = StringUtils.repeat(paramLetter, (i / 26) + 1);
				params.append(param).append(", ");
			}
			params.delete(params.length() - 2, params.length());
			params.append(">");
			return this.name + params.toString();
		} else {
			return this.name;
		}
	}
	
	private String generateTypeBodyCode() {
		StringBuilder code = new StringBuilder();
		for (MissingType nt : this.nestedTypes) {
			code.append(nt.generateCode());
		}
		return code.toString();
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((packagePath == null) ? 0 : packagePath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MissingType))
			return false;
		MissingType other = (MissingType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (packagePath == null) {
			if (other.packagePath != null)
				return false;
		} else if (!packagePath.equals(other.packagePath))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getId();
	}
}
