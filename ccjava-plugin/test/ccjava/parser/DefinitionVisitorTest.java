package ccjava.parser;

import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Test;

import ccjava.javaparser.Parser;
import ccjava.model.Changeset;
import ccjava.model.Definition;

import static org.junit.Assert.*;

public class DefinitionVisitorTest {
	@Test
	public void testAnnotationDefinition() {
		Changeset cs = parse("test/examples/annotation");
		Assert.assertTrue(containsDefinition(cs, "Testable.java", "Testable", 28, 103));
	}
	
	@Test
	public void testAnnotationMemberDefinition() {
		Changeset cs = parse("test/examples/annotation");
		Assert.assertTrue(containsDefinition(cs, "Testable.java", "id", 58, 66));
		Assert.assertTrue(containsDefinition(cs, "Testable.java", "tester", 69, 101));
	}
	
	@Test
	public void testClazzDefinition() {
		Changeset cs = parse("test/examples/toy_example");
		Assert.assertTrue(containsDefinition(cs, "Person.java", "Person", 41, 568, true));
	}
	
	@Test
	public void testClazzDefinition2() {
		Changeset cs = parse("test/examples/toy_example_multi_file");
		Assert.assertTrue(containsDefinition(cs, "Entity.java", "Entity", 41, 179, true));
	}
	
	@Test
	public void testClazzDefinitionDoesNotIncludeMocks() {
		Changeset cs = parse("test/examples/missingtypes");
		Assert.assertTrue(containsDefinition(cs, "Player.java", "Player", 90, 540, true));
		
		Assert.assertFalse(containsDefinition(cs, "Actor.java", "Actor"));
		Assert.assertFalse(containsDefinition(cs, "Entity.java", "Entity"));
		Assert.assertFalse(containsDefinition(cs, "Sprite.java", "Sprite"));
		Assert.assertFalse(containsDefinition(cs, "Position.java", "Position"));
	}
	
	@Test
	public void testEnumDefinition() {
		Changeset cs = parse("test/examples/enum");
		Assert.assertTrue(containsDefinition(cs, "Color.java", "Color", 32, 308, true));
	}
	
	@Test
	public void testEnumConstantDefinition() {
		Changeset cs = parse("test/examples/enum");
		Assert.assertTrue(containsDefinition(cs, "Color.java", "BLUE", 53, 67));
		Assert.assertTrue(containsDefinition(cs, "Color.java", "RED", 71, 84));
		Assert.assertTrue(containsDefinition(cs, "Color.java", "GREEN", 88, 103));
	}
	
