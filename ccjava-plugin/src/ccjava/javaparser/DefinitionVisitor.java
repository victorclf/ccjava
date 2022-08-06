package ccjava.javaparser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import ccjava.DEFINES;
import ccjava.model.CharacterInterval;
import ccjava.model.Definition;
import ccjava.model.SourceFile;

public class DefinitionVisitor extends ASTVisitor {
	private SourceFile sourceFile;
	private List<Definition> definitions;
	
	public DefinitionVisitor(SourceFile sourceFile) {
		Validate.notNull(sourceFile);
		
		this.sourceFile = sourceFile;
		this.definitions = new ArrayList<Definition>();
	}
	
	public List<Definition> getDefinitions() {
		return this.definitions;
	}
	
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			definitions.add(def);
			log("annotation definition", def);
		} else {
			log("skipped annotation definition (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			definitions.add(def);
			log("annotation member definition", def);
		} else {
			log("skipped annotation member definition (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(EnumConstantDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveVariable() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveVariable());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			definitions.add(def);
			log("enum constant definition", def);
		} else {
			log("skipped enum constant definition (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(EnumDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			def.setTypeDefinition(true);
			definitions.add(def);
			log("enum definition", def);
		} else {
			log("skipped enum definition (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(FieldDeclaration node) {
		for (Object fragObj : node.fragments()) {
			VariableDeclarationFragment frag = (VariableDeclarationFragment) fragObj;
			String name = frag.getName().getFullyQualifiedName();
			CharacterInterval position = getPositionFromNode(node);
			
			if (frag.resolveBinding() == null) {
				log("skipped field definition (no binding): " + name);
				continue;
			}
			
			String bindingKey = getBindingKey(frag.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			definitions.add(def);
			log("field definition", def);
		}
		return true;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			def.setMethodDefinition(true);
			definitions.add(def);
			log("method definition", def);
		} else {
			log("skipped method definition (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			definitions.add(def);
			log("singlevariable definition", def);
		} else {
			log("skipped singlevariable definition (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(TypeDeclaration node) {
		String name = node.getName().getFullyQualifiedName();

		if (node.resolveBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			def.setTypeDefinition(true);
			definitions.add(def);
			log("type definition", def);
		} else {
			log("skipped type definition (no binding): " + name);
		}
		
		return true;
	}
	
//	@Override
//	public boolean visit(TypeParameter node) {
//		String name = node.getName().getFullyQualifiedName();
//
//		if (node.resolveBinding() != null) {
//			CharacterInterval position = getPositionFromNode(node);
//			String bindingKey = getBindingKey(node.resolveBinding());
//			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
//			definitions.add(def);
//			log("type parameter definition", def);
//		} else {
//			log("skipped type parameter (no binding): " + name);
//		}
//		
//		return true;
//	}
	
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		for (Object fragObj : node.fragments()) {
			VariableDeclarationFragment frag = (VariableDeclarationFragment) fragObj;
			String name = frag.getName().getFullyQualifiedName();
			CharacterInterval position = node.fragments().size() > 1 
					? getPositionFromNode(frag)
					: getPositionFromNode(node);
			
			if (frag.resolveBinding() == null) {
				log("skipped variable definition (no binding): " + name);
				continue;
			}
			
			String bindingKey = getBindingKey(frag.resolveBinding());
			Definition def = new Definition(name, this.sourceFile, position, bindingKey);
			definitions.add(def);
			log("variable definition", def);
		}
		return true;
	}
	
	private void log(String msg) {
		if (DEFINES.LOG_DEFINITION_VISITOR) {
			System.out.println(msg);
		}
	}
	
	private void log(String msg, Definition def) {
		if (DEFINES.LOG_DEFINITION_VISITOR) {
			System.out.printf("%s %s%n", msg, def.toString());
		}
	}

	private CharacterInterval getPositionFromNode(ASTNode node) {
		return CharacterInterval.fromPositionAndLength(node.getStartPosition(), node.getLength());
	}
	
	private String getBindingKey(IBinding binding) {
		return BindingKeyParser.parse(binding.getKey());
	}
}
