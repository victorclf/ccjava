package ccjava.parser;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import ccjava.javaparser.Parser;
import ccjava.model.Changeset;
import ccjava.model.Definition;
import ccjava.model.Use;

public class UseVisitorTest {
	@Test
	public void testAnnotationUse() {
		Changeset cs = parse("test/examples/annotation");
		Assert.assertTrue(containsUse(cs, "Change.java", "Testable", 71, 78, true));
	}

	@Test
	public void testAnnotationMemberUse() {
		Changeset cs = parse("test/examples/annotation");
		Assert.assertTrue(containsUse(cs, "Change.java", "id", 80, 81, true));
	}
	
	@Test
	public void testClazzUse() {
		Changeset cs = parse("test/examples/toy_example_multi_file");
		Assert.assertFalse(containsUse(cs, "Person.java", "Entity", 152, 157));
	}
	
	@Test
	public void testClazzUseMockDefinitionMustNotHaveAssociatedDefinition() {
		Changeset cs = parse("test/examples/missingtypes");
		Assert.assertTrue(containsUse(cs, "Player.java", "Actor", 118, 122, false));
		Assert.assertTrue(containsUse(cs, "Player.java", "Entity", 135, 140, false));
		Assert.assertTrue(containsUse(cs, "Player.java", "Sprite", 153, 158, false));
		Assert.assertTrue(containsUse(cs, "Player.java", "Position", 177, 184, false));
		Assert.assertTrue(containsUse(cs, "Player.java", "Surface", 418, 424, false));
	}
	
	@Test
	public void testEnumUse() {
		Changeset cs = parse("test/examples/enum");
		Assert.assertFalse(containsUse(cs, "Rect.java", "Color", 98, 102));
		Assert.assertFalse(containsUse(cs, "Rect.java", "Color", 200, 204));
		Assert.assertFalse(containsUse(cs, "Rect.java", "Color", 261, 265));
	}
	
	@Test
	public void testEnumConstantUse() {
		Changeset cs = parse("test/examples/enum");
		Assert.assertTrue(containsUse(cs, "Rect.java", "BLUE", 206, 209, true));
	}
	
	@Test
	public void testFieldUse() {
		Changeset cs = parse("test/examples/toy_example");
		Assert.assertTrue(containsUse(cs, "Person.java", "firstName", 187, 195, true));
		Assert.assertTrue(containsUse(cs, "Person.java", "firstName", 267, 275, true));
		
		Assert.assertTrue(containsUse(cs, "Person.java", "lastName", 346, 353, true));
		Assert.assertTrue(containsUse(cs, "Person.java", "lastName", 423, 430, true));
		
		Assert.assertTrue(containsUse(cs, "Person.java", "age", 492, 494, true));
		Assert.assertTrue(containsUse(cs, "Person.java", "age", 551, 553, true));
	}
	
	@Test
	public void testFieldUseGenericType() {
		Changeset cs = parse("test/examples/generics2");
		
		Assert.assertTrue(containsUse(cs, "GenTest.java", "bag", 118, 120, true));
		Assert.assertTrue(containsUse(cs, "GenTest.java", "id", 172, 173, true));
	}
	
	@Test
	public void testFieldUseGenericTypeHandleDollarSignsAndDotsBindingKey() {
		Changeset cs = parse("test/examples/generics-dropwizard-metrics-779");
		
		Assert.assertTrue(containsUse(cs, "InstrumentedCloseableHttpAsyncClient.java", "consumer", 5984, 5991, true));
		Assert.assertTrue(containsUse(cs, "InstrumentedCloseableHttpAsyncClient.java", "consumer", 6366, 6373, true));
		Assert.assertTrue(containsUse(cs, "InstrumentedCloseableHttpAsyncClient.java", "consumer", 7206, 7213, true));
	}

	@Test
	public void testFieldUseInConstructorCall() {
		Changeset cs = parse("test/examples/constructorcall");
		
		Assert.assertTrue(containsUse(cs, "Universe.java", "defaultColor", 300, 311, 97, 124));
	}
	
	@Test
	public void testIgnoreUsesWithoutAssociatedDefinitions() {
		Changeset cs = parse("test/examples/missingtypes");
		Assert.assertTrue(containsUse(cs, "Player.java", "String", 344, 349));
		
		cs = parse("test/examples/missingtypes", true);
		Assert.assertFalse(containsUse(cs, "Player.java", "String", 344, 349));
	}
	
