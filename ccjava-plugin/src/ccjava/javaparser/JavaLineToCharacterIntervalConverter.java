package ccjava.javaparser;

import org.apache.commons.lang3.Validate;
import org.eclipse.jdt.core.dom.CompilationUnit;

import ccjava.diffparser.LineInterval;
import ccjava.model.CharacterInterval;
import ccjava.model.LineToCharacterIntervalConverter;

public class JavaLineToCharacterIntervalConverter implements LineToCharacterIntervalConverter {
	private CompilationUnit parsedCode;
	
	public JavaLineToCharacterIntervalConverter(CompilationUnit parsedCode) {
		Validate.notNull(parsedCode);
		
		this.parsedCode = parsedCode;
	}

	@Override
	public CharacterInterval getCharacterInterval(LineInterval lines) {
		int firstCharPos = this.parsedCode.getPosition(lines.getFirstLineNumber(), 0);
		int lastCharPos = this.parsedCode.getPosition(lines.getLastLineNumber() + 1, 0) - 1;
		if (lastCharPos < 0) { // happens if lastLineNumber() + 1 doesnt exist
			lastCharPos = this.parsedCode.getLength() - 1;
		}
		
		return CharacterInterval.fromPositions(firstCharPos, lastCharPos);
	}
}
