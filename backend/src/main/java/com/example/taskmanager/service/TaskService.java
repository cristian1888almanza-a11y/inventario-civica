package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TaskService {
    private final TaskRepository repository;
    public TaskService(TaskRepository repository) { this.repository = repository; }

    public List<TaskResponse> findAll(Boolean completed) {
        List<Task> tasks = completed == null ? repository.findAll() : repository.findAllByCompletedOrderByDueDateAsc(completed);
        return tasks.stream().map(TaskResponse::from).toList();
    }
    public TaskResponse findById(Long id) { return TaskResponse.from(get(id)); }
    public TaskResponse create(TaskRequest request) { return TaskResponse.from(repository.save(toEntity(request))); }
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = get(id); task.setTitle(request.title()); task.setDescription(request.description()); task.setDueDate(request.dueDate()); task.setCompleted(request.completed());
        return TaskResponse.from(repository.save(task));
    }
    public void delete(Long id) { repository.delete(get(id)); }
    private Task get(Long id) { return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id)); }
    private Task toEntity(TaskRequest r) { Task t = new Task(); t.setTitle(r.title()); t.setDescription(r.description()); t.setDueDate(r.dueDate()); t.setCompleted(r.completed()); return t; }
}
