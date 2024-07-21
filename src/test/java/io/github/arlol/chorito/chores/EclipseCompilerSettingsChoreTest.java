package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class EclipseCompilerSettingsChoreTest {

	private static final String EXPECTED_JDT_CORE_PREFS = """
			eclipse.preferences.version=1
			org.eclipse.jdt.core.builder.annotationPath.allLocations=disabled
			org.eclipse.jdt.core.compiler.annotation.inheritNullAnnotations=disabled
			org.eclipse.jdt.core.compiler.annotation.missingNonNullByDefaultAnnotation=ignore
			org.eclipse.jdt.core.compiler.annotation.nonnull=javax.annotation.Nonnull
			org.eclipse.jdt.core.compiler.annotation.nonnull.secondary=org.eclipse.jdt.annotation.NonNull
			org.eclipse.jdt.core.compiler.annotation.nonnullbydefault=javax.annotation.ParametersAreNonnullByDefault
			org.eclipse.jdt.core.compiler.annotation.nonnullbydefault.secondary=org.eclipse.jdt.annotation.NonNullByDefault
			org.eclipse.jdt.core.compiler.annotation.notowning=org.eclipse.jdt.annotation.NotOwning
			org.eclipse.jdt.core.compiler.annotation.nullable=javax.annotation.Nullable
			org.eclipse.jdt.core.compiler.annotation.nullable.secondary=org.eclipse.jdt.annotation.Nullable
			org.eclipse.jdt.core.compiler.annotation.nullanalysis=enabled
			org.eclipse.jdt.core.compiler.annotation.owning=org.eclipse.jdt.annotation.Owning
			org.eclipse.jdt.core.compiler.annotation.resourceanalysis=disabled
			org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled
			org.eclipse.jdt.core.compiler.codegen.methodParameters=generate
			org.eclipse.jdt.core.compiler.codegen.targetPlatform=21
			org.eclipse.jdt.core.compiler.codegen.unusedLocal=preserve
			org.eclipse.jdt.core.compiler.compliance=21
			org.eclipse.jdt.core.compiler.debug.lineNumber=generate
			org.eclipse.jdt.core.compiler.debug.localVariable=generate
			org.eclipse.jdt.core.compiler.debug.sourceFile=generate
			org.eclipse.jdt.core.compiler.problem.APILeak=warning
			org.eclipse.jdt.core.compiler.problem.annotatedTypeArgumentToUnannotated=info
			org.eclipse.jdt.core.compiler.problem.annotationSuperInterface=warning
			org.eclipse.jdt.core.compiler.problem.assertIdentifier=error
			org.eclipse.jdt.core.compiler.problem.autoboxing=ignore
			org.eclipse.jdt.core.compiler.problem.comparingIdentical=warning
			org.eclipse.jdt.core.compiler.problem.deadCode=warning
			org.eclipse.jdt.core.compiler.problem.deprecation=warning
			org.eclipse.jdt.core.compiler.problem.deprecationInDeprecatedCode=disabled
			org.eclipse.jdt.core.compiler.problem.deprecationWhenOverridingDeprecatedMethod=disabled
			org.eclipse.jdt.core.compiler.problem.discouragedReference=warning
			org.eclipse.jdt.core.compiler.problem.emptyStatement=ignore
			org.eclipse.jdt.core.compiler.problem.enablePreviewFeatures=disabled
			org.eclipse.jdt.core.compiler.problem.enumIdentifier=error
			org.eclipse.jdt.core.compiler.problem.explicitlyClosedAutoCloseable=ignore
			org.eclipse.jdt.core.compiler.problem.fallthroughCase=ignore
			org.eclipse.jdt.core.compiler.problem.fatalOptionalError=disabled
			org.eclipse.jdt.core.compiler.problem.fieldHiding=ignore
			org.eclipse.jdt.core.compiler.problem.finalParameterBound=warning
			org.eclipse.jdt.core.compiler.problem.finallyBlockNotCompletingNormally=warning
			org.eclipse.jdt.core.compiler.problem.forbiddenReference=warning
			org.eclipse.jdt.core.compiler.problem.hiddenCatchBlock=warning
			org.eclipse.jdt.core.compiler.problem.includeNullInfoFromAsserts=disabled
			org.eclipse.jdt.core.compiler.problem.incompatibleNonInheritedInterfaceMethod=warning
			org.eclipse.jdt.core.compiler.problem.incompatibleOwningContract=warning
			org.eclipse.jdt.core.compiler.problem.incompleteEnumSwitch=warning
			org.eclipse.jdt.core.compiler.problem.indirectStaticAccess=ignore
			org.eclipse.jdt.core.compiler.problem.insufficientResourceAnalysis=warning
			org.eclipse.jdt.core.compiler.problem.localVariableHiding=ignore
			org.eclipse.jdt.core.compiler.problem.methodWithConstructorName=warning
			org.eclipse.jdt.core.compiler.problem.missingDefaultCase=ignore
			org.eclipse.jdt.core.compiler.problem.missingDeprecatedAnnotation=ignore
			org.eclipse.jdt.core.compiler.problem.missingEnumCaseDespiteDefault=disabled
			org.eclipse.jdt.core.compiler.problem.missingHashCodeMethod=ignore
			org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotation=ignore
			org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotationForInterfaceMethodImplementation=enabled
			org.eclipse.jdt.core.compiler.problem.missingSerialVersion=warning
			org.eclipse.jdt.core.compiler.problem.missingSynchronizedOnInheritedMethod=ignore
			org.eclipse.jdt.core.compiler.problem.noEffectAssignment=warning
			org.eclipse.jdt.core.compiler.problem.noImplicitStringConversion=warning
			org.eclipse.jdt.core.compiler.problem.nonExternalizedStringLiteral=ignore
			org.eclipse.jdt.core.compiler.problem.nonnullParameterAnnotationDropped=warning
			org.eclipse.jdt.core.compiler.problem.nonnullTypeVariableFromLegacyInvocation=warning
			org.eclipse.jdt.core.compiler.problem.nullAnnotationInferenceConflict=error
			org.eclipse.jdt.core.compiler.problem.nullReference=warning
			org.eclipse.jdt.core.compiler.problem.nullSpecViolation=warning
			org.eclipse.jdt.core.compiler.problem.nullUncheckedConversion=warning
			org.eclipse.jdt.core.compiler.problem.overridingPackageDefaultMethod=warning
			org.eclipse.jdt.core.compiler.problem.parameterAssignment=ignore
			org.eclipse.jdt.core.compiler.problem.pessimisticNullAnalysisForFreeTypeVariables=warning
			org.eclipse.jdt.core.compiler.problem.possibleAccidentalBooleanAssignment=ignore
			org.eclipse.jdt.core.compiler.problem.potentialNullReference=error
			org.eclipse.jdt.core.compiler.problem.potentiallyUnclosedCloseable=ignore
			org.eclipse.jdt.core.compiler.problem.rawTypeReference=warning
			org.eclipse.jdt.core.compiler.problem.redundantNullAnnotation=warning
			org.eclipse.jdt.core.compiler.problem.redundantNullCheck=ignore
			org.eclipse.jdt.core.compiler.problem.redundantSpecificationOfTypeArguments=ignore
			org.eclipse.jdt.core.compiler.problem.redundantSuperinterface=ignore
			org.eclipse.jdt.core.compiler.problem.reportMethodCanBePotentiallyStatic=ignore
			org.eclipse.jdt.core.compiler.problem.reportMethodCanBeStatic=ignore
			org.eclipse.jdt.core.compiler.problem.reportPreviewFeatures=warning
			org.eclipse.jdt.core.compiler.problem.specialParameterHidingField=disabled
			org.eclipse.jdt.core.compiler.problem.staticAccessReceiver=warning
			org.eclipse.jdt.core.compiler.problem.suppressOptionalErrors=disabled
			org.eclipse.jdt.core.compiler.problem.suppressWarnings=enabled
			org.eclipse.jdt.core.compiler.problem.suppressWarningsNotFullyAnalysed=info
			org.eclipse.jdt.core.compiler.problem.syntacticNullAnalysisForFields=enabled
			org.eclipse.jdt.core.compiler.problem.syntheticAccessEmulation=ignore
			org.eclipse.jdt.core.compiler.problem.terminalDeprecation=warning
			org.eclipse.jdt.core.compiler.problem.typeParameterHiding=warning
			org.eclipse.jdt.core.compiler.problem.unavoidableGenericTypeProblems=enabled
			org.eclipse.jdt.core.compiler.problem.uncheckedTypeOperation=warning
			org.eclipse.jdt.core.compiler.problem.unclosedCloseable=warning
			org.eclipse.jdt.core.compiler.problem.undocumentedEmptyBlock=ignore
			org.eclipse.jdt.core.compiler.problem.unhandledWarningToken=warning
			org.eclipse.jdt.core.compiler.problem.unlikelyCollectionMethodArgumentType=warning
			org.eclipse.jdt.core.compiler.problem.unlikelyCollectionMethodArgumentTypeStrict=disabled
			org.eclipse.jdt.core.compiler.problem.unlikelyEqualsArgumentType=info
			org.eclipse.jdt.core.compiler.problem.unnecessaryElse=ignore
			org.eclipse.jdt.core.compiler.problem.unnecessaryTypeCheck=ignore
			org.eclipse.jdt.core.compiler.problem.unqualifiedFieldAccess=ignore
			org.eclipse.jdt.core.compiler.problem.unstableAutoModuleName=warning
			org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownException=ignore
			org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionExemptExceptionAndThrowable=enabled
			org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionIncludeDocCommentReference=enabled
			org.eclipse.jdt.core.compiler.problem.unusedDeclaredThrownExceptionWhenOverriding=disabled
			org.eclipse.jdt.core.compiler.problem.unusedExceptionParameter=ignore
			org.eclipse.jdt.core.compiler.problem.unusedImport=warning
			org.eclipse.jdt.core.compiler.problem.unusedLabel=warning
			org.eclipse.jdt.core.compiler.problem.unusedLocal=warning
			org.eclipse.jdt.core.compiler.problem.unusedObjectAllocation=ignore
			org.eclipse.jdt.core.compiler.problem.unusedParameter=ignore
			org.eclipse.jdt.core.compiler.problem.unusedParameterIncludeDocCommentReference=enabled
			org.eclipse.jdt.core.compiler.problem.unusedParameterWhenImplementingAbstract=disabled
			org.eclipse.jdt.core.compiler.problem.unusedParameterWhenOverridingConcrete=disabled
			org.eclipse.jdt.core.compiler.problem.unusedPrivateMember=warning
			org.eclipse.jdt.core.compiler.problem.unusedTypeParameter=ignore
			org.eclipse.jdt.core.compiler.problem.unusedWarningToken=warning
			org.eclipse.jdt.core.compiler.problem.varargsArgumentNeedCast=warning
			org.eclipse.jdt.core.compiler.processAnnotations=disabled
			org.eclipse.jdt.core.compiler.release=enabled
			org.eclipse.jdt.core.compiler.source=21
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new EclipseCompilerSettingsChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void test() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path jdtCorePrefs = extension.root()
				.resolve(".settings/org.eclipse.jdt.core.prefs");

		assertThat(jdtCorePrefs).exists();
		assertThat(jdtCorePrefs).isNotEmptyFile();
		assertThat(jdtCorePrefs).content().isEqualTo(EXPECTED_JDT_CORE_PREFS);
	}

}