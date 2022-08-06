package ccjava;

public class DEFINES {
	public static final boolean FAIL_ON_ERROR = false;
	public static final boolean FAIL_WHEN_CANT_CREATE_MOCKS_FOR_ALL_MISSING_TYPES = FAIL_ON_ERROR & true;
	
	public static final boolean LOG_ENABLED = true;
	public static final boolean LOG_DEFINITION_VISITOR = LOG_ENABLED && true;
	public static final boolean LOG_USE_VISITOR = LOG_ENABLED && true;
	public static final boolean LOG_COMMENT_VISITOR = LOG_ENABLED && true;
	public static final boolean LOG_MISSING_TYPE_VISITOR = LOG_ENABLED && true;
	public static final boolean LOG_DIFFS = LOG_ENABLED && true;
	public static final boolean LOG_RELATED_DIFFS = LOG_ENABLED && true;
	public static final boolean LOG_PARTITIONS = LOG_ENABLED && true;
}
