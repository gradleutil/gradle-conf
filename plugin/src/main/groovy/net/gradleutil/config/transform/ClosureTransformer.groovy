package net.gradleutil.config.transform


import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * https://github.com/groovy/groovy-core/blob/master/src/main/org/codehaus/groovy/ast/builder/AstBuilderTransformation.java
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class ClosureTransformer implements ASTTransformation {

    List<String> targetMethodNames = []

    void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        TransformUtil.transform(nodes, sourceUnit, targetMethodNames)
    }


}


