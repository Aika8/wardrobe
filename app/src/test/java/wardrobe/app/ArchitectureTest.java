package wardrobe.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependencies point inward only (ADR-0002): domain ← app. */
@AnalyzeClasses(packages = {"wardrobe"}, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPlainJava = noClasses().that().resideInAPackage("wardrobe.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages("wardrobe.domain..", "java.lang..", "java.util..");

    @ArchTest
    static final ArchRule domainNeverReadsTheClock = noClasses().that().resideInAPackage("wardrobe.domain..")
            .should().callMethod(java.time.Instant.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .orShould().callMethod(System.class, "nanoTime");

    @ArchTest
    static final ArchRule domainKnowsNoAdapters = noClasses().that().resideInAPackage("wardrobe.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("wardrobe.app..", "org.springframework..",
                    "tools.jackson..", "com.fasterxml..");
}
