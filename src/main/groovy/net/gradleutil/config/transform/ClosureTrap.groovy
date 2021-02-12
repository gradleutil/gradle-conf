package net.gradleutil.config.transform

import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.tools.ClosureUtils
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.io.ReaderSource
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException

/**
 * This class traps closure invocations and applies them to the supplied objects
 */
@Slf4j
class ClosureTrap extends CodeVisitorSupport {

    private final List factoryTargets = []
    private final ReaderSource readerSource
    private final SourceUnit sourceUnit
    List<String> targetMethodNames = []
    List<Map<String, String>> targetMethodSources = []
    List<String> skipMethodNames = ['setBinding', 'run']

    /**
     * Creates the trap and captures all the ways in which a class may be referenced via imports.
     * @param imports
     *      all the imports from the source
     * @param importPackages
     *      all the imported packages from the source
     * @param source
     *      the reader source that contains source for the SourceUnit
     * @param sourceUnit
     *      the source unit being compiled. Used for error messages.
     */
    ClosureTrap(SourceUnit sourceUnit, List<String> targetMethodNames) {
        this.sourceUnit = sourceUnit
        if (!this.sourceUnit) throw new IllegalArgumentException("Null: sourceUnit")
        readerSource = sourceUnit.source
        if (!readerSource) throw new IllegalArgumentException("Null: source")
        this.targetMethodNames = targetMethodNames

        // factory type may be references as fully qualified, an import, or an alias
        factoryTargets.add("java.lang.Object")//default package
        def imports = sourceUnit.getAST().imports
        def importPackages = sourceUnit.getAST().starImports

        if (imports != null) {
            for (ImportNode importStatement : imports) {
                if ("net.gradleutil.config.ConfConfigConvention" == importStatement.getType().getName()) {
                    factoryTargets.add(importStatement.getAlias())
                }
            }
        }

        if (importPackages != null) {
            for (ImportNode importPackage : importPackages) {
                if ("net.gradleutil.config." == importPackage.getPackageName()) {
                    factoryTargets.add("AstBuilder")
                    break
                }
            }
        }

    }

    /**
     * Reports an error back to the source unit.
     *
     * @param msg the error message
     * @param expr the expression that caused the error message.
     */
    private void addError(String msg, ASTNode expr) {
        sourceUnit.getErrorCollector().addErrorAndContinue(
                new SyntaxErrorMessage(new SyntaxException(msg + '\n', expr.getLineNumber(), expr.getColumnNumber(), expr.getLastLineNumber(), expr.getLastColumnNumber()), sourceUnit)
        )
    }

    @Override
    void visitAssertStatement(AssertStatement statement) {
        statement.booleanExpression = new BooleanExpression(new ConstantExpression('true'))
    }

    /**
     * Attempts to find specified invocations.
     *
     * @param call
     *       the method call expression that may or may not be an AstBuilder 'from code' invocation.
     */
    void visitMethodCallExpression(MethodCallExpression call) {

        if (isClosureInvocation(call)) {
            log.info("found method call")

            ClosureExpression closureExpression = getClosureArgument(call)
            List<Expression> otherArgs = getNonClosureArguments(call)
            String source = convertClosureToSource(closureExpression)
            targetMethodSources.add([(call.methodAsString): source])
            if(otherArgs){
                log.warn("Ignoring other closure args: ${otherArgs.collect {convertExpressionToSource(it)}.join('\n')}")
            }

            call.objectExpression = new VariableExpression('confConfigConvention')
            call.method = new ConstantExpression('with')
            call.spreadSafe = false
            call.safe = false
            call.implicitThis = false

        } else if (!skipMethodNames.contains(call.methodAsString)) {

            // TODO: something better
            call.objectExpression = new ConstantExpression('System.out')
            call.arguments = new ConstantExpression('')
            call.method = new ConstantExpression('print')
        }
    }


    /**
     * Looks for method calls that take a Closure as parameter. This is all needed b/c build is overloaded.
     * @param call
     *      the method call expression, may not be null
     */
    private boolean isClosureInvocation(MethodCallExpression call) {
        if (call == null) throw new IllegalArgumentException('Null: call')

        // is method name correct?
        if (call.method instanceof ConstantExpression && targetMethodNames.contains(call.method?.value)) {

            // is method object correct type?
            String name = call.objectExpression?.type?.name
            if (name && factoryTargets.contains(name)) {

                // is one of the arguments a closure?
                if (call.arguments && call.arguments instanceof TupleExpression) {
                    if (call.arguments.expressions?.find { it instanceof ClosureExpression }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private static List<Expression> getNonClosureArguments(MethodCallExpression call) {
        List<Expression> result = new ArrayList<Expression>()
        if (call.getArguments() instanceof TupleExpression) {
            for (ASTNode node : ((TupleExpression) call.getArguments()).getExpressions()) {
                if (!(node instanceof ClosureExpression)) {
                    result.add((Expression) node)
                }
            }
        }
        return result
    }

    private static ClosureExpression getClosureArgument(MethodCallExpression call) {

        if (call.getArguments() instanceof TupleExpression) {
            for (ASTNode node : ((TupleExpression) call.getArguments()).getExpressions()) {
                if (node instanceof ClosureExpression) {
                    return (ClosureExpression) node
                }
            }
        }
        return null
    }

    /**
     * Converts an Expression into the String source.
     * @param expression an expression
     * @return the source the expression was created from
     */
    private String convertExpressionToSource(Expression expression) {
        if (expression == null) throw new IllegalArgumentException('Null: expression')

        def lineRange = (expression.lineNumber..expression.lastLineNumber)

        def source = lineRange.collect {
            def line = readerSource.getLine(it, null)
            if (line == null) {
                addError(
                        "Error calculating source code for expression. Trying to read line $it from ${readerSource.class}",
                        expression
                )
            }
            if (it == expression.lastLineNumber) {
                line = line.substring(0, expression.lastColumnNumber - 1)
            }
            if (it == expression.lineNumber) {
                line = line.substring(expression.columnNumber - 1)
            }
            return line
        }?.join('\n')?.trim()   //restoring line breaks is important b/c of lack of semicolons

        if (!source.startsWith('{')) {
            addError(
                    'Error converting ClosureExpression into source code. ' +
                            "Closures must start with {. Found: $source",
                    expression
            )
        }

        return source
    }

    /**
     * Converts a ClosureExpression into the String source.
     *
     * @param expression a closure
     * @return the source the closure was created from
     */
    private String convertClosureToSource(ClosureExpression expression) {
        try {
            return ClosureUtils.convertClosureToSource(readerSource, expression)
        } catch (Exception e) {
            addError(e.getMessage(), expression)
        }
        return null
    }
}