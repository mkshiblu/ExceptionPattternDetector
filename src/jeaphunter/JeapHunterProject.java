package jeaphunter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Abstraction and wrapper around the JDT project
 */
public class JeapHunterProject {

	private IProject project;
	private CompilationUnit[] compilationUnits;

	public JeapHunterProject(IProject project) {
		this.project = project;
	}

	/**
	 * Returns all the compilation units of this project. The result it cached after
	 * the first time
	 */
	public CompilationUnit[] getAllCompilationUnits() throws JavaModelException {
		if (compilationUnits == null) {
			List<CompilationUnit> compilationUnitList = new ArrayList<>();
			for (IPackageFragment packageFragment : JavaCore.create(project).getPackageFragments()) {
				// Get all the source file / compilation unit of the package and add to the list
				Arrays.stream(packageFragment.getCompilationUnits())
						.forEach(icompUnit -> compilationUnitList.add(parse(icompUnit)));
			}
			compilationUnits = compilationUnitList.toArray(new CompilationUnit[compilationUnitList.size()]);
		}

		return compilationUnits;
	}

	/**
	 * Getter for the underlying project
	 */
	public IProject getProject() {
		return project;
	}

	public static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS13);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}