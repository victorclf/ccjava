package ccjava.javaparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.launching.JavaRuntime;

import ccjava.DEFINES;
import ccjava.javaparser.exception.InvalidPackageNameException;
import ccjava.model.Changeset;
import ccjava.model.CharacterInterval;
import ccjava.model.Comment;
import ccjava.model.Definition;
import ccjava.model.SourceFile;
import ccjava.model.Use;

public class Parser {
	private static final String JAVA_PROJECT_VERSION = JavaCore.VERSION_1_8;
	private static final String DEFAULT_SOURCE_FOLDER_NAME = "src";
	private static final String DEFAULT_PACKAGELESS_SOURCE_FOLDER_NAME = "srcmisc";
	 // Roughly the maximum class hierarchy level we are willing to parse. @see createMocksForMissingTypes()
	private static final int MAXIMUM_CREATE_MISSING_MOCKS_ITERATIONS = 8;
	private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("package\\s+([a-zA_Z_][\\.\\w]*);");
	private boolean ignoreUsesWithoutAssociatedDefinitions = true;
	
	private String sourceDir;
	private IProject project;
	private IJavaProject javaProject;
	private IPackageFragmentRoot lastSrcFolderPackageFragmentRoot;
	
	private Map<String, SourceFile> sourceFiles;
	private Set<Definition> definitions;
	private Set<Use> uses;
	
	private Set<String> mockTypesPaths;


	public Parser(String sourceDir) {
		this.sourceDir = sourceDir;
		this.mockTypesPaths = new HashSet<String>();
	}

	public Changeset parse() throws CoreException, IOException {
		createProject();
		
		createMocksForMissingTypes();
		
		List<CompilationUnit> parsedCompilationUnits = parsePackageFragments();
		this.sourceFiles = createSourceFiles(parsedCompilationUnits);
		this.definitions = extractDefinitions(parsedCompilationUnits);
		this.uses = extractUses(parsedCompilationUnits);
		extractComments(parsedCompilationUnits);
		
		return new Changeset(this.sourceFiles);
	}

	public void setIgnoreUsesWithoutAssociatedDefinitions(
			boolean ignoreUsesWithoutAssociatedDefinitions) {
		this.ignoreUsesWithoutAssociatedDefinitions = ignoreUsesWithoutAssociatedDefinitions;
	}

	private Map<String, SourceFile> createSourceFiles(
			Collection<CompilationUnit> parsedCompilationUnits) {
		Map<String, SourceFile> sourceFilesMap = new HashMap<String, SourceFile>();
		
		for (CompilationUnit cu : parsedCompilationUnits) {
			if (!isCompilationUnitMockClass(cu)){
				String cuPath = getCompilationUnitPath(cu);
				String filePath = new Path(sourceDir).append(cuPath).toOSString();
				SourceFile sf = new SourceFile(cuPath, filePath, new JavaLineToCharacterIntervalConverter(cu));
				sourceFilesMap.put(cuPath, sf);
			}
		}
		
		return sourceFilesMap;
	}

