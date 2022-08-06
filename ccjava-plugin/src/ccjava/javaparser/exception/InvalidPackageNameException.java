package ccjava.javaparser.exception;

public class InvalidPackageNameException extends RuntimeException {
	private static final long serialVersionUID = 6283281582087806028L;
	
	public InvalidPackageNameException(String packageName) {
		super("Invalid package name: " + packageName);
	}
}
