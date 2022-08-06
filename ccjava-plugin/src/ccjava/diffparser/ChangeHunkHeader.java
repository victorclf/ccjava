package ccjava.diffparser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class ChangeHunkHeader {
	private LineInterval originalSpan;
	private LineInterval newSpan;
	private String header;
	
	public static ChangeHunkHeader parseChangeHunkHeader(String changeHunkHeader) {
		// @@ -l,s +l,s @@ optional section heading
		// {"@@", "-l,s", "+l,s", "@@", "optional section heading part 1", , "optional section heading part n"}
		String[] splitHeader = changeHunkHeader.trim().split(" ");
		
		LineInterval originalSpan = parseChangeHunkHeaderSpanField(splitHeader[1]);
		LineInterval newSpan = parseChangeHunkHeaderSpanField(splitHeader[2]);
		
		String header = "";
		if (splitHeader.length > 4) {
			header = StringUtils.join(splitHeader, " ", 4, splitHeader.length);
		}
		
		return new ChangeHunkHeader(originalSpan, newSpan, header);
	}

	public ChangeHunkHeader(LineInterval originalSpan, LineInterval newSpan) {
		this(originalSpan, newSpan, "");
	}
	
	public ChangeHunkHeader(LineInterval originalSpan, LineInterval newSpan, String header) {
		Validate.isTrue(originalSpan != null || newSpan != null);
		Validate.notNull(header);
		
		this.originalSpan = originalSpan;
		this.newSpan = newSpan;
		this.header = header;
	}

	/**
	 * Returns null if file was created (-0,0). 
	 */
	public LineInterval getOriginalSpan() {
		return this.originalSpan;
	}

	/**
	 * Returns null if file was deleted (+0,0). 
	 */
	public LineInterval getNewSpan() {
		return this.newSpan;
	}
	
	public boolean isCreatedFile() {
		return this.originalSpan == null;
	}
	
	public boolean isDeletedFile() {
		return this.newSpan == null;
	}
	
	public String getHeader() {
		return this.header;
	}
	
	private static LineInterval parseChangeHunkHeaderSpanField(String spanStr) {
		// -l,s or -l or +l,s or +l
		int firstLineNumber;
		int length;

		String s = spanStr.substring(1, spanStr.length());
		if (s.contains(",")) {
			String[] splitS = s.split(",");
			firstLineNumber = Integer.valueOf(splitS[0]);
			length = Integer.valueOf(splitS[1]);
			
		} else {
			firstLineNumber = Integer.valueOf(s);
			length = 1;
		}
		
		// 0 value represents a special case instead of a line, it's either file creation or deletion
		return firstLineNumber != 0 
				? LineInterval.fromNumberAndLength(firstLineNumber, length)
				: null;
	}
}
