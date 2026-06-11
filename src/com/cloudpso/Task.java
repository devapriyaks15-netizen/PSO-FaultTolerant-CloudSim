package com.cloudpso;

public class Task {
    public int taskId;
    public long length;
    public int replicaCount;
    public boolean completed;
    public boolean failed;

    public Task(int taskId, long length) {
        this.taskId = taskId;
        this.length = length;
        this.replicaCount = 1;
        this.completed = false;
        this.failed = false;
    }
}