	@Test
	public void testFieldDefinition() {
		Changeset cs = parse("test/examples/toy_example");
		Assert.assertTrue(containsDefinition(cs, "Person.java", "firstName", 60, 84, false));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "lastName", 90, 113, false));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "age", 119, 134, false));
	}
	
	@Test
	public void testFieldDefinitionGenericType() {
		Changeset cs = parse("test/examples/generics-dropwizard-metrics-779");
		
		Assert.assertTrue(containsDefinition(cs, "InstrumentedCloseableHttpAsyncClient.java", "consumer", 5822, 5873));
	}
	
	@Test
	public void testLocalVariableDefinition() {
		Changeset cs = parse("test/examples/localvars");
		Assert.assertTrue(containsDefinition(cs, "PrettyPrinter.java", "sBuilder", 196, 240));
		Assert.assertTrue(containsDefinition(cs, "PrettyPrinter.java", "sum", 248, 254));
		Assert.assertTrue(containsDefinition(cs, "PrettyPrinter.java", "doubleSum", 260, 272));
	}
	
	@Test
	public void testMethodDefinition() {
		Changeset cs = parse("test/examples/toy_example");
		Assert.assertTrue(containsDefinition(cs, "Person.java", "getFirstName", 141, 202, true));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "setFirstName", 209, 294, true));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "getLastName", 301, 360, true));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "setLastName", 367, 448, true));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "getAge", 455, 501, true));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "setAge", 508, 566, true));
	}
	
	@Test
	public void testMethodDefinitionConstructor() {
		Changeset cs = parse("test/examples/toy_example_multi_file");
		Assert.assertTrue(containsDefinition(cs, "Entity.java", "Entity", 88, 177, true));
	}
	
	@Test
	public void testMethodDefinitionInterface() {
		Changeset cs = parse("test/examples/interface");
		Assert.assertTrue(containsDefinition(cs, "Vehicle.java", "accelerate", 48, 74, true));
		Assert.assertTrue(containsDefinition(cs, "Vehicle.java", "brake", 77, 98));
		Assert.assertTrue(containsDefinition(cs, "Motorcycle.java", "Motorcycle", 112, 223));
	}
	
	@Test
	public void testMethodDefinitionMissingTypes() {
		Changeset cs = parse("test/examples/missingtypes");
		Assert.assertTrue(containsDefinition(cs, "Player.java", "Player", 199, 321, true));
		Assert.assertTrue(containsDefinition(cs, "Player.java", "getName", 326, 396));
		Assert.assertTrue(containsDefinition(cs, "Player.java", "draw", 401, 465));
		Assert.assertTrue(containsDefinition(cs, "Player.java", "move", 470, 538));
	}
	
	@Test
	public void testParameterDefinition() {
		Changeset cs = parse("test/examples/toy_example");
		Assert.assertTrue(containsDefinition(cs, "Person.java", "firstName", 234, 249));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "lastName", 391, 405));
		Assert.assertTrue(containsDefinition(cs, "Person.java", "age", 527, 533));
	}
	
	@Test 
	public void testParameterDefinitionInterfaceMethod() {
		Changeset cs = parse("test/examples/interface");
		Assert.assertTrue(containsDefinition(cs, "Vehicle.java", "speed", 64, 72));
		Assert.assertTrue(containsDefinition(cs, "Vehicle.java", "force", 88, 96));
	}
	
	@Test 
	public void testParameterDefinitionExceptionCatchClause() {
		Changeset cs = parse("test/examples/exception");
		Assert.assertTrue(containsDefinition(cs, "Danger.java", "e", 280, 290));
		Assert.assertFalse(containsDefinition(cs, "Danger.java", "e", 281, 290));
		Assert.assertFalse(containsDefinition(cs, "Danger.java", "e", 280, 289));
		Assert.assertFalse(containsDefinition(cs, "Danger.java", "ee", 280, 290));
	}
	
	@Test
	public void testPaths() {
		Changeset cs = parse("test/examples/toy_example");
		for (Definition d : cs.getDefinitions()) {
			Assert.assertTrue(d.getSourceFile().getPath().startsWith("somecompany/"));
		}
	}
	
	@Test 
	public void testTypeParameterDefinition() {
		Changeset cs = parse("test/examples/generics");
		Assert.assertFalse(containsDefinition(cs, "MyList.java", "T", 43, 43));
	}
	
	private boolean containsDefinition(Changeset cs, String partialFilePath, String name, int startPos, int endPos, boolean isOrganizationalUnit) {
		Definition d = getDefinition(cs, partialFilePath, name, startPos, endPos);
		return d != null && d.isOrganizationalUnit() == isOrganizationalUnit;
	}
	
	private boolean containsDefinition(Changeset cs, String partialFilePath, String name, int startPos, int endPos) {
		return getDefinition(cs, partialFilePath, name, startPos, endPos) != null;
	}
	
	private Definition getDefinition(Changeset cs, String partialFilePath, String name, int startPos, int endPos) {
		for (Definition d : cs.getDefinitions()) {
			if (d.getSourceFile().getPath().contains(partialFilePath)
				&& name.equals(d.getName())
				&& startPos == d.getPosition().getFirstCharacterPosition() 
				&& endPos == d.getPosition().getLastCharacterPosition()) {
					return d;
			}
		}
		return null;
	}
	
	private boolean containsDefinition(Changeset cs, String partialFilePath, String name) {
		for (Definition d : cs.getDefinitions()) {
			if (d.getSourceFile().getPath().contains(partialFilePath)
				&& name.equals(d.getName())) {
					return true;
			}
		}
		return false;
	}

	private Changeset parse(String directory) {
		Changeset cs = null;
		try {
			Parser p = new Parser(directory);
			cs = p.parse();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception");
		}
		return cs;
	}
}
