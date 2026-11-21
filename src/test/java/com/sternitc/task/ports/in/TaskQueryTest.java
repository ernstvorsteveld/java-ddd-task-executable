package com.sternitc.task.ports.in;

import com.sternitc.task.adapters.out.inmemory.InMemoryEntityType1Repository;
import com.sternitc.task.adapters.out.inmemory.InMemoryEntityType2Repository;
import com.sternitc.task.adapters.out.inmemory.InMemoryEntityType3Repository;
import com.sternitc.task.adapters.out.inmemory.InMemoryEntityType4Repository;
import com.sternitc.task.adapters.out.mongo.MongoTaskRepository;
import com.sternitc.task.application.ExecutableContextResolver;
import com.sternitc.task.application.TaskQueryService;
import com.sternitc.task.domain.model.CommandExecutionStatus;
import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.DefaultExecutableContext;
import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType1;
import com.sternitc.task.domain.model.EntityType2;
import com.sternitc.task.domain.model.EntityType3;
import com.sternitc.task.domain.model.EntityType4;
import com.sternitc.task.domain.model.EntityTypeName;
import com.sternitc.task.domain.model.Executable;
import com.sternitc.task.domain.model.InvalidPageRequestException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.LinkedEntities;
import com.sternitc.task.domain.model.LinkedEntity;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.RoleName;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import com.sternitc.task.domain.model.TaskStatus;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.domain.model.UserId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaskQuery Inbound Port")
class TaskQueryTest extends InboundPortTestSupport {

    @Nested
    @DisplayName("Requirement 3: Query & Search Tasks")
    class QueryTasksSpec {

        @Test
        @DisplayName("3.1 returns the matching task when queried by existing TaskId")
        void findsTaskById() {
            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            Optional<Task> found = taskQuery.findById(created.id());

            assertThat(found).contains(created);
            assertThat(found.orElseThrow().type()).isEqualTo(TaskType.DO_AANVRAAG);
            assertThat(found.orElseThrow().owner()).isEqualTo(ProcessName.PS_IE);
        }

