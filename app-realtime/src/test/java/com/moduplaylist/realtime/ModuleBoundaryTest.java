package com.moduplaylist.realtime;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTest {

    @Test
    void appRealtimeShouldNotDependOnOtherProjectModules() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("com.moduplaylist");

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.moduplaylist.realtime..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.moduplaylist.api..",
                        "com.moduplaylist.batch..",
                        "com.moduplaylist.core..",
                        "com.moduplaylist.infrastructure.."
                );

        rule.check(importedClasses);
    }

    @Test
    void sseDeliveryShouldNotDependOnKafkaContracts() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("com.moduplaylist.realtime");

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.moduplaylist.realtime.notification.sse..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.moduplaylist.realtime.kafka..");

        rule.check(importedClasses);
    }
}
