package com.sternitc.task.domain.model;

public sealed interface TaskExecutor permits TaskExecutor.User, TaskExecutor.Role {

    record User(UserId userId) implements TaskExecutor {
        public User {
            if (userId == null) {
                throw new InvalidTaskExecutorException("userId must not be null");
            }
        }
    }

    record Role(RoleName roleName) implements TaskExecutor {
        public Role {
            if (roleName == null) {
                throw new InvalidTaskExecutorException("roleName must not be null");
            }
        }
    }
}
