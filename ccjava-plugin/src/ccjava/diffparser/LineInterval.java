package ccjava.diffparser;

import org.apache.commons.lang3.Validate;

public class LineInterval {
	private int firstLineNumber;
	private int lastLineNumber;
	
	public static LineInterval fromNumbers(int firstLineNumber, int lastLineNumber) {
		return new LineInterval(firstLineNumber, lastLineNumber);
	}
	
	public static LineInterval fromNumberAndLength(int firstLineNumber, int length) {
		return new LineInterval(firstLineNumber, firstLineNumber + length - 1);
	}
	
	private LineInterval(int firstLineNumber, int lastLineNumber) {
		Validate.isTrue(firstLineNumber >= 1);		
		Validate.isTrue(lastLineNumber >= 1);
		Validate.isTrue(firstLineNumber <= lastLineNumber);
		
		this.firstLineNumber = firstLineNumber;
		this.lastLineNumber = lastLineNumber;
	}
	
	public int getFirstLineNumber() {
		return firstLineNumber;
	}
	
	public int getLastLineNumber() {
		return lastLineNumber;
	}
	
	public int getLength() {
		return lastLineNumber - firstLineNumber + 1;
	}
	
	public boolean intersects(LineInterval other) {
		Validate.notNull(other);
		
		return !( (this.firstLineNumber > other.lastLineNumber) || (this.lastLineNumber < other.firstLineNumber) );
	}
	
	public boolean contains(LineInterval other) {
		Validate.notNull(other);
		
		return this.firstLineNumber <= other.firstLineNumber && other.lastLineNumber <= this.lastLineNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstLineNumber;
		result = prime * result + lastLineNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LineInterval))
			return false;
		LineInterval other = (LineInterval) obj;
		if (firstLineNumber != other.firstLineNumber)
			return false;
		if (lastLineNumber != other.lastLineNumber)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("(%d, %d)", firstLineNumber, lastLineNumber);
	}
}
