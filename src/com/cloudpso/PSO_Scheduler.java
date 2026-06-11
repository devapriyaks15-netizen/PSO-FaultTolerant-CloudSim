package com.cloudpso;

import java.util.List;
import java.util.Random;

public class PSO_Scheduler {

    private static final int SWARM_SIZE = 30;
    private static final int MAX_ITERATIONS = 100;
    private static final double C1 = 1.5;
    private static final double C2 = 1.5;
    private static final double W_MAX = 0.9;
    private static final double W_MIN = 0.4;

    private int numTasks;
    private int numVMs;
    private List<Task> tasks;
    private List<VmWrapper> vms;

    private int[][] position;
    private int[][] velocity;
    private int[][] pBest;
    private int[] gBest;
    private double[] pBestFitness;
    private double gBestFitness;

    private Random rand = new Random();

    public PSO_Scheduler(List<Task> tasks, List<VmWrapper> vms) {
        this.tasks = tasks;
        this.vms = vms;
        this.numTasks = tasks.size();
        this.numVMs = vms.size();

        position = new int[SWARM_SIZE][numTasks];
        velocity = new int[SWARM_SIZE][numTasks];
        pBest = new int[SWARM_SIZE][numTasks];
        gBest = new int[numTasks];
        pBestFitness = new double[SWARM_SIZE];
        gBestFitness = Double.MAX_VALUE;
    }

    private void initialize() {
        for (int i = 0; i < SWARM_SIZE; i++) {
            for (int j = 0; j < numTasks; j++) {
                position[i][j] = rand.nextInt(numVMs);
                velocity[i][j] = 0;
                pBest[i][j] = position[i][j];
            }
            pBestFitness[i] = calculateFitness(position[i]);
            if (pBestFitness[i] < gBestFitness) {
                gBestFitness = pBestFitness[i];
                gBest = position[i].clone();
            }
        }
    }

    private double calculateFitness(int[] particlePosition) {
        double[] vmLoad = new double[numVMs];
        for (int j = 0; j < numTasks; j++) {
            int vmIndex = particlePosition[j];
            if (vms.get(vmIndex).isFailed) {
                return Double.MAX_VALUE;
            }
            vmLoad[vmIndex] += tasks.get(j).length 
                             / vms.get(vmIndex).mips;
        }
        double makespan = 0;
        for (double load : vmLoad) {
            if (load > makespan) makespan = load;
        }
        return makespan;
    }

    private double getInertiaWeight() {
        return W_MAX - 0.5 * rand.nextDouble();
    }

    public int[] runPSO() {
        initialize();

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double w = getInertiaWeight();

            for (int i = 0; i < SWARM_SIZE; i++) {
                for (int j = 0; j < numTasks; j++) {
                    double r1 = rand.nextDouble();
                    double r2 = rand.nextDouble();

                    velocity[i][j] = (int)(
                        w * velocity[i][j]
                        + r1 * C1 * (pBest[i][j] - position[i][j])
                        + r2 * C2 * (gBest[j] - position[i][j])
                    );

                    position[i][j] = position[i][j] + velocity[i][j];
                    position[i][j] = Math.abs(position[i][j]) % numVMs;
                }

                double currentFitness = calculateFitness(position[i]);
                if (currentFitness < pBestFitness[i]) {
                    pBestFitness[i] = currentFitness;
                    pBest[i] = position[i].clone();
                    if (currentFitness < gBestFitness) {
                        gBestFitness = currentFitness;
                        gBest = position[i].clone();
                    }
                }
            }
        }

        System.out.println("PSO Best Makespan: " 
                         + gBestFitness + " seconds");
        return gBest;
    }
}