package ccjava.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class CSVExporter {
	private static final Path DEFINITIONS_FILENAME = Paths.get("defs.csv");
	private static final Path USES_FILENAME = Paths.get("uses.csv");
	private static final Path DIFFS_FILENAME = Paths.get("diffs.csv");
	private static final Path DIFF_RELATIONS_FILENAME = Paths.get("diffRelations.csv");
	private static final Path PARTITIONS_FILENAME = Paths.get("partitions.csv");
	private static final Path SUMMARY_FILENAME = Paths.get("summary.csv");
	
	private static class Summary {
		public int sourceFiles = 0;
		public int defs = 0;
		public int uses = 0;
		public int diffs = 0;
		public int totalPartitions = 0;
		public int trivialPartitions = 0;
		public int nonTrivialPartitions = 0;
	}
	
	private Changeset changeset;
	private ClusterChanges cc;
	
	public CSVExporter(Changeset changeset, ClusterChanges cc) {
		Validate.notNull(changeset);
		Validate.notNull(cc);
		
		this.changeset = changeset;
		this.cc = cc;
	}
	
	public void exportAsCSV(Path outputDir) throws IOException {
		Validate.notNull(outputDir);
		
		exportDefs(outputDir);
		exportUses(outputDir);
		exportDiffs(outputDir);
		exportDiffRelations(outputDir);
		exportPartitions(outputDir);
		exportSummary(outputDir);
	}

	private void exportDefs(Path outputDir) throws IOException {
		String header = "defId,sourceFile,characterSpanStart,characterSpanEnd,name,isTypeDef,isMethodDef,isInsideADiff";
		
		List<String> rows = new ArrayList<String>(); 
		for (Definition d : this.changeset.getDefinitions()) {
			rows.add(getCSVString(new Object[] {
					d.getId(),
					d.getSourceFile(),		
					d.getPosition().getFirstCharacterPosition(),
					d.getPosition().getLastCharacterPosition(),
					d.getName(),
					Boolean.valueOf(d.isTypeDefinition()).toString(),
					Boolean.valueOf(d.isMethodDefinition()).toString(),
					Boolean.valueOf(d.isInsideADiffRegion()).toString()
					}));
		}
		
		createCSVFile(outputDir.resolve(DEFINITIONS_FILENAME), header, rows);
	}
	
	private void exportUses(Path outputDir) throws IOException {
		String header = "useId,sourceFile,characterSpanStart,characterSpanEnd,name,associatedDefId,associatedDefSourceFile,associatedDefCharacterSpanStart,associatedDefCharacterSpanEnd,isInsideADiff";
		
		List<String> rows = new ArrayList<String>(); 
		for (Use u : this.changeset.getUses()) {
			rows.add(getCSVString(new Object[] {
					u.getId(),
					u.getSourceFile(),
					u.getPosition().getFirstCharacterPosition(),
					u.getPosition().getLastCharacterPosition(),
					u.getName(),
					u.getAssociatedDefinition().getId(),
					u.getAssociatedDefinition().getSourceFile(),
					u.getAssociatedDefinition().getPosition().getFirstCharacterPosition(),
					u.getAssociatedDefinition().getPosition().getLastCharacterPosition(),
					Boolean.valueOf(u.isInsideADiffRegion()).toString()
					}));
		}
		
		createCSVFile(outputDir.resolve(USES_FILENAME), header, rows);
	}
	
	private void exportDiffs(Path outputDir) throws IOException {
		String header = "diffId,sourceFile,lineSpanStart,lineSpanEnd,characterSpanStart,characterSpanEnd";
		
		List<String> rows = new ArrayList<String>(); 
		for (DiffRegion dr : this.changeset.getDiffRegions()) {
			rows.add(getCSVString(new Object[] {
					dr.getId(),
					dr.getSourceFile(),
					dr.getLineSpan().getFirstLineNumber(),
					dr.getLineSpan().getLastLineNumber(),
					dr.getCharacterSpan().getFirstCharacterPosition(),
					dr.getCharacterSpan().getLastCharacterPosition(),
					}));
		}
		
		createCSVFile(outputDir.resolve(DIFFS_FILENAME), header, rows);
	}
	
	private void exportDiffRelations(Path outputDir) throws IOException {
		String header = "relationId,relationType,diffId1,sourceFile1,lineSpanStart1,lineSpanEnd1,diffId2,sourceFile2,lineSpanStart2,lineSpanEnd2";
		
		List<String> rows = new ArrayList<String>(); 
		for (RelatedDiffPair rdp : this.cc.getRelatedDiffs()) {
			rows.add(getCSVString(new Object[] {
					rdp.getId(),
					rdp.getRelationType(),
					rdp.getFirstDiffRegion().getId(),
					rdp.getFirstDiffRegion().getSourceFile(),
					rdp.getFirstDiffRegion().getLineSpan().getFirstLineNumber(),
					rdp.getFirstDiffRegion().getLineSpan().getLastLineNumber(),
					rdp.getSecondDiffRegion().getId(),
					rdp.getSecondDiffRegion().getSourceFile(),
					rdp.getSecondDiffRegion().getLineSpan().getFirstLineNumber(),
					rdp.getSecondDiffRegion().getLineSpan().getLastLineNumber(),
					}));
		}
		
		createCSVFile(outputDir.resolve(DIFF_RELATIONS_FILENAME), header, rows);
	}
	
	private void exportPartitions(Path outputDir) throws IOException {
		String header = "partitionId,isTrivial,diffId,diffSourceFile,diffLineSpanStart,diffLineSpanEnd,diffCharacterSpanStart,diffCharacterSpanEnd,enclosingMethodDefId";
		
		List<String> rows = new ArrayList<String>(); 
		for (Partition p : this.cc.getPartitions()) {
			for (DiffRegion dr : p.getDiffRegions()) {
				rows.add(getCSVString(new Object[] {
						p.getId(),
						Boolean.valueOf(p.isTrivial()).toString(),
						dr.getId(),
						dr.getSourceFile(),
						dr.getLineSpan().getFirstLineNumber(),
						dr.getLineSpan().getLastLineNumber(),
						dr.getCharacterSpan().getFirstCharacterPosition(),
						dr.getCharacterSpan().getLastCharacterPosition(),
						dr.getEnclosingMethod() != null ? dr.getEnclosingMethod().getId() : "null",
						}));
			}
		}
		
		createCSVFile(outputDir.resolve(PARTITIONS_FILENAME), header, rows);
	}
	
	private void exportSummary(Path outputDir) throws IOException {
		String header = "sourceFiles,defs,uses,diffs,totalPartitions,nonTrivialPartitions,trivialPartitions";
		
		Summary s = getSummary();

		List<String> rows = new ArrayList<String>();
		rows.add(getCSVString(new Object[] {
				s.sourceFiles,
				s.defs,
				s.uses,
				s.diffs,
				s.totalPartitions,
				s.nonTrivialPartitions,
				s.trivialPartitions
				}));
		
		createCSVFile(outputDir.resolve(SUMMARY_FILENAME), header, rows);
	}
	
	private Summary getSummary() {
		Summary s = new Summary();
		
		s.sourceFiles = this.changeset.getSourceFiles().size();
		s.defs = this.changeset.getDefinitions().size();
		s.uses = this.changeset.getUses().size();
		s.diffs = this.changeset.getDiffRegions().size();
		
		for (Partition p : this.cc.getPartitions()) {
			++s.totalPartitions;
			
			if (p.isTrivial()) {
				++s.trivialPartitions;
			} else {
				++s.nonTrivialPartitions;
			}
		}
		
		return s;
	}
	
	private void createCSVFile(Path filePath, String header, List<String> rows) throws IOException {
		filePath.getParent().toFile().mkdirs();
		
		try (BufferedWriter fout = new BufferedWriter(new FileWriter(filePath.toFile()))) {
			fout.write(header);
			fout.newLine();
			for (String row : rows) {
				fout.write(row);
				fout.newLine();
			}
		} 
	}

	private String getCSVString(Object[] fields) {
		Validate.notNull(fields);
		
		String fmtStr = StringUtils.repeat("%s,", fields.length);
		fmtStr = fmtStr.substring(0, fmtStr.length() - 1); // removes extra comma at the end
		
		return String.format(fmtStr, fields);
	}
}
