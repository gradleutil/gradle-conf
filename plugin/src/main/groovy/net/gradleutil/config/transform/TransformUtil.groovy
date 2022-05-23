package net.gradleutil.config.transform

import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.gradle.api.Project
import org.gradle.api.internal.GradleInternal
import org.gradle.groovy.scripts.DefaultScript
import org.gradle.groovy.scripts.TextResourceScriptSource
import org.gradle.internal.resource.StringTextResource
import org.gradle.internal.service.ServiceRegistry

@Slf4j
class TransformUtil {


    static SourceUnit getSourceUnit(String sourceText) {
        SourceUnit unit = SourceUnit.create("gradle", sourceText)
        unit.parse()
        unit.completePhase()
        unit.convert()
        return unit
    }

    static void delegateTo(String sourceText, Object delegate, List<String> methodNames, Binding binding) {
        def compilerConfiguration = new CompilerConfiguration()
        def configTransformer = new ClosureTransformer()
        configTransformer.targetMethodNames = methodNames
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(configTransformer))
        compilerConfiguration.scriptBaseClass = DelegatingScript.class.name
        def shell = new GroovyShell(delegate.class.classLoader, binding, compilerConfiguration)
        log.info("parsing build and delegating closure to delegate")
        def script = shell.parse(sourceText)
        script.setDelegate(delegate)
        log.info("parsed build, running...")
        try {
            script.run()
        } catch (Exception ignored) {
            ignored.printStackTrace()
        }
        catch (Error ignored) {
            println ignored.message
        }
    }

    static void runClosuresOnObjectsFromSource(String sourceText, Project project, Map<String, Object> closureObjects) {
        def unit = getSourceUnit(sourceText)
        def compilerConfiguration = new CompilerConfiguration()
//        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(this))
        compilerConfiguration.scriptBaseClass = DefaultScript.class.name
        def binding = new Binding()
        def closureSources = getMethodSources(unit, closureObjects*.key.toList())
        def targetSourceText = new StringBuilder()
        log.debug("Running closures from source")
        closureObjects.each { String closureName, Object object ->
            assert !(object instanceof String)
            binding.setProperty(closureName, object)
            closureSources.findAll { it.containsKey(closureName) }*.values()*.each {
                targetSourceText.append("${closureName}.with${it}")
            }
        }
        def shell = new GroovyShell(binding, compilerConfiguration)
        def script = shell.parse(targetSourceText.toString()) as DefaultScript
        script.setScriptSource(new TextResourceScriptSource(new StringTextResource('script', targetSourceText.toString())))
        GradleInternal gradleInternal = (GradleInternal) project.getGradle()
        ServiceRegistry serviceRegistry = gradleInternal.getServices()
        script.init(project, serviceRegistry)
        script.run()
    }

    static List<Map<String, String>> getMethodSources(SourceUnit sourceUnit, List<String> targetMethodNames) {
        transform(null, sourceUnit, targetMethodNames).targetMethodSources
    }

    static List<Map<String, String>> getMethodSources(String sourceText, List<String> targetMethodNames) {
        transform(null, getSourceUnit(sourceText), targetMethodNames).targetMethodSources
    }

    static ClosureTrap transform(ASTNode[] nodes, SourceUnit sourceUnit, List<String> targetMethodNames) {


        ClosureTrap transformer = new ClosureTrap(
                sourceUnit,
                targetMethodNames
        )
        if (nodes != null) {
            for (ASTNode it : nodes) {
                if (!(it instanceof AnnotationNode) && !(it instanceof ClassNode)) {
                    it.visit(transformer)
                }
            }
        }
        if (sourceUnit.getAST() != null) {
            sourceUnit.getAST().visit(transformer)
            if (sourceUnit.getAST().getStatementBlock() != null) {
                sourceUnit.getAST().getStatementBlock().visit(transformer)
            }
            if (sourceUnit.getAST().getClasses() != null) {
                for (ClassNode classNode : sourceUnit.getAST().getClasses()) {
                    if (classNode.getMethods() != null) {
                        for (MethodNode node : classNode.getMethods()) {
                            if (node != null && node.getCode() != null) {
                                node.getCode().visit(transformer)
                            }
                        }
                    }

                    try {
                        if (classNode.getDeclaredConstructors() != null) {
                            for (MethodNode node : classNode.getDeclaredConstructors()) {
                                if (node != null && node.getCode() != null) {
                                    node.getCode().visit(transformer)
                                }
                            }
                        }
                    } catch (MissingPropertyException ignored) {
                    }

                    // all properties are also always fields
                    if (classNode.getFields() != null) {
                        for (FieldNode node : classNode.getFields()) {
                            if (node.getInitialValueExpression() != null) {
                                node.getInitialValueExpression().visit(transformer)
                            }
                        }
                    }

                    try {
                        if (classNode.getObjectInitializerStatements() != null) {
                            for (Statement node : classNode.getObjectInitializerStatements()) {
                                if (node != null) {
                                    node.visit(transformer)
                                }
                            }
                        }
                    } catch (MissingPropertyException ignored) {
                    }

                }
            }
            if (sourceUnit.getAST().getMethods() != null) {
                for (MethodNode node : sourceUnit.getAST().getMethods()) {
                    if (node != null) {
                        if (node.getParameters() != null) {
                            for (Parameter parameter : node.getParameters()) {
                                if (parameter != null && parameter.getInitialExpression() != null) {
                                    parameter.getInitialExpression().visit(transformer)
                                }
                            }
                        }
                        if (node.getCode() != null) {
                            node.getCode().visit(transformer)
                        }
                    }
                }
            }
        }
        transformer
    }
}
