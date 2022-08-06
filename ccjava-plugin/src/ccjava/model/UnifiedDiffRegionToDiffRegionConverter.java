package ccjava.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ccjava.diffparser.LineInterval;
import ccjava.diffparser.UnifiedDiffRegion;

public class UnifiedDiffRegionToDiffRegionConverter {
	private UnifiedDiffRegion uniDiffRegion;
	private LineToCharacterIntervalConverter lineToCharConverter;
	private Collection<Definition> sourceFileDefinitions;
	private Collection<Use> sourceFileUses;
	private SourceFile sourceFile;

	public UnifiedDiffRegionToDiffRegionConverter(UnifiedDiffRegion uniDiffRegion,
			LineToCharacterIntervalConverter lineToCharConverter,
			SourceFile sourceFile,
			Collection<Definition> sourceFileDefinitions,
			Collection<Use> sourceFileUses) {
		this.uniDiffRegion = uniDiffRegion;
		this.lineToCharConverter = lineToCharConverter;
		this.sourceFile = sourceFile;
		this.sourceFileDefinitions = sourceFileDefinitions;
		this.sourceFileUses = sourceFileUses;
	}
	
	public List<DiffRegion> convert() {
		LineInterval udrLineSpan = uniDiffRegion.getLines();
		CharacterInterval udrCharSpan = lineToCharConverter.getCharacterInterval(udrLineSpan);
		Set<Definition> definitionsInsideUDR = extractDefinitionsInside(sourceFileDefinitions, udrCharSpan);
		Set<Use> usesInsideUDR = extractUsesInside(sourceFileUses, udrCharSpan);
		
		List<LineInterval> splitSpan = splitDiffRegionSpanBasedOnOrganizationalUnits(udrLineSpan, lineToCharConverter, definitionsInsideUDR);
		
		List<DiffRegion> diffRegions = createDiffRegionForEachSpan(splitSpan, definitionsInsideUDR, usesInsideUDR);
		diffRegions = removedIrrelevantDiffRegions(diffRegions);
		return diffRegions;
	}

	private Set<Definition> extractDefinitionsInside(Collection<Definition> definitions, CharacterInterval span) {
		Set<Definition> definitionsInside = new LinkedHashSet<Definition>();
		for (Definition d : definitions) {
			if (d.getPosition().intersects(span)) {
				definitionsInside.add(d);
			}
		}
		return definitionsInside;
	}
	
	private Set<Use> extractUsesInside(Collection<Use> uses, CharacterInterval span) {
		Set<Use> usesInside = new LinkedHashSet<Use>();
		for (Use u : uses) {
			if (u.getPosition().intersects(span)) {
				usesInside.add(u);
			}
		}
		return usesInside;
	}

	/*
	 * We want to split diff regions if they contain more than one method or class.
	 * 
	 * If we simply check whether the current line contains more than one definition for a method/class, 
	 * we will end up with several one-line diffs, since every method is inside a class. Counting methods and classes
	 * separately isn't ideal either since Java supports inner classes.
	 * 
	 * Therefore, the algorithm here keeps adding one more line to a possible diff region and if this new line makes
	 * the number of organizational units in the region increase, then the diff region is split at that line.  
	 */
	private List<LineInterval> splitDiffRegionSpanBasedOnOrganizationalUnits(LineInterval originalSpan,
			LineToCharacterIntervalConverter lineToCharConverter,
			Set<Definition> definitionsInsideUDR) {
		List<LineInterval> splitSpans = new ArrayList<LineInterval>(originalSpan.getLength());
		
		LineInterval currentSpan = LineInterval.fromNumberAndLength(originalSpan.getFirstLineNumber(), 1);
		
		while (originalSpan.contains(currentSpan)) {
			int currentOrgUnitCount = countOrganizationalUnitsInside(currentSpan, lineToCharConverter, definitionsInsideUDR);
			LineInterval newSpan = LineInterval.fromNumberAndLength(currentSpan.getFirstLineNumber(), currentSpan.getLength() + 1);
			
			if (!originalSpan.contains(newSpan)
				|| countOrganizationalUnitsInside(newSpan, lineToCharConverter, definitionsInsideUDR) > currentOrgUnitCount) {
					splitSpans.add(currentSpan);
					currentSpan = LineInterval.fromNumberAndLength(currentSpan.getLastLineNumber() + 1, 1);
			} else {
				currentSpan = newSpan;
			}
		}
		
		return splitSpans;
	}
	
