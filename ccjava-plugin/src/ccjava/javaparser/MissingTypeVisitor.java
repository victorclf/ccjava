package ccjava.javaparser;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.WildcardType;

import ccjava.DEFINES;

public class MissingTypeVisitor extends ASTVisitor {
	private Map<String, MissingType> missingTypes;
	private String cUnitPackagePath;
	private Map<String, String> typeNameToPackagePath;
	
	public MissingTypeVisitor(Map<String, MissingType> missingTypes) {
		Validate.notNull(missingTypes);
		
		this.missingTypes = missingTypes;
		this.cUnitPackagePath = "";
		this.typeNameToPackagePath = new HashMap<String, String>();
	}
	
	@Override
	public boolean visit(PackageDeclaration node) {
		this.cUnitPackagePath = node.getName().getFullyQualifiedName();
		return true;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		if (!node.isOnDemand() && node.getName().isQualifiedName()) {
			QualifiedName nodeQualifiedName = (QualifiedName) node.getName();
			String packagePath = nodeQualifiedName.getQualifier().getFullyQualifiedName();
			String name = nodeQualifiedName.getName().getFullyQualifiedName();
			this.typeNameToPackagePath.put(name, packagePath);
		}
		return true;
	}
	
	@Override
	public boolean visit(ArrayType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(IntersectionType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(NameQualifiedType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(ParameterizedType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(PrimitiveType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(QualifiedType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(SimpleType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(UnionType node) {
		handle(node);
		return true;
	}
	
	@Override
	public boolean visit(WildcardType node) {
		handle(node);
		return true;
	}
	
	private MissingType handle(ArrayType node) {
		return handleType(node.getElementType());
	}
	
	private MissingType handle(IntersectionType node) {
		Type firstNode = null;
		MissingType firstNodeMT = null;
		for (Object obj : node.types()) {
			Type t = (Type) obj;
			MissingType mt = handleType(t);
			if (firstNode == null) {
				firstNode = t;
				firstNodeMT = mt;
			}
		}
		return firstNodeMT;
	}
	
	private MissingType handle(NameQualifiedType node) {
		if (node.resolveBinding() == null) {
			String packagePath = node.getQualifier().getFullyQualifiedName();
			if (isTypeName(packagePath)) {
				packagePath = convertQualifierFromTypeNameToPackagePath(packagePath);
			}
			String name = node.getName().getFullyQualifiedName();
			return addMissingType(packagePath, name);
		}
		return null;
	}
	
	private MissingType handle(ParameterizedType node) {
		MissingType mt = handleType(node.getType());
		if (mt != null) {
			mt.setNumGenericParameters(node.typeArguments().size());
		}
		return mt;
	}
	
	private MissingType handle(PrimitiveType node) {
		return null; // primitive types will never be missing
	}
	
	private MissingType handle(QualifiedType node) {
		if (node.resolveBinding() == null) {
			String packagePath = node.getQualifier().toString();
			if (node.getQualifier().isSimpleType()) {
				packagePath = ((SimpleType)node.getQualifier()).getName().getFullyQualifiedName();
			}
			if (isTypeName(packagePath)) {
				packagePath = convertQualifierFromTypeNameToPackagePath(packagePath);
			}
			
			String name = node.getName().getFullyQualifiedName();
			return addMissingType(packagePath, name);
		}
		return null;
	}
	
	private MissingType handle(SimpleType node) {
		if (node.resolveBinding() == null) {
			if (node.getName().isQualifiedName()) {
				QualifiedName nodeQualifiedName = (QualifiedName) node.getName();
				String packagePath = nodeQualifiedName.getQualifier().getFullyQualifiedName();
				if (isTypeName(packagePath)) {
					// The qualifier is not a package path but actually the parent type.
					// We need to append the package path to the parent type then.
					packagePath = convertQualifierFromTypeNameToPackagePath(packagePath);
				}
				String name = nodeQualifiedName.getName().getFullyQualifiedName();
				return addMissingType(packagePath, name);
			} else {
				String name = node.getName().getFullyQualifiedName();
				String packagePath = getPackagePathForTypeName(name);
				return addMissingType(packagePath, name);
			}
		}
		return null;
	}
	
	private MissingType handle(UnionType node) {
		Type firstNode = null;
		MissingType firstNodeMT = null;
		for (Object obj : node.types()) {
			Type t = (Type) obj;
			MissingType mt = handleType(t);
			if (firstNode == null) {
				firstNode = t;
				firstNodeMT = mt;
			}
		}
		return firstNodeMT;
	}
	
	private MissingType handle(WildcardType node) {
		if (node.getBound() != null && node.resolveBinding() == null) {
			return handleType(node.getBound());
		}
		return null;
	}
	
	private MissingType handleType(Type node) {
		if (node.isArrayType()) {
			return handle((ArrayType) node);
		} else if (node.isIntersectionType()) {
			return handle((IntersectionType) node);
		} else if (node.isNameQualifiedType()) {
			return handle((NameQualifiedType) node);
		} else if (node.isParameterizedType()) {
			return handle((ParameterizedType) node);
		} else if (node.isPrimitiveType()) { 
			return handle((PrimitiveType) node);
		} else if (node.isQualifiedType()) {
			return handle((QualifiedType) node);
		} else if (node.isSimpleType()) { 
			return handle((SimpleType) node);
		} else if (node.isUnionType()) {
			return handle((UnionType) node);
		} else if (node.isWildcardType()) {
			return handle((WildcardType) node);
		}
		
		return null;
	}

	/*
	 * Function used when a name qualifier is not a package path but actually the parent type. When this happens,
	 * we need to append the package path to the parent type then.
	 * 
	 * For example, consider the class hierarchy: Map <- Entry <- Subentry. We can have a reference "Map.Entry.Subentry"
	 * which is not a package path but the name of the type. This function would add the package path to it: 
	 * "org.java.Map.Entry.Subentry".
	 */
	private String convertQualifierFromTypeNameToPackagePath(String nameQualifier) {
		String parentTypeName = nameQualifier.split("\\.")[0];
		return getPackagePathForTypeName(parentTypeName) + "." + nameQualifier;
	}
	
	private MissingType addMissingType(String packagePath, String name) {
		String mtId = MissingType.getId(packagePath, name);
		
		if (this.missingTypes.containsKey(mtId)) {
			return this.missingTypes.get(mtId);
		}
		
		MissingType mt = new MissingType(packagePath, name);
		if (packagePathReferencesType(packagePath)) {
			MissingType parentMT = createParentMissingTypeFromPath(packagePath);
			parentMT.addChild(mt);
		}
		this.missingTypes.put(mt.getId(), mt);
		log("added missing type", mt);
		return mt;
	}
	
	private MissingType createParentMissingTypeFromPath(String packagePath) {
		Validate.notBlank(packagePath);
		
		String[] packagePathFields = packagePath.split("\\.");
		for (int i = packagePathFields.length - 1; i >= 0; --i) {
			String f = packagePathFields[i];
			if (isTypeName(f)) {
				String parentPackagePath = StringUtils.join(packagePathFields, ".", 0, i); // everything before f
				String parentName = f;
				return addMissingType(parentPackagePath, parentName);
			}
		}
		
		throw new RuntimeException("can't create parent missing type from path:" + packagePath);
	}

	private boolean packagePathReferencesType(String packagePath) {
		if (StringUtils.isBlank(packagePath)) {
			return false;
		}
		
		String[] packagePathFields = packagePath.split("\\.");
		for (int i = packagePathFields.length - 1; i >= 0; --i) {
			String f = packagePathFields[i];
			if (isTypeName(f)) {
				return true;
			}
		}
		return false;
	}

	private boolean isTypeName(String packagePathField) {
		if (StringUtils.isBlank(packagePathField)) {
			return false;
		}
		String firstChar = packagePathField.substring(0, 1);
		return firstChar.equals(firstChar.toUpperCase());
	}
	
	private void log(String msg) {
		if (DEFINES.LOG_MISSING_TYPE_VISITOR) {
			System.out.printf("%s%n", msg);
		}
	}

	private void log(String msg, MissingType mt) {
		if (DEFINES.LOG_MISSING_TYPE_VISITOR) {
			System.out.printf("%s %s%n", msg, mt);
		}
	}

	private String getPackagePathForTypeName(String name) {
		return this.typeNameToPackagePath.containsKey(name) 
				? this.typeNameToPackagePath.get(name)
				: this.cUnitPackagePath;
	}
}
