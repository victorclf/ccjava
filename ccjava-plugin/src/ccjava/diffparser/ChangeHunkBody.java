package ccjava.diffparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;

public class ChangeHunkBody {
	private List<UnifiedDiffRegion> diffs;
	
	public static ChangeHunkBody parseChangeHunkBody(ChangeHunkHeader header, String filePath, List<String> bodyStrList) {
		List<UnifiedDiffRegion> diffs = new ArrayList<UnifiedDiffRegion>();
		
		int currentLineNum = header.getNewSpan().getFirstLineNumber(); 
		boolean parsingDiffRegion = false;
		int diffRegionStartLineNum = -1;
		int diffRegionLength = 0;
		for (String line : bodyStrList) {
			if (line.startsWith("-") // we don't care about deletions 
				|| line.startsWith("\\ No newline at end of file") //special case of git diff
				) { 
				continue;
			}
			
			if (line.startsWith("+")) {
				if (!parsingDiffRegion) {
					parsingDiffRegion = true;
					diffRegionStartLineNum = currentLineNum;
					diffRegionLength = 0;
				}
				
				++diffRegionLength;
			} else {
				// Line was not modified. If parsing a diff region, finish it now.
				if (parsingDiffRegion) {
					diffs.add(new UnifiedDiffRegion(filePath, LineInterval.fromNumberAndLength(diffRegionStartLineNum, diffRegionLength)));
					parsingDiffRegion = false;
				}
			}
			
			++currentLineNum;
		}
		
		// Parse last diff region if applicable
		if (parsingDiffRegion) {
			diffs.add(new UnifiedDiffRegion(filePath, LineInterval.fromNumberAndLength(diffRegionStartLineNum, diffRegionLength)));
			parsingDiffRegion = false;
		}
		
		return new ChangeHunkBody(diffs);
	}
	
	public ChangeHunkBody(Collection<UnifiedDiffRegion> diffs) {
		Validate.notNull(diffs);
		
		this.diffs = new ArrayList<UnifiedDiffRegion>(diffs);
	}
	
	public List<UnifiedDiffRegion> getDiffs() {
		return this.diffs;
	}
}
