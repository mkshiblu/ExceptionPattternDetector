package jeaphunter;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;

import jeaphunter.plugin.PluginConsole;
import jeaphunter.visitors.TryStatementVisitor;

/**
 * Class contains various exception anti-pattern detection methods
 */
public class JeapHunter {

	public static PrintStream Console = System.out;
	
	private JeapHunterProject project;
	private HashSet<TryStatement> projectNestedTryStatements = new HashSet<TryStatement>();

	public JeapHunter(JeapHunterProject project) {
		this.project = project;
	}

	/**
	 * Detects all Exception anti-patterns in the project
	 */
	public void detectAllExceptionAntiPatterns() {
		CompilationUnit[] compilationUnits;
		try {
			compilationUnits = project.getAllCompilationUnits();
			for (CompilationUnit compilationUnit : compilationUnits) {
				// projectNestedTryStatements.addAll(detectNestedTry(compilationUnit));
				detectDestructiveWrapping(compilationUnit);
				detectOverCatch(compilationUnit);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		/// PluginConsole.writeLine("NESTED TRY RESULTS(" +
		/// projectNestedTryStatements.size() + " items):\n");

		for (TryStatement nestedTryStatement : projectNestedTryStatements) {
			CompilationUnit compilationUnit = (CompilationUnit) nestedTryStatement.getRoot();
			int lineNumber = compilationUnit.getLineNumber(nestedTryStatement.getStartPosition());
			// PluginConsole.writeLine(compilationUnit.getTypeRoot().getJavaProject().getProject().getName()+"
			// project: ");
			// PluginConsole.writeLine(compilationUnit.getTypeRoot().getElementName() + " at
			// Line:" + lineNumber + "\n");
			// PluginConsole.writeLine(nestedTryStatement.toString()+"\n");
		}
	}

	public HashSet<TryStatement> detectNestedTry(CompilationUnit compilationUnit) {
		TryStatementVisitor compilationUnitTryVisitor = new TryStatementVisitor();
		HashSet<TryStatement> compilationUnitNestedTryStatements = new HashSet<>();
		compilationUnit.accept(compilationUnitTryVisitor);
		for (TryStatement tryStatement : compilationUnitTryVisitor.getTryStatements()) {
			TryStatementVisitor tryStatementTryVisitor = new TryStatementVisitor();

			tryStatement.getBody().accept(tryStatementTryVisitor);

			if (tryStatementTryVisitor.getTryStatements().size() > 0) {
				compilationUnitNestedTryStatements.add(tryStatement);
			}
		}
		return compilationUnitNestedTryStatements;
	}

	public void detectDestructiveWrapping(CompilationUnit compilationUnit) {

	}

	/**
	 * Detect patterns when catch is given but there is now throw for that catch
	 * inside the try
	 * 
	 * @param compilationUnit
	 */
	public void detectOverCatch(CompilationUnit compilationUnit) {
		Console.println("Detecting Over Catch for ..." + compilationUnit);
	}
}
