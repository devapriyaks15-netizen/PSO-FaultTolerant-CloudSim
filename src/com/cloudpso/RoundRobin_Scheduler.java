package com.cloudpso;

import java.util.List;

public class RoundRobin_Scheduler {

    private List<Task> tasks;
    private List<VmWrapper> vms;
    private int numTasks;
    private int numVMs;

    public RoundRobin_Scheduler(List<Task> tasks, 
                                 List<VmWrapper> vms) {
        this.tasks = tasks;
        this.vms = vms;
        this.numTasks = tasks.size();
        this.numVMs = vms.size();
    }

    public int[] runRoundRobin() {
        int[] mapping = new int[numTasks];
        for (int i = 0; i < numTasks; i++) {
            mapping[i] = i % numVMs;
        }
        return mapping;
    }

    public double calculateMakespan(int[] mapping) {
        double[] vmLoad = new double[numVMs];
        for (int j = 0; j < numTasks; j++) {
            int vmIndex = mapping[j];
            vmLoad[vmIndex] += tasks.get(j).length 
                             / vms.get(vmIndex).mips;
        }
        double makespan = 0;
        for (double load : vmLoad) {
            if (load > makespan) makespan = load;
        }
        return makespan;
    }

    public double calculateSequentialMakespan() {
        double total = 0;
        for (Task t : tasks) {
            total += t.length / vms.get(0).mips;
        }
        return total;
    }
}