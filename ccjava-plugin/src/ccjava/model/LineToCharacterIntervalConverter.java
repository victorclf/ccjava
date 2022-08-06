package ccjava.model;

import ccjava.diffparser.LineInterval;

public interface LineToCharacterIntervalConverter {
	CharacterInterval getCharacterInterval(LineInterval lines);
}
