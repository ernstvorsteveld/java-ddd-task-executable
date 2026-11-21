package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.CommandReceipt;

public interface UpdateTaskUseCase {

    CommandReceipt updateTask(UpdateTaskCommand command);
}
