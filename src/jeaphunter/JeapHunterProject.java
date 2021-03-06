package jeaphunter;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstraction and wrapper around the JDT project
 */
public class JeapHunterProject {

    private IProject project;

    public JeapHunterProject(IProject project) {
        this.project = project;
    }

    /**
     * Returns all the java compilation units of this project
     */
    public ICompilationUnit[] getSourceFiles() throws JavaModelException {
        List<ICompilationUnit> files = new ArrayList<>();
        for (IPackageFragment packageFragment : JavaCore.create(project).getPackageFragments()) {
            if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
                // Get all the source file / compilation unit of the package and add to the list
                Arrays.stream(packageFragment.getCompilationUnits()).forEach(icompUnit -> {
                    files.add(icompUnit);
                });
            }
        }
        return files.toArray(new ICompilationUnit[0]);
    }

    /**
     * Getter for the underlying project
     */
    public IProject getProject() {
        return project;
    }

    /**
     * Returns the name of the project
     */
    public String getName() {
        return project.getName();
    }
}