	private int countOrganizationalUnitsInside(LineInterval lineSpan, LineToCharacterIntervalConverter lineToCharConverter, Set<Definition> definitions) {
		CharacterInterval charSpan = lineToCharConverter.getCharacterInterval(lineSpan);
		int organizationalUnits = 0;
		
		for (Definition d : definitions) {
			if (d.isOrganizationalUnit() && d.getPosition().intersects(charSpan)) {
				++organizationalUnits;
			}
		}
		
		return organizationalUnits;
	}
	
	private boolean hasOrganizationalUnitsInside(DiffRegion dr) {
		for (Definition d : this.sourceFileDefinitions) {
			if (d.isOrganizationalUnit() && d.getPosition().intersects(dr.getCharacterSpan())) {
				return true;
			}
		}
		
		return false;
	}
	
	private List<DiffRegion> createDiffRegionForEachSpan(
			List<LineInterval> splitSpan, Set<Definition> definitionsInsideUDR, Set<Use> usesInsideUDR) {
		
		List<DiffRegion> diffRegions = new ArrayList<DiffRegion>(splitSpan.size());
		
		for (LineInterval unitSplitSpan : splitSpan) {
			CharacterInterval charUnitSplitSpan = this.lineToCharConverter.getCharacterInterval(unitSplitSpan);
			Set<Definition> unitDefinitionsInside = extractDefinitionsInside(definitionsInsideUDR, charUnitSplitSpan);
			Set<Use> unitUsesInside = extractUsesInside(usesInsideUDR, charUnitSplitSpan);
			diffRegions.add(new DiffRegion(this.sourceFile, charUnitSplitSpan, unitSplitSpan, unitDefinitionsInside, unitUsesInside));
		}
		
		return diffRegions;
	}

	private List<DiffRegion> removedIrrelevantDiffRegions(List<DiffRegion> diffRegions) {
		List<DiffRegion> relevantDiffRegions = new ArrayList<DiffRegion>(diffRegions.size());
		for (DiffRegion dr : diffRegions) {
			if (isDiffRegionBlank(dr)) {
				System.out.println("ignored blank diff region: " + dr);
				continue;
			}
			if (diffRegionContainsOnlyImportsAndPackageDeclaration(dr)) {
				System.out.println("ignored imports/package-decl diff region: " + dr);
				continue;
			}
			if (diffRegionContainsOnlyComments(dr)) {
				System.out.println("ignored comments diff region: " + dr);
				continue;
			}
			
			relevantDiffRegions.add(dr);
		}
		
		return relevantDiffRegions;				
	}
	
	private boolean isDiffRegionBlank(DiffRegion dr) {
		List<String> contents = dr.getContents();
		for (String s : contents) {
			if (!StringUtils.isBlank(s)) {
				return false;
			}
		}
		return true;
	}

	private boolean diffRegionContainsOnlyImportsAndPackageDeclaration(DiffRegion dr) {
		return !hasOrganizationalUnitsInside(dr);
	}
	
	private boolean diffRegionContainsOnlyComments(DiffRegion dr) {
		int currentLineNumber = dr.getLineSpan().getFirstLineNumber();
		for (String line : dr.getContents()) {
			if (StringUtils.isBlank(line)) {
				continue;
			}
			if (!diffLineContainsOnlyComments(this.sourceFile.getComments(), line, currentLineNumber)) {
				return false;
			}
			
			++currentLineNumber;
		}
		return true;
	}

	private boolean diffLineContainsOnlyComments(Collection<Comment> comments, String line, int lineNumber) {
		CharacterInterval diffLineCharSpan = this.lineToCharConverter.getCharacterInterval(LineInterval.fromNumberAndLength(lineNumber, 1));
		String trimmedLine = line.trim();
		int trimmedLineFirstCharPos = diffLineCharSpan.getFirstCharacterPosition() + line.indexOf(trimmedLine);
		int trimmedLineLastCharPos = trimmedLineFirstCharPos + trimmedLine.length() - 1;
		CharacterInterval trimmedLineCharSpan = CharacterInterval.fromPositions(trimmedLineFirstCharPos, trimmedLineLastCharPos);
		for (Comment c : comments) {
			if (c.getPosition().contains(trimmedLineCharSpan)) {
				return true;
			}
		}
		return false;
	}

}
