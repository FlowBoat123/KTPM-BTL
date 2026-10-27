package com.dental;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void businessCodeDoesNotDependOnWebOrDatabaseLibraries() {
    var classes = new ClassFileImporter().importPackages("com.dental");
    noClasses()
        .that()
        .resideInAPackage("com.dental.service..")
        .or()
        .haveFullyQualifiedName("com.dental.exception.BusinessException")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.http..",
            "org.springframework.web..",
            "jakarta.servlet..",
            "java.sql..",
            "javax.sql..",
            "jakarta.persistence..",
            "org.hibernate..",
            "org.postgresql..",
            "org.sqlite..",
            "org.springframework.jdbc..",
            "org.springframework.data..")
        .because("Phase 1 business rules must be independent of HTTP and database adapters")
        .check(classes);
  }
}
