package jeaphunter.visitors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;

import jeaphunter.entities.JTryStatement;
import jeaphunter.util.ASTUtil;

public class Visitor extends ASTVisitor {

	public static final int MAX_DEPTH_OF_SEARCHING_INSIDE_METHOD_INVOCATIONS = 4;

	private Set<JTryStatement> jTryStatements = new HashSet<>();
	private int methodInvocationDepth = 1;

	JTryStatement rootTry;
	MethodDeclaration currentMethod;

	public Visitor(JTryStatement rootTry, MethodDeclaration currentMethod) {
		this.rootTry = rootTry;
		this.currentMethod = currentMethod;
	}

	public Visitor(JTryStatement parentTry, MethodDeclaration currentMethod, int methodInvocationDepth) {
		this(parentTry, currentMethod);
		this.methodInvocationDepth = methodInvocationDepth;
	}

	@Override
	public boolean visit(TryStatement node) {

		// Ignore try without catch but consider its body
		if (node.catchClauses() == null || node.catchClauses().size() == 0)
			return true;

		JTryStatement jTry = new JTryStatement(node);

		// Don't process if no catch clauses?

		jTry.setParentTry(rootTry);
		//jTry.addCatchClauses(node.catchClauses());
		jTry.setBody(node.getBody());

		ASTNode root = node.getRoot();
		if (root instanceof CompilationUnit) {

			CompilationUnit cu = (CompilationUnit) root;
			jTry.setSoureFilePath(cu.getTypeRoot().getElementName());

			int lineNo = cu.getLineNumber(node.getStartPosition());
			int columnNo = cu.getColumnNumber(node.getStartPosition());
			jTry.setStartLineInSource(lineNo);
			jTry.setStartColumnInSource(columnNo);
		}

		jTryStatements.add(jTry);
		rootTry.addToNestedTryStatements(jTry);

		Visitor v = new Visitor(jTry, currentMethod, methodInvocationDepth);
		node.getBody().accept(v);

		// Skip child block visits ?
		return false;//
	}

	@Override
	public boolean visit(MethodInvocation node) {

		MethodDeclaration declartion = ASTUtil.declarationFromInvocation(node);
		List<Type> thrownFromSignature = null;
		Javadoc javadoc = null;
		if (declartion != null) {
			thrownFromSignature = declartion.thrownExceptionTypes();
			javadoc = declartion.getJavadoc();
		} else {

			IMethodBinding binding = node.resolveMethodBinding();

			if (binding != null) {

//				if (binding.getJavaElement() instanceof IMember) {
//					IMember member = (IMember) binding.getJavaElement();
//
//					try {
//						// String html = member.getAttachedJavadoc(null);
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						// e.printStackTrace();
//					}
//				}

				IMethodBinding thridPartyDeclaration = binding.getMethodDeclaration();
				// TODO find ejavadoc
				if (thridPartyDeclaration != null) {
					ITypeBinding[] types = thridPartyDeclaration.getExceptionTypes();
					if (types != null && types.length > 0)
						for (ITypeBinding exp : types) {
							rootTry.addToThrownExceptionTypes(exp);
						}
				}
			} else {

				System.out.println("Cannot resolve method declaration for " + node);
			}
		}

		// Check if thrown from method signature
		if (thrownFromSignature != null) {
			for (Type type : thrownFromSignature) {
				ITypeBinding typeBinding = type.resolveBinding();

				if (typeBinding == null) {
					System.out.println("Cannot resolve binding for thow :" + type + "in " + node.getName());
				} else {
					rootTry.addToThrownExceptionTypes(typeBinding);
				}
			}
		}

		// Check if throwns from javadoc
		if (javadoc != null) {
			List<ITypeBinding> thrownTypes = ASTUtil.getThrowableExceptionsFromJavadoc(javadoc);
			for (ITypeBinding typeBinding : thrownTypes) {
				rootTry.addToThrownExceptionTypes(typeBinding);
			}
		}

		// Traverse declaration if depth is not reached
		if (declartion != null && methodInvocationDepth < MAX_DEPTH_OF_SEARCHING_INSIDE_METHOD_INVOCATIONS) {
			if (!declartion.equals(currentMethod)) {
				Visitor visitor = new Visitor(rootTry, declartion, methodInvocationDepth + 1);
				declartion.accept(visitor);
			}
		}

		rootTry.addToInvokedMethods(node);
		return false;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		IMethodBinding constructor = node.resolveConstructorBinding();
		if (constructor != null) {
			ITypeBinding[] exceptions = constructor.getExceptionTypes();
			if (exceptions != null) {
				for (ITypeBinding exception : exceptions) {
					rootTry.addToThrownExceptionTypes(exception);
				}
			}
		}
		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		ITypeBinding typeBinding = null;
		rootTry.addToThrowedStatements(node);
		try {
			Expression expression = node.getExpression();
			if (expression instanceof ClassInstanceCreation) {
				typeBinding = ((ClassInstanceCreation) expression).resolveTypeBinding();
			} else if (expression instanceof SimpleName) {
				SimpleName simpleName = ((SimpleName) expression);
				typeBinding = simpleName.resolveTypeBinding();
			} else if (expression instanceof MethodInvocation) {
				MethodInvocation methodInvocation = ((MethodInvocation) expression);
				MethodDeclaration declaration = ASTUtil.declarationFromInvocation(methodInvocation);
				if (declaration != null) {
					// get return type from the expression
					typeBinding = declaration.getReturnType2().resolveBinding();
				}
				// TOOD: Also handle other probable cases
			}
		} catch (Exception ex) {
			System.out.println("Exception determining the type of thrown exception type, ignoring: " + node);
		}

		if (typeBinding == null) {
			System.out.println("Cannot determine the type of thrown exception type, ignoring: " + node);
		} else {
			rootTry.addToThrownExceptionTypes(typeBinding);
		}
		return false;
	}

	public Set<JTryStatement> getTryStatements() {
		return jTryStatements;
	}
}