	// TODO break this method into smaller ones
	private IProject createProject() throws CoreException, IOException {
		// Create project
		String projectName = getUniqueProjectName();
		this.project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		project.create(null);
		project.open(null);
		
		// Create source folders
//		IFolder rootSrcFolder = createSourceFolder(DEFAULT_SOURCE_FOLDER_NAME);

		this.javaProject = JavaCore.create(project);
		addNatureToProject(project, JavaCore.NATURE_ID);

		// TODO: Is this really needed?
		// Create default output folder
		// PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_NAME);

		// Refresh project
		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		// Collect class paths
		List<IClasspathEntry> classPaths = new ArrayList<IClasspathEntry>();
		IClasspathEntry JREClassPath = JavaRuntime.getDefaultJREContainerEntry();
		classPaths.add(JREClassPath);
//		IClasspathEntry rootSrcClassPath = JavaCore.newSourceEntry(project.getFullPath().append(rootSrcFolder.getName()));
//		classPaths.add(rootSrcClassPath);

		// Set compliance settings
		this.javaProject.setOption(JavaCore.COMPILER_COMPLIANCE, JAVA_PROJECT_VERSION);
		this.javaProject.setOption(JavaCore.COMPILER_SOURCE, JAVA_PROJECT_VERSION);
		this.javaProject.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JAVA_PROJECT_VERSION);
		this.javaProject.setOption(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
		this.javaProject.setOption(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);

//		this.rootSrcFolderPackageFragmentRoot = this.javaProject.getPackageFragmentRoot(rootSrcFolder);

		// Add the changeset after-files to the project
		File root = new File(sourceDir);
		for (File f : FileUtils.listFiles(root, new SuffixFileFilter(".java"), TrueFileFilter.INSTANCE)) {
			String pkgName = "";
			try {
				pkgName = getPackageNameFromJavaCodeFile(f);
			} catch (InvalidPackageNameException e) {
				if (DEFINES.FAIL_ON_ERROR) {
					throw e;
				}
				continue;
			}
			
			String srcFolderName = getSourceFolderName(f.getAbsolutePath(), pkgName);
			
			if (!sourceFolderExists(srcFolderName)) {
				IClasspathEntry srcClassPath = JavaCore.newSourceEntry(project.getFullPath().append(srcFolderName));
				classPaths.add(srcClassPath);
			}
			
			IFolder folder = createSourceFolder(srcFolderName);
			IPackageFragmentRoot pkgFragRoot = this.javaProject.getPackageFragmentRoot(folder);
			IPackageFragment pkgFrag = pkgFragRoot.createPackageFragment(pkgName, true, null);
			
			this.lastSrcFolderPackageFragmentRoot = pkgFragRoot; // mocks will be added to the last src folder

			String cUnitName = f.getName();
			
			@SuppressWarnings("unused")
			ICompilationUnit cUnit = pkgFrag.createCompilationUnit(cUnitName, readFileToString(f), false, null);
		}
		
		// Set classpath
		this.javaProject.setRawClasspath(classPaths.toArray(new IClasspathEntry[] {}), null);

		return project;
	}
	
	private String getRelativePath(String path1, String path2) throws IOException {
		java.nio.file.Path nioPath1 = java.nio.file.Paths.get(path1).normalize().toRealPath();
		java.nio.file.Path nioPath2 = java.nio.file.Paths.get(path2).normalize().toRealPath();
		return nioPath1.relativize(nioPath2).toString();
	}
	
	private String getSourceFolderName(String absolutePath, String pkgName) throws IOException {
		if (pkgName.trim().isEmpty()) {
			// Avoids source folder nesting conflicts.
			return DEFAULT_PACKAGELESS_SOURCE_FOLDER_NAME;
		}
		
		String pkgPath = pkgName.replace(".", "/");
		String fullSrcFolderPath = absolutePath.split(pkgPath)[0];
		String relativeSrcFolderPath = getRelativePath(this.sourceDir, fullSrcFolderPath);
		return DEFAULT_SOURCE_FOLDER_NAME + "/" + relativeSrcFolderPath;
	}
	
	private boolean sourceFolderExists(String folderPathStr) {
		return this.project.getFolder(new Path(folderPathStr)).exists();
	}

	private IFolder createSourceFolder(String folderPathStr) throws CoreException {
		Path path = new Path(folderPathStr);
		String currentPathStr = "";
		IFolder f = null;
		for (String seg : path.segments()) {
			if (currentPathStr.isEmpty()) {
				currentPathStr = seg;
			} else {
				currentPathStr += "/" + seg;
			}
			
			f = this.project.getFolder(new Path(currentPathStr));
			if (!f.exists()) {
				f.create(true, true, null);
			}
		}
		return f;
	}

	private String getPackageNameFromJavaCodeFile(File f) {
		String sourceCode = readFileToString(f);
		Matcher m = PACKAGE_NAME_PATTERN.matcher(sourceCode);
		boolean found = m.find();
		// "Capturing groups are indexed from left to right, starting at one"
		// Thus, group 1 is the package name.
		String pkgName = found ? m.group(1) : "";
				
		if (!validPackageName(pkgName)) {
			throw new InvalidPackageNameException(pkgName);
		}
		
		return pkgName;
	}
	
