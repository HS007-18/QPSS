package com.qpss.arch;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.qpss");

    @Test
    void backendShouldNotDependOnFrontend() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..backend..")
                .should().dependOnClassesThat().resideInAPackage("..frontend..");
        rule.check(classes);
    }

    @Test
    void frontendShouldOnlyDependOnBackendInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..frontend..")
                .should().dependOnClassesThat().resideInAPackage("..backend..")
                .andShould().dependOnClassesThat().resideInAPackage("..backend..domain")
                .andShould().dependOnClassesThat().resideInAPackage("..backend..dto");
        rule.check(classes);
    }

    @Test
    void commonShouldNotDependOnBackendOrFrontend() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..common..")
                .should().dependOnClassesThat().resideInAnyPackage("..backend..", "..frontend..");
        rule.check(classes);
    }
}