package com.sternitc.task.architecture;

import com.sternitc.task.domain.model.Executable;
import com.sternitc.task.domain.model.ExecutableContext;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(
        packages = "com.sternitc.task",
        importOptions = ImportOption.OnlyIncludeTests.class
)
class TestBoundaryArchitectureTest {

    private static final String PORTS_IN_PACKAGE = "com.sternitc.task.ports.in..";
    private static final String ARCHITECTURE_PACKAGE = "com.sternitc.task.architecture..";

    private static final Map<String, Set<String>> FORBIDDEN_DOMAIN_METHODS = Map.of(
            Task.class.getName(), Set.of("plan", "reconstitute", "restore", "update", "isExecutable"),
            Executable.class.getName(), Set.of("isExecutable"),
            ExecutableContext.class.getName(), Set.of("evaluate"),
            StoredCommand.class.getName(), Set.of("accepted", "reconstitute", "markCompleted", "markFailed"),
            TaskSearchCriteria.class.getName(), Set.of("matches")
    );

    @ArchTest
    static final ArchRule testClassesShouldResideInPortsInOrArchitecture =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Test")
                    .should()
                    .resideInAnyPackage(PORTS_IN_PACKAGE, ARCHITECTURE_PACKAGE);

    @ArchTest
    static final ArchRule portTestsShouldNotInvokeDomainBehaviorOrEvaluationMethodsDirectly =
            noClasses()
                    .that()
                    .resideInAPackage(PORTS_IN_PACKAGE)
                    .should()
                    .callMethodWhere(directDomainBehaviorOrEvaluationCall());

    @ArchTest
    static void everyInboundPortShouldHaveAtLeastOneTestClass(JavaClasses testClasses) {
        JavaClasses productionInboundPorts = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.sternitc.task.ports.in");

        List<String> inboundPortInterfaces = productionInboundPorts.stream()
                .filter(JavaClass::isInterface)
                .map(JavaClass::getSimpleName)
                .toList();

        assertThat(inboundPortInterfaces).isNotEmpty();

        Set<String> portTestSimpleNames = testClasses.stream()
                .filter(clazz -> clazz.getPackageName().startsWith("com.sternitc.task.ports.in"))
                .map(JavaClass::getSimpleName)
                .filter(name -> name.endsWith("Test"))
                .collect(java.util.stream.Collectors.toSet());

        for (String portInterface : inboundPortInterfaces) {
            boolean hasMatchingTest = portTestSimpleNames.stream()
                    .anyMatch(testName -> testName.startsWith(portInterface));
            assertThat(hasMatchingTest)
                    .as("Expected at least one test class starting with '%s' and ending with 'Test' in com.sternitc.task.ports.in", portInterface)
                    .isTrue();
        }
    }

    private static DescribedPredicate<JavaMethodCall> directDomainBehaviorOrEvaluationCall() {
        return new DescribedPredicate<>("call domain behavior, state-transition, or evaluation methods directly") {
            @Override
            public boolean test(JavaMethodCall call) {
                Set<String> forbiddenMethods = FORBIDDEN_DOMAIN_METHODS.get(call.getTargetOwner().getFullName());
                return forbiddenMethods != null && forbiddenMethods.contains(call.getName());
            }
        };
    }
}
