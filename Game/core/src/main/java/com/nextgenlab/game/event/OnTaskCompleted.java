package com.nextgenlab.game.event;

public class OnTaskCompleted {
    public final int    totalCompleted;
    public final String taskName;
    public final int    totalTasks;
    public OnTaskCompleted(int totalCompleted, String taskName, int totalTasks) {
        this.totalCompleted = totalCompleted;
        this.taskName       = taskName;
        this.totalTasks     = totalTasks;
    }
}
