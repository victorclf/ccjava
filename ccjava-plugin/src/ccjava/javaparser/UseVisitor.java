package ccjava.javaparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import ccjava.DEFINES;
import ccjava.model.CharacterInterval;
import ccjava.model.Definition;
import ccjava.model.SourceFile;
import ccjava.model.Use;

public class UseVisitor extends ASTVisitor {
	private SourceFile sourceFile;
	private List<Use> uses;
	private boolean ignoreUsesWithoutAssociatedDefinitions;
	private Map<String, Definition> bindingKeyToDefinitionMap;
	
	public UseVisitor(SourceFile sourceFile, Collection<Definition> definitions, 
			boolean ignoreUsesWithoutAssociatedDefinitions) {
		Validate.notNull(sourceFile);
		Validate.notNull(definitions);
		
		this.sourceFile = sourceFile;
		this.uses = new ArrayList<Use>();
		this.ignoreUsesWithoutAssociatedDefinitions = ignoreUsesWithoutAssociatedDefinitions;
		
		bindingKeyToDefinitionMap = new HashMap<String, Definition>();
		for (Definition d : definitions) {
			bindingKeyToDefinitionMap.put(d.getBindingKey(), d);
		}
	}

	public List<Use> getUses() {
		return this.uses;
	}
	
	// This is a special case for constructors because the SimpleName node inside a constructor invocation 
	// points to the class instead of the constructor. Here we make the use point to the constructor method instead
	// of to the type.
	@Override
	public boolean visit(ClassInstanceCreation node) {
		String name = node.getType().toString();

		if (node.resolveConstructorBinding() != null) {
			CharacterInterval position = getPositionFromNode(node);
			String bindingKey = getBindingKey(node.resolveConstructorBinding());
			Definition associatedDefinition = findAssociatedDefinition(bindingKey);
			
			if (ignoreUsesWithoutAssociatedDefinitions && associatedDefinition == null) {
				log(String.format("skipped constructor use (no associated def): %s | %s | %s", name, position, bindingKey));
			} else {
				Use u = new Use(name, sourceFile, position, bindingKey, associatedDefinition);
				log("constructor use", u);
				this.uses.add(u);
			}
		} else {
			log("skipped constructor use (no binding): " + name);
		}
		
		return true;
	}
	
	@Override
	public boolean visit(SimpleName node) {
		if (!node.isDeclaration()) { // we only care about uses here
			// Constructor calls are handled in another method, but since we are ignoring type uses, we don't need such check.
			//&& !isNodeParentsOfType(node, ASTNode.CLASS_INSTANCE_CREATION, 2)) { 
				String name = node.getFullyQualifiedName();
				CharacterInterval position = getPositionFromNode(node);
				
				if (node.resolveBinding() != null) {
					String bindingKey = getBindingKey(node.resolveBinding());
					Definition associatedDefinition = findAssociatedDefinition(bindingKey);
					
					if (ignoreUsesWithoutAssociatedDefinitions && associatedDefinition == null) {
						log(String.format("skipped simplename use (no associated def): %s | %s | %s", name, position, bindingKey));
					} else if (associatedDefinition != null && associatedDefinition.isTypeDefinition()) {
						log(String.format("skipped simplename use (type uses are not considered): %s | %s | %s", name, position, bindingKey));
					} else {
						Use u = new Use(name, sourceFile, position, bindingKey, associatedDefinition);
						log("simplename use", u);
						this.uses.add(u);
					}
				} else {
					log(String.format("skipped simplename use (no binding): %s | %s", name, position));
				}
		}
		
		return true;
	}
	
	private boolean isNodeParentsOfType(ASTNode node, int nodeType, int levels) {
		ASTNode parent = node;
		while (levels-- > 0) {
			parent = parent.getParent();
			if (parent == null) {
				return false;
			} else if(parent.getNodeType() == nodeType) {
				return true;
			}
		}
		return false;
	}

	private void log(String msg) {
		if (DEFINES.LOG_USE_VISITOR) {
			System.out.println(msg);
		}
	}
	
	private void log(String msg, Use use) {
		if (DEFINES.LOG_USE_VISITOR) {
			//System.out.printf("%s / %s @ %s%n", name, bindingKey, position);
			System.out.printf("%s %s%n", msg, use.toString());
			if (use.getAssociatedDefinition() != null) {
				System.out.println("---> " + use.getAssociatedDefinition());
			}
		}
	}
	
	private Definition findAssociatedDefinition(String bindingKey) {
		return this.bindingKeyToDefinitionMap.get(bindingKey);
	}
	
	private CharacterInterval getPositionFromNode(ASTNode node) {
		return CharacterInterval.fromPositionAndLength(node.getStartPosition(), node.getLength());
	}
	
	private String getBindingKey(IBinding binding) {
		return BindingKeyParser.parse(binding.getKey());
	}
}
