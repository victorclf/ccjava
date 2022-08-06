package ccjava;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import ccjava.diffparser.GitDiffParser;
import ccjava.diffparser.UnifiedDiffRegion;
import ccjava.javaparser.Parser;
import ccjava.model.CSVExporter;
import ccjava.model.Changeset;
import ccjava.model.ClusterChanges;

public class Application implements IApplication {
	public Object start(IApplicationContext context) throws Exception {
		String sourceDir = getSourceDir(context);
		Changeset changeset = new Parser(sourceDir).parse();
		
		if (!changeset.isEmpty()) {
			List<UnifiedDiffRegion> uniDiffRegions = new GitDiffParser(Paths.get(sourceDir)).parse();
			changeset.addDiffRegions(uniDiffRegions);
			ClusterChanges cc = new ClusterChanges(changeset);
			cc.run();
			CSVExporter csv = new CSVExporter(changeset, cc);
			csv.exportAsCSV(Paths.get(sourceDir, "ccjava-results"));
		}
		
		return IApplication.EXIT_OK;
	}

	public void stop() {
	}
	
	private String getSourceDir(IApplicationContext context) {
		Map args = context.getArguments();
		String[] appArgs = (String[]) args.get("application.args");
		if (appArgs.length != 1) {
			throw new RuntimeException("invalid number of arguments: " + Arrays.asList(appArgs));
		}
		return appArgs[0];
	}
}
