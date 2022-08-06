package ccjava.javaparser;

public class BindingKeyParser {
	public static String parse(String ecjBindingKey) {
		String key = ecjBindingKey;
		key = removeGenericsInformation(key);
		return key;
	}

	// Assumes key has balanced brackets.
	private static String removeGenericsInformation(String key) {
		StringBuilder parsedKey = new StringBuilder();
		
		// For unknown reasons, ECJ tends to replace $ with dot when there are parameter brackets preceding it.
		key = key.replace(">.", ">$");
		
		int brackets = 0;
		for (Character c : key.toCharArray()) {
			if (c == '<') {
				++brackets; 
			} else if (c == '>') {
				--brackets;
			} else if (brackets == 0) {
				parsedKey.append(c);
			}
		}
		
		return parsedKey.toString();
	}
}
