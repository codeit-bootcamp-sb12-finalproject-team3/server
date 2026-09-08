package com.moduplaylist.realtime;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTest {

    @Test
    void appRealtimeShouldNotDependOnAppApiOrAppBatch() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("com.moduplaylist");

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.moduplaylist.realtime..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.moduplaylist.api..", "com.moduplaylist.batch..");

        rule.check(importedClasses);
    }
}