	private boolean validPackageName(String packageName) {
		return packageName.matches("[0-9A-Za-z\\.]*");
	}

	private String readFileToString(File f) {
		String fileData = null;
		try {
			fileData = FileUtils.readFileToString(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return fileData;
	}

	private void addNatureToProject(IProject project, String natureId) throws CoreException {
		IProjectDescription projectDescription = project.getDescription();
		String[] natureIds = projectDescription.getNatureIds();
		String[] newNatureIds = new String[natureIds.length + 1];
		System.arraycopy(natureIds, 0, newNatureIds, 0, natureIds.length);
		newNatureIds[natureIds.length] = natureId;
		projectDescription.setNatureIds(newNatureIds);
		project.setDescription(projectDescription, null);
	}

	private String getUniqueProjectName() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private Set<Definition> extractDefinitions(List<CompilationUnit> parsedCompilationUnits) throws JavaModelException {
		Set<Definition> defs = new HashSet<Definition>();
		
		for (CompilationUnit cu : parsedCompilationUnits) {
			if (!isCompilationUnitMockClass(cu)) {
				SourceFile sf = this.sourceFiles.get(getCompilationUnitPath(cu));
				
				if (DEFINES.LOG_DEFINITION_VISITOR) {
					System.out.println("\n***Extracting definitions from " + sf.getPath());
				}
				
				DefinitionVisitor defVisitor = new DefinitionVisitor(sf);
				cu.accept(defVisitor);
				defs.addAll(defVisitor.getDefinitions());
			}
		}
		
		return defs;
	}
	
	private Set<Use> extractUses(List<CompilationUnit> parsedCompilationUnits) throws JavaModelException {
		Set<Use> uses = new HashSet<Use>();
		
		for (CompilationUnit cu : parsedCompilationUnits) {
			if (!isCompilationUnitMockClass(cu)) {
				SourceFile sf = this.sourceFiles.get(getCompilationUnitPath(cu));
			
				if (DEFINES.LOG_USE_VISITOR) {
					System.out.println("\n***Extracting uses from " + sf.getPath());
				}
				
				UseVisitor useVisitor = new UseVisitor(sf, this.definitions, ignoreUsesWithoutAssociatedDefinitions);
				cu.accept(useVisitor);
				uses.addAll(useVisitor.getUses());
			}
		}
		
		return uses;
	}
	
	private Set<Comment> extractComments(List<CompilationUnit> parsedCompilationUnits) throws JavaModelException {
		Set<Comment> comments = new HashSet<Comment>();
		
		for (CompilationUnit cu : parsedCompilationUnits) {
			if (!isCompilationUnitMockClass(cu)) {
				SourceFile sf = this.sourceFiles.get(getCompilationUnitPath(cu));
				
				if (DEFINES.LOG_COMMENT_VISITOR) {
					System.out.println("\n***Extracting comments from " + sf.getPath());
				}
				
				for (Object commentObj : cu.getCommentList()) {
					org.eclipse.jdt.core.dom.Comment commentNode = (org.eclipse.jdt.core.dom.Comment) commentObj;
					CharacterInterval position = CharacterInterval.fromPositionAndLength(
							commentNode.getStartPosition(), commentNode.getLength());
					Comment c = new Comment(sf, position);
					comments.add(c);
					
					if (DEFINES.LOG_COMMENT_VISITOR) {
						System.out.println("comment " + c);
					}
				}
			}
		}
		
		return comments;
	}
	
	private boolean isCompilationUnitMockClass(ICompilationUnit cu) {
		return this.mockTypesPaths.contains(getCompilationUnitPath(cu));
	}

	private boolean isCompilationUnitMockClass(CompilationUnit cu) {
		return this.mockTypesPaths.contains(getCompilationUnitPath(cu));
	}
	
	private String getCompilationUnitPath(CompilationUnit cu) {
		return getCompilationUnitPath(cu.getJavaElement());
	}

	private String getCompilationUnitPath(IJavaElement cu) {
		String absolutePath = cu.getPath().makeAbsolute().toString();
		java.nio.file.Path nioCUnitPath = java.nio.file.Paths.get(absolutePath);
		return nioCUnitPath.subpath(2, nioCUnitPath.getNameCount()).toString(); // remove project and src folder names
	}
	
	private Collection<MissingType> extractMissingTypes() throws JavaModelException {
		if (DEFINES.LOG_MISSING_TYPE_VISITOR) {
			System.out.println("\n***Extracting missing types");
		}
		
		List<CompilationUnit> parsedCompilationUnits = parsePackageFragments();
		
		Map<String, MissingType> missingTypes = new HashMap<String, MissingType>();
		for (CompilationUnit cu : parsedCompilationUnits) {
			if (!isCompilationUnitMockClass(cu)) {
				if (DEFINES.LOG_MISSING_TYPE_VISITOR) {
					System.out.println("\n***Extracting missing types from " + getCompilationUnitPath(cu));
				}
				MissingTypeVisitor mtVisitor = new MissingTypeVisitor(missingTypes);
				cu.accept(mtVisitor);
			}
		}
		
		return missingTypes.values();
	}
	
	/*
	 * If there is a multi-level hierarchy, this method needs to be run multiple times to create all the necessary 
	 * mocks and ensure all bindings are correctly detected. The return value indicates whether mocks for all missing
	 * types were succesfully created. 
	 * 
	 * For example, suppose that A <- B <- C denotes an inheritance tree. B and C are in the changeset, but not A.
	 * Before the bindings between B and another entity are detected, the mock of A must be created.
	 */
	private boolean createMocksForMissingTypesSinglePass() throws JavaModelException {
		Collection<MissingType> missingTypes = extractMissingTypes();
		
		for (MissingType mt : missingTypes) {
			if (mt.isNested()) {
				continue; // nested (inner) types dont have their own compilation units
			}
			
			IPackageFragment pkgFrag = this.lastSrcFolderPackageFragmentRoot.createPackageFragment(mt.getPackagePath(), true, null);
			String cUnitName = mt.getName() + ".java";
			
			try {
				ICompilationUnit cUnit = pkgFrag.getCompilationUnit(cUnitName);
				if (isCompilationUnitMockClass(cUnit) && cUnit.exists()) {
					cUnit.delete(true, null);
					assert(!cUnit.exists());
				}
				cUnit = pkgFrag.createCompilationUnit(cUnitName, mt.generateCode(), false, null);
				assert(cUnit.exists());
				this.mockTypesPaths.add(getCompilationUnitPath(cUnit));
			} catch(JavaModelException e) {
				System.err.println("Failed to create mock: " + cUnitName);
				e.printStackTrace();
			}
		}
		
		return missingTypes.isEmpty();
	}
	
	private void createMocksForMissingTypes() throws JavaModelException {
		for (int i = 0; i < MAXIMUM_CREATE_MISSING_MOCKS_ITERATIONS; ++i) {
			if (createMocksForMissingTypesSinglePass()) {
				return;
			}
		}
		
		if (DEFINES.FAIL_WHEN_CANT_CREATE_MOCKS_FOR_ALL_MISSING_TYPES) {
			throw new JavaModelException(new Exception("Couldn't create mocks for all missing types!"), IJavaModelStatus.ERROR);
		}
	}

	private List<CompilationUnit> parsePackageFragments() throws JavaModelException {
		List<CompilationUnit> parsedCUnits = new ArrayList<CompilationUnit>();
		IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();

		for (IPackageFragment pkgFrag : packages) {
//			System.err.println("pkgFragName: " + pkgFrag.getElementName());
			if (pkgFrag.getKind() == IPackageFragmentRoot.K_SOURCE) {
				parsedCUnits.addAll(parsePackageFragment(pkgFrag));
			}
		}
		
		return parsedCUnits;
	}

	private List<CompilationUnit> parsePackageFragment(IPackageFragment packageFragment) throws JavaModelException {
		List<CompilationUnit> parsedCUnits = new ArrayList<CompilationUnit>();
		
		for (ICompilationUnit cUnit : packageFragment.getCompilationUnits()) {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(cUnit);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parsedCUnits.add((CompilationUnit) parser.createAST(null));
		}
		
		return parsedCUnits;
	}
}
