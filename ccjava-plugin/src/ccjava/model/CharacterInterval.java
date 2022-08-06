package ccjava.model;

import org.apache.commons.lang3.Validate;

public class CharacterInterval {
	private int firstCharPos;
	private int lastCharPos;
	
	public static CharacterInterval fromPositions(int firstCharacterPosition, int lastCharacterPosition) {
		return new CharacterInterval(firstCharacterPosition, lastCharacterPosition);
	}
	
	public static CharacterInterval fromPositionAndLength(int firstCharacterPosition, int length) {
		return new CharacterInterval(firstCharacterPosition, firstCharacterPosition + length - 1);
	}
	
	private CharacterInterval(int firstCharPos, int lastCharPos) {
		Validate.isTrue(firstCharPos >= 0);		
		Validate.isTrue(lastCharPos >= 0);
		Validate.isTrue(firstCharPos <= lastCharPos);
		
		this.firstCharPos = firstCharPos;
		this.lastCharPos = lastCharPos;
	}
	
	public int getFirstCharacterPosition() {
		return firstCharPos;
	}
	
	public int getLastCharacterPosition() {
		return lastCharPos;
	}
	
	public boolean intersects(CharacterInterval other) {
		Validate.notNull(other);
		
		return !( (this.firstCharPos > other.lastCharPos) || (this.lastCharPos < other.firstCharPos) );
	}
	
	public boolean contains(CharacterInterval other) {
		Validate.notNull(other);
		
		return this.firstCharPos <= other.firstCharPos && other.lastCharPos <= this.lastCharPos;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstCharPos;
		result = prime * result + lastCharPos;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CharacterInterval))
			return false;
		CharacterInterval other = (CharacterInterval) obj;
		if (firstCharPos != other.firstCharPos)
			return false;
		if (lastCharPos != other.lastCharPos)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("(%d, %d)", firstCharPos, lastCharPos);
	}
}
