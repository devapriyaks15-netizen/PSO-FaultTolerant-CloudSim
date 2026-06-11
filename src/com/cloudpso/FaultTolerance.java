package com.cloudpso;

import java.util.List;
import java.util.Random;

public class FaultTolerance {

    private static final double FAILURE_PROBABILITY = 0.1;
    private static final int MAX_RESUBMISSIONS = 3;
    private Random rand = new Random();

    public void injectFailures(List<VmWrapper> vms) {
        System.out.println("\n--- Injecting Random Failures ---");
        for (VmWrapper vm : vms) {
            if (rand.nextDouble() < FAILURE_PROBABILITY) {
                vm.isFailed = true;
                System.out.println("VM " + vm.vmId + " has FAILED!");
            }
        }
    }

    public int replicateTask(Task task, int[] mapping, 
    		List<VmWrapper> vms, int currentVm) {
    	System.out.println("Replicating Task " + task.taskId 
        + " (was on VM " + currentVm + ")");
    	
    	// Find least loaded healthy VM instead of first available
    	double[] vmLoad = new double[vms.size()];
    	for (int t = 0; t < mapping.length; t++) {
    		if (!vms.get(mapping[t]).isFailed) {
    			vmLoad[mapping[t]]++;
    		}
    	}
    	
    	int bestVm = -1;
    	double minLoad = Double.MAX_VALUE;
    	for (int i = 0; i < vms.size(); i++) {
    		if (i != currentVm && !vms.get(i).isFailed) {
    			if (vmLoad[i] < minLoad) {
    				minLoad = vmLoad[i];
    				bestVm = i;
    			}
    		}
    	}
    	
    	if (bestVm != -1) {
    		System.out.println("Task " + task.taskId 
    				+ " replicated to VM " + bestVm);
    	}
    	return bestVm;
    }

    public boolean resubmitTask(Task task, int[] mapping, 
                                List<VmWrapper> vms) {
        int attempts = 0;
        while (attempts < MAX_RESUBMISSIONS) {
            attempts++;
            System.out.println("Resubmitting Task " + task.taskId 
                             + " - Attempt " + attempts);
            for (int i = 0; i < vms.size(); i++) {
                if (!vms.get(i).isFailed) {
                    mapping[task.taskId] = i;
                    System.out.println("Task " + task.taskId 
                                     + " resubmitted to VM " + i);
                    task.completed = true;
                    return true;
                }
            }
            System.out.println("No healthy VM found. Retrying...");
        }
        System.out.println("Task " + task.taskId 
                         + " FAILED after max resubmissions!");
        task.failed = true;
        return false;
    }

    public void handleFaults(int[] mapping, List<Task> tasks, 
                             List<VmWrapper> vms) {
        System.out.println("\n--- Running Fault Tolerance Handler ---");
        int recoveredCount = 0;
        int failedCount = 0;

        for (Task task : tasks) {
            int assignedVm = mapping[task.taskId];
            if (vms.get(assignedVm).isFailed) {
                System.out.println("Task " + task.taskId 
                                 + " affected! VM " + assignedVm + " failed.");
                int newVm = replicateTask(task, mapping, vms, assignedVm);
                if (newVm != -1) {
                    mapping[task.taskId] = newVm;
                    task.completed = true;
                    recoveredCount++;
                } else {
                    boolean success = resubmitTask(task, mapping, vms);
                    if (success) recoveredCount++;
                    else failedCount++;
                }
            } else {
                task.completed = true;
            }
        }

        System.out.println("\n--- Fault Tolerance Summary ---");
        System.out.println("Total Tasks: " + tasks.size());
        System.out.println("Recovered: " + recoveredCount);
        System.out.println("Failed: " + failedCount);
    }
}