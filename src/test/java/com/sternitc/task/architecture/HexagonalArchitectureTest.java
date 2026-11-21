package com.sternitc.task.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.sternitc.task",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    private static final String DOMAIN_PACKAGE = "com.sternitc.task.domain..";
    private static final String DOMAIN_MODEL_PACKAGE = "com.sternitc.task.domain.model..";
    private static final String DOMAIN_SERVICES_PACKAGE = "com.sternitc.task.domain.services..";
    private static final String APPLICATION_PACKAGE = "com.sternitc.task.application..";
    private static final String PORTS_PACKAGE = "com.sternitc.task.ports..";
    private static final String PORTS_IN_PACKAGE = "com.sternitc.task.ports.in..";
    private static final String PORTS_OUT_PACKAGE = "com.sternitc.task.ports.out..";
    private static final String ADAPTERS_IN_PACKAGE = "com.sternitc.task.adapters.in..";
    private static final String ADAPTERS_OUT_PACKAGE = "com.sternitc.task.adapters.out..";
    private static final String API_PACKAGE = "com.sternitc.task.api..";

    @ArchTest
    static final ArchRule domainClassesShouldResideInModelOrServices =
            classes()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .resideInAnyPackage(DOMAIN_MODEL_PACKAGE, DOMAIN_SERVICES_PACKAGE);

    @ArchTest
    static final ArchRule domainModelShouldOnlyDependOnJavaAndDomainModel =
            classes()
                    .that()
                    .resideInAPackage(DOMAIN_MODEL_PACKAGE)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", DOMAIN_MODEL_PACKAGE);

    @ArchTest
    static final ArchRule domainServicesShouldOnlyDependOnJavaDomainModelAndDomainServices =
            classes()
                    .that()
                    .resideInAPackage(DOMAIN_SERVICES_PACKAGE)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", DOMAIN_MODEL_PACKAGE, DOMAIN_SERVICES_PACKAGE)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inboundPortsShouldOnlyDependOnInboundPortsDomainModelAndJava =
            classes()
                    .that()
                    .resideInAPackage(PORTS_IN_PACKAGE)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", PORTS_IN_PACKAGE, DOMAIN_MODEL_PACKAGE);

    @ArchTest
    static final ArchRule outboundPortsShouldOnlyDependOnOutboundPortsDomainModelAndJava =
            classes()
                    .that()
                    .resideInAPackage(PORTS_OUT_PACKAGE)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", PORTS_OUT_PACKAGE, DOMAIN_MODEL_PACKAGE);

    @ArchTest
    static final ArchRule portsShouldNotDependOnApplicationOrDomainServices =
            noClasses()
                    .that()
                    .resideInAPackage(PORTS_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(APPLICATION_PACKAGE, DOMAIN_SERVICES_PACKAGE);

    @ArchTest
    static final ArchRule applicationShouldOnlyDependOnApplicationDomainPortsAndJava =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", APPLICATION_PACKAGE, DOMAIN_PACKAGE, PORTS_PACKAGE);

    @ArchTest
    static final ArchRule applicationClassesShouldImplementAtMostOneInboundPort =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .should(implementAtMostOneInboundPort());

    @ArchTest
    static final ArchRule inboundAdaptersShouldNotDependOnOutboundAdaptersOutboundPortsOrApplication =
            noClasses()
                    .that()
                    .resideInAPackage(ADAPTERS_IN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(ADAPTERS_OUT_PACKAGE, PORTS_OUT_PACKAGE, APPLICATION_PACKAGE);

    @ArchTest
    static final ArchRule outboundAdaptersShouldNotDependOnInboundAdaptersApiSpringInboundPortsOrApplication =
            noClasses()
                    .that()
                    .resideInAPackage(ADAPTERS_OUT_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            ADAPTERS_IN_PACKAGE,
                            API_PACKAGE,
                            "org.springframework..",
                            PORTS_IN_PACKAGE,
                            APPLICATION_PACKAGE
                    );

    private static ArchCondition<JavaClass> implementAtMostOneInboundPort() {
        return new ArchCondition<>("implement at most one ports.in interface") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                long inboundPortCount = item.getAllRawInterfaces().stream()
                        .filter(iface -> iface.getPackageName().startsWith("com.sternitc.task.ports.in"))
                        .count();
                if (inboundPortCount > 1) {
                    events.add(SimpleConditionEvent.violated(
                            item,
                            item.getName() + " implements " + inboundPortCount + " ports.in interfaces (max 1 allowed)"
                    ));
                }
            }
        };
    }
}