        @Test
        @DisplayName("3.1 returns empty when queried by an unknown TaskId")
        void returnsEmptyForUnknownTaskId() {
            Optional<Task> found = taskQuery.findById(new TaskId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("3.2 returns only tasks assigned to the queried User or Role")
        void findsTasksFilteredByUserOrRole() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            var bob = new TaskExecutor.User(new UserId("bob"));
            var approvers = new TaskExecutor.Role(new RoleName("APPROVER"));

            Task aliceTask1 = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG, ProcessName.PS_IE, new PlanningWindow(START_BEFORE, FINISH_BEFORE), alice
            ));
            planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_OPDRACHT, ProcessName.PS_UE, new PlanningWindow(START_BEFORE, FINISH_BEFORE), bob
            ));
            Task roleTask = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_CONTRACT, ProcessName.PO_VG, new PlanningWindow(START_BEFORE, FINISH_BEFORE), approvers
            ));
            Task aliceTask2 = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_OFFERTE, ProcessName.PO_BO, new PlanningWindow(START_BEFORE, FINISH_BEFORE), alice
            ));

            assertThat(taskQuery.findByExecutor(FindTasksByExecutorQuery.of(alice)))
                    .containsExactly(aliceTask1, aliceTask2);
            assertThat(taskQuery.findByExecutor(FindTasksByExecutorQuery.of(approvers)))
                    .containsExactly(roleTask);
            assertThat(taskQuery.findByExecutor(FindTasksByExecutorQuery.of(new TaskExecutor.Role(new RoleName("OTHER")))))
                    .isEmpty();
        }

        @Test
        @DisplayName("3.2 defaults pageNumber to 0 and pageSize to 20 and slices pages using 0-based pageNumber")
        void paginatesTasksWithDefaultsAndCustomPages() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            List<Task> created = IntStream.range(0, 25)
                    .mapToObj(i -> planAndAwaitTask(new PlanTaskCommand(
                            TaskType.DO_AANVRAAG,
                            ProcessName.PS_IE,
                            new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                            alice
                    )))
                    .toList();

            FindTasksByExecutorQuery defaultQuery = FindTasksByExecutorQuery.of(alice);
            assertThat(defaultQuery.pageNumber()).isEqualTo(0);
            assertThat(defaultQuery.pageSize()).isEqualTo(20);

            List<Task> firstDefaultPage = taskQuery.findByExecutor(defaultQuery);
            List<Task> secondPage = taskQuery.findByExecutor(new FindTasksByExecutorQuery(alice, 1, 20));
            List<Task> outOfRangePage = taskQuery.findByExecutor(new FindTasksByExecutorQuery(alice, 2, 20));

            assertThat(firstDefaultPage).containsExactlyElementsOf(created.subList(0, 20));
            assertThat(secondPage).containsExactlyElementsOf(created.subList(20, 25));
            assertThat(outOfRangePage).isEmpty();
        }

        @Test
        @DisplayName("3.3 searches tasks using optional executor, status, type, and owner filters")
        void searchesTasksWithOptionalFilters() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            var approvers = new TaskExecutor.Role(new RoleName("APPROVER"));

            Task t1 = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG, ProcessName.PS_IE, new PlanningWindow(START_BEFORE, FINISH_BEFORE), alice
            ));
            Task t2 = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_CONTRACT, ProcessName.PO_VG, new PlanningWindow(START_BEFORE, FINISH_BEFORE), approvers
            ));
            Task t3 = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG, ProcessName.PS_IE, new PlanningWindow(START_BEFORE, FINISH_BEFORE), alice
            ));
            CommandReceipt updateReceipt = updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    t3.id(), TaskStatus.IN_PROGRESS, new PlanningWindow(START_BEFORE, FINISH_BEFORE), alice
            ));
            awaitCommandStatus(updateReceipt, CommandExecutionStatus.COMPLETED);
            Task t3InProgress = taskQuery.findById(t3.id()).orElseThrow();

            assertThat(taskQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty())))
                    .containsExactly(t1, t2, t3InProgress);
            assertThat(taskQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty().withStatus(TaskStatus.IN_PROGRESS))))
                    .containsExactly(t3InProgress);
            assertThat(taskQuery.search(SearchTasksQuery.of(
                    TaskSearchCriteria.empty()
                            .withExecutor(alice)
                            .withType(TaskType.DO_AANVRAAG)
                            .withOwner(ProcessName.PS_IE)
                            .withStatus(TaskStatus.CREATED)
            ))).containsExactly(t1);
            assertThat(taskQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty().withOwner(ProcessName.PO_UG))))
                    .isEmpty();
        }

        @Test
        @DisplayName("3.3 defaults pageNumber to 0 and pageSize to 20 and slices matching search results")
        void paginatesSearchResultsWithDefaults() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            List<Task> created = IntStream.range(0, 23)
                    .mapToObj(i -> planAndAwaitTask(new PlanTaskCommand(
                            TaskType.DO_OFFERTE,
                            ProcessName.PO_BO,
                            new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                            alice
                    )))
                    .toList();

            SearchTasksQuery defaultQuery = SearchTasksQuery.of(TaskSearchCriteria.empty().withType(TaskType.DO_OFFERTE));
            assertThat(defaultQuery.pageNumber()).isEqualTo(0);
            assertThat(defaultQuery.pageSize()).isEqualTo(20);

            assertThat(taskQuery.search(defaultQuery))
                    .containsExactlyElementsOf(created.subList(0, 20));
            assertThat(taskQuery.search(new SearchTasksQuery(TaskSearchCriteria.empty().withType(TaskType.DO_OFFERTE), 1, 20)))
                    .containsExactlyElementsOf(created.subList(20, 23));
        }

        @Test
        @DisplayName("3.4 rejects negative pageNumber, non-positive pageSize, or null executor in queries")
        void rejectsInvalidPagination() {
            var alice = new TaskExecutor.User(new UserId("alice"));

            assertThatThrownBy(() -> taskQuery.findByExecutor(new FindTasksByExecutorQuery(alice, -1, 20)))
                    .isInstanceOf(InvalidPageRequestException.class);
            assertThatThrownBy(() -> taskQuery.findByExecutor(new FindTasksByExecutorQuery(alice, 0, 0)))
                    .isInstanceOf(InvalidPageRequestException.class);
            assertThatThrownBy(() -> taskQuery.findByExecutor(new FindTasksByExecutorQuery(alice, 0, -5)))
                    .isInstanceOf(InvalidPageRequestException.class);
            assertThatThrownBy(() -> taskQuery.findByExecutor(new FindTasksByExecutorQuery(null, 0, 20)))
                    .isInstanceOf(InvalidTaskExecutorException.class);

            assertThatThrownBy(() -> taskQuery.search(new SearchTasksQuery(TaskSearchCriteria.empty(), -1, 20)))
                    .isInstanceOf(InvalidPageRequestException.class);
            assertThatThrownBy(() -> taskQuery.search(new SearchTasksQuery(TaskSearchCriteria.empty(), 0, 0)))
                    .isInstanceOf(InvalidPageRequestException.class);
            assertThatThrownBy(() -> taskQuery.search(new SearchTasksQuery(TaskSearchCriteria.empty(), 0, -10)))
                    .isInstanceOf(InvalidPageRequestException.class);
        }
    }

    @Nested
    @DisplayName("Task Executability — Requirements 1 & 2")
    class TaskExecutabilitySpec {

        @Test
        @DisplayName("1.1 & 1.4 Task implements Executable and default executability returns the task through TaskQuery")
        void taskImplementsExecutableWithDefaultReturningTrue() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            Task planned = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice
            ));

            List<Task> byExecutor = taskQuery.findByExecutor(FindTasksByExecutorQuery.of(alice));
            List<Task> bySearch = taskQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()));

            assertThat(byExecutor).containsExactly(planned);
            assertThat(bySearch).containsExactly(planned);
            assertThat(byExecutor.getFirst()).isInstanceOf(Executable.class);
        }

        @Test
        @DisplayName("1.2 & 1.3 TaskQuery returns a task only when all predicates in its ExecutableContext evaluate to true")
        void evaluatesPredicatesListOnExecutableContext() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            var linked = new LinkedEntities(List.of(
                    new LinkedEntity(EntityTypeName.ENTITY_TYPE_1, new EntityId("ent-1"))
            ));
            Task planned = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    linked
            ));

            var repo1 = new InMemoryEntityType1Repository();
            var repo2 = new InMemoryEntityType2Repository();
            var repo3 = new InMemoryEntityType3Repository();
            var repo4 = new InMemoryEntityType4Repository();

            TaskQuery allMatchQuery = new TaskQueryService(
                    new MongoTaskRepository(database),
                    ExecutableContextResolver.of(
                            Map.of(
                                    TaskType.DO_AANVRAAG,
                                    List.of((ignoredTask, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            t -> t.status() == TaskStatus.CREATED,
                                            t -> t.linkedEntities().entities().stream()
                                                    .anyMatch(e -> e.typeName() == EntityTypeName.ENTITY_TYPE_1)
                                    )))
                            ),
                            repo1,
                            repo2,
                            repo3,
                            repo4
                    )
            );
            TaskQuery oneFailsQuery = new TaskQueryService(
                    new MongoTaskRepository(database),
                    ExecutableContextResolver.of(
                            Map.of(
                                    TaskType.DO_AANVRAAG,
                                    List.of((ignoredTask, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            t -> t.status() == TaskStatus.CREATED,
                                            t -> t.owner() == ProcessName.PO_UG
                                    )))
                            ),
                            repo1,
                            repo2,
                            repo3,
                            repo4
                    )
            );

            assertThat(allMatchQuery.findByExecutor(FindTasksByExecutorQuery.of(alice))).containsExactly(planned);
            assertThat(allMatchQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()))).containsExactly(planned);
            assertThat(oneFailsQuery.findByExecutor(FindTasksByExecutorQuery.of(alice))).isEmpty();
            assertThat(oneFailsQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()))).isEmpty();
        }

        @Test
        @DisplayName("1.2 & 1.5 TaskQuery resolves list of ExecutableContext providers per TaskType and defaults empty/absent lists to executable")
        void filtersNonExecutableTasksWhenQuerying() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            Task executableAanvraag = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    new LinkedEntities(List.of(new LinkedEntity(EntityTypeName.ENTITY_TYPE_1, new EntityId("ent-1"))))
            ));
            planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    LinkedEntities.empty()
            ));
            planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PO_UG,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    new LinkedEntities(List.of(new LinkedEntity(EntityTypeName.ENTITY_TYPE_1, new EntityId("ent-2"))))
            ));
            Task emptyProvidersOpdracht = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_OPDRACHT,
                    ProcessName.PS_UE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    LinkedEntities.empty()
            ));
            Task defaultExecutableContract = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_CONTRACT,
                    ProcessName.PO_VG,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    LinkedEntities.empty()
            ));

            ExecutableContextResolver resolver = ExecutableContextResolver.of(
                    Map.of(
                            TaskType.DO_AANVRAAG,
                            List.of(
                                    (ignoredTask, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            t -> !t.linkedEntities().entities().isEmpty()
                                    )),
                                    (ignoredTask, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            t -> t.owner() == ProcessName.PS_IE
                                    ))
                            ),
                            TaskType.DO_OPDRACHT,
                            List.of()
                    ),
                    new InMemoryEntityType1Repository(),
                    new InMemoryEntityType2Repository(),
                    new InMemoryEntityType3Repository(),
                    new InMemoryEntityType4Repository()
            );
            TaskQuery filteredQuery = new TaskQueryService(new MongoTaskRepository(database), resolver);

            assertThat(filteredQuery.findByExecutor(FindTasksByExecutorQuery.of(alice)))
                    .containsExactly(executableAanvraag, emptyProvidersOpdracht, defaultExecutableContract);
            assertThat(filteredQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty())))
                    .containsExactly(executableAanvraag, emptyProvidersOpdracht, defaultExecutableContract);
        }

        @Test
        @DisplayName("2.1 & 2.2 TaskQuery evaluates predicates against both Task attributes and loaded typed DomainObjects")
        void executableContextHoldsLoadedDomainObjectsForPredicateEvaluation() {
            var repo1 = new InMemoryEntityType1Repository();
            var repo2 = new InMemoryEntityType2Repository();
            var repo3 = new InMemoryEntityType3Repository();
            var repo4 = new InMemoryEntityType4Repository();
            var entity1 = new EntityType1(new EntityId("e1"));
            var entity2 = new EntityType2(new EntityId("e2"));
            repo1.save(entity1);
            repo2.save(entity2);

            var alice = new TaskExecutor.User(new UserId("alice"));
            Task planned = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    new LinkedEntities(List.of(entity1.reference(), entity2.reference()))
            ));

            TaskQuery matchingQuery = new TaskQueryService(
                    new MongoTaskRepository(database),
                    ExecutableContextResolver.of(
                            Map.of(
                                    TaskType.DO_AANVRAAG,
                                    List.of((ignoredTask, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            t -> t.status() == TaskStatus.CREATED,
                                            t -> loaded.findByType(EntityTypeName.ENTITY_TYPE_1, EntityType1.class).contains(entity1),
                                            t -> loaded.find(EntityTypeName.ENTITY_TYPE_2, new EntityId("e2"))
                                                    .map(entity2::equals)
                                                    .orElse(false)
                                    )))
                            ),
                            repo1,
                            repo2,
                            repo3,
                            repo4
                    )
            );
            TaskQuery nonMatchingQuery = new TaskQueryService(
                    new MongoTaskRepository(database),
                    ExecutableContextResolver.of(
                            Map.of(
                                    TaskType.DO_AANVRAAG,
                                    List.of((ignoredTask, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            t -> t.status() == TaskStatus.CREATED,
                                            t -> loaded.find(EntityTypeName.ENTITY_TYPE_2, new EntityId("other"))
                                                    .isPresent()
                                    )))
                            ),
                            repo1,
                            repo2,
                            repo3,
                            repo4
                    )
            );

            assertThat(matchingQuery.findByExecutor(FindTasksByExecutorQuery.of(alice))).containsExactly(planned);
            assertThat(matchingQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()))).containsExactly(planned);
            assertThat(nonMatchingQuery.findByExecutor(FindTasksByExecutorQuery.of(alice))).isEmpty();
            assertThat(nonMatchingQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()))).isEmpty();
        }

        @Test
        @DisplayName("2.3, 2.4 & 2.5 TaskQuery loads linked EntityType1..4 into ExecutableContext, skips loading when unconfigured, and handles missing entities")
        void loadsLinkedDomainObjectsIntoExecutableContextAndHandlesMissingEntities() {
            var repo1 = new InMemoryEntityType1Repository();
            var repo2 = new InMemoryEntityType2Repository();
            var repo3 = new InMemoryEntityType3Repository();
            var repo4 = new InMemoryEntityType4Repository();

            var loadedEntity1 = new EntityType1(new EntityId("ent-1"));
            var loadedEntity2 = new EntityType2(new EntityId("ent-2"));
            var loadedEntity3 = new EntityType3(new EntityId("ent-3"));
            var loadedEntity4 = new EntityType4(new EntityId("ent-4"));
            repo1.save(loadedEntity1);
            repo2.save(loadedEntity2);
            repo3.save(loadedEntity3);
            repo4.save(loadedEntity4);

            var alice = new TaskExecutor.User(new UserId("alice"));
            Task executableTask = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    new LinkedEntities(List.of(
                            loadedEntity1.reference(),
                            loadedEntity2.reference(),
                            loadedEntity3.reference(),
                            loadedEntity4.reference()
                    ))
            ));
            planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    new LinkedEntities(List.of(
                            loadedEntity1.reference(),
                            new LinkedEntity(EntityTypeName.ENTITY_TYPE_2, new EntityId("missing-ent-2"))
                    ))
            ));
            Task unconfiguredTypeWithMissingEntity = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_CONTRACT,
                    ProcessName.PO_VG,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice,
                    new LinkedEntities(List.of(
                            new LinkedEntity(EntityTypeName.ENTITY_TYPE_4, new EntityId("missing-ent-4"))
                    ))
            ));

            ExecutableContextResolver resolver = ExecutableContextResolver.of(
                    Map.of(
                            TaskType.DO_AANVRAAG,
                            List.of(
                                    (t, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            ignored -> loaded.findByType(EntityTypeName.ENTITY_TYPE_1, EntityType1.class)
                                                    .contains(loadedEntity1),
                                            ignored -> loaded.findByType(EntityTypeName.ENTITY_TYPE_2, EntityType2.class)
                                                    .contains(loadedEntity2)
                                    )),
                                    (t, loaded) -> new DefaultExecutableContext(loaded, List.of(
                                            ignored -> loaded.findByType(EntityTypeName.ENTITY_TYPE_3, EntityType3.class)
                                                    .contains(loadedEntity3),
                                            ignored -> loaded.findByType(EntityTypeName.ENTITY_TYPE_4, EntityType4.class)
                                                    .contains(loadedEntity4)
                                    ))
                            )
                    ),
                    repo1,
                    repo2,
                    repo3,
                    repo4
            );
            TaskQuery filteredQuery = new TaskQueryService(new MongoTaskRepository(database), resolver);

            assertThat(filteredQuery.findByExecutor(FindTasksByExecutorQuery.of(alice)))
                    .containsExactly(executableTask, unconfiguredTypeWithMissingEntity);
            assertThat(filteredQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty())))
                    .containsExactly(executableTask, unconfiguredTypeWithMissingEntity);
        }
    }
}