	@Test
	public void testLocalVariableUse() {
		Changeset cs = parse("test/examples/localvars");
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "sBuilder", 310, 317, true));
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "sum", 356, 358, true));
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "doubleSum", 369, 377, true));
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "sBuilder", 391, 398, true));
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "sum", 419, 421, true));
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "doubleSum", 444, 452, true));
		Assert.assertTrue(containsUse(cs, "PrettyPrinter.java", "sBuilder", 465, 472, true));
	}
	
	@Test
	public void testMethodUse() {
		Changeset cs = parse("test/examples/methodcall");
		Assert.assertTrue(containsUse(cs, "Main.java", "clear", 239, 243, true));
		Assert.assertTrue(containsUse(cs, "Main.java", "draw", 270, 273, true));
		Assert.assertTrue(containsUse(cs, "Main.java", "update", 284, 289, true));
		
		Assert.assertTrue(containsUse(cs, "Sprite.java", "loadImageFromDisk", 179, 195, true));
		Assert.assertTrue(containsUse(cs, "Sprite.java", "drawRect", 420, 427, true));
	}
	
	@Test
	public void testMethodUseConstructor() {
		Changeset cs = parse("test/examples/methodcall");
		Assert.assertTrue(containsUse(cs, "Main.java", "Window", 139, 158, 101, 268));
		Assert.assertTrue(containsUse(cs, "Main.java", "Sprite", 174, 214, true));
	}
	
	@Test
	public void testMethodUseOfMockClassesMustNotHaveAssociatedDef() {
		Changeset cs = parse("test/examples/methodcall");
		// Uses won't be created because there won't be binding keys since the mock classes wont contain these methods
		Assert.assertFalse(containsUse(cs, "Window.java", "drawRect", 377, 384, false));
		Assert.assertFalse(containsUse(cs, "Window.java", "update", 531, 536, false));
	}
	

	@Test
	public void testMethodUseInConstructorCall() {
		Changeset cs = parse("test/examples/constructorcall");
		
		Assert.assertTrue(containsUse(cs, "Universe.java", "getDefaultColor", 342, 356, 368, 434));
	}
	
	@Test
	public void testParameterUse() {
		Changeset cs = parse("test/examples/toy_example");
		Assert.assertTrue(containsUse(cs, "Person.java", "firstName", 279, 287, true));
		Assert.assertTrue(containsUse(cs, "Person.java", "lastName", 434, 441, true));
		Assert.assertTrue(containsUse(cs, "Person.java", "age", 557, 559, true));
	}
	
	@Test 
	public void testParameterUseExceptionCatchClause() {
		Changeset cs = parse("test/examples/exception");
		Assert.assertTrue(containsUse(cs, "Danger.java", "e", 298, 298, true));
	}
	
	@Test
	public void testPaths() {
		Changeset cs = parse("test/examples/toy_example");
		for (Use u : cs.getUses()) {
			Assert.assertTrue(u.getSourceFile().getPath().startsWith("somecompany/"));
		}
	}
	
//	@Test 
//	public void testTypeParameterDefinition() {
//		Changeset cs = parse("test/examples/generics");
//		Assert.assertTrue(containsUse(cs, "MyList.java", "T", 250, 250));
//		Assert.assertTrue(containsUse(cs, "MyList.java", "T", 656, 656));
//		Assert.assertTrue(containsUse(cs, "MyList.java", "T", 771, 771));
//	}

	private boolean containsUse(Changeset cs, String partialFilePath, String name, int startPos, int endPos) {
		for (Use u : cs.getUses()) {
			if (u.getSourceFile().getPath().contains(partialFilePath)
				&& name.equals(u.getName())
				&& startPos == u.getPosition().getFirstCharacterPosition() 
				&& endPos == u.getPosition().getLastCharacterPosition()) {
					return true;
			}
		}
		return false;
	}
	
	private boolean containsUse(Changeset cs, String partialFilePath, String name, int startPos, int endPos, boolean hasAssociatedDefinition) {
		for (Use u : cs.getUses()) {
			if (u.getSourceFile().getPath().contains(partialFilePath)
				&& name.equals(u.getName())
				&& startPos == u.getPosition().getFirstCharacterPosition() 
				&& endPos == u.getPosition().getLastCharacterPosition()) {
					if (hasAssociatedDefinition) {
						return hasCorrectAssociatedDefinition(u);
					} else {
						return u.getAssociatedDefinition() == null;
					}
			}
		}
		return false;
	}
	
	private boolean containsUse(Changeset cs, String partialFilePath, String name, int startPos, int endPos, int defStartPos, int defEndPos) {
		for (Use u : cs.getUses()) {
			if (u.getSourceFile().getPath().contains(partialFilePath)
				&& name.equals(u.getName())
				&& startPos == u.getPosition().getFirstCharacterPosition() 
				&& endPos == u.getPosition().getLastCharacterPosition()
				&& hasCorrectAssociatedDefinition(u, defStartPos, defEndPos)) {
					return true;
			}
		}
		return false;
	}

	private boolean hasCorrectAssociatedDefinition(Use u) {
		Definition d = u.getAssociatedDefinition();
		return d != null 
			   && u.getName().equals(d.getName());
	}
	
	private boolean hasCorrectAssociatedDefinition(Use u, int defStartPos, int defEndPos) {
		Definition d = u.getAssociatedDefinition();
		return d != null 
			   && u.getName().equals(d.getName())
			   && d.getPosition().getFirstCharacterPosition() == defStartPos
			   && d.getPosition().getLastCharacterPosition() == defEndPos;
	}
	
	private Changeset parse(String directory) {
		return parse(directory, false);
	}
	
	private Changeset parse(String directory, boolean ignoreUsesWithoutAssociatedDefinitions) {
		Changeset cs = null;
		try {
			Parser p = new Parser(directory);
			p.setIgnoreUsesWithoutAssociatedDefinitions(ignoreUsesWithoutAssociatedDefinitions);
			cs = p.parse();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception");
		}
		return cs;
	}
}
