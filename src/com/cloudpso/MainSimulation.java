package com.cloudpso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class MainSimulation {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;

    public static void main(String[] args) {

        System.out.println(
            "=== PSO + Fault Tolerant Scheduling " +
            "Simulation V2 ===");

        try {
            // STEP 1: Initialize CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUsers, calendar, traceFlag);

            // STEP 2: Create Datacenter
            Datacenter datacenter =
                createDatacenter("Datacenter_0");

            // STEP 3: Create Broker
            DatacenterBroker broker =
                new DatacenterBroker("Broker_0");
            int brokerId = broker.getId();

            // STEP 4: Create VMs with varied MIPS
            // All VMs fast enough, enough variation for PSO
            vmList = new ArrayList<Vm>();
            List<VmWrapper> vmWrappers =
                new ArrayList<VmWrapper>();
            int numVMs = 10;

            int[] mipsValues = {1000, 1100, 1200, 1300, 1500,
                                900,  1050, 1150, 1400, 1250};

            for (int i = 0; i < numVMs; i++) {
                Vm vm = new Vm(
                    i, brokerId, mipsValues[i], 1,
                    512, 1000, 10000,
                    "Xen",
                    new CloudletSchedulerTimeShared()
                );
                vmList.add(vm);
                vmWrappers.add(
                    new VmWrapper(i, mipsValues[i]));
            }
            broker.submitGuestList(vmList);

            // STEP 5: Create Tasks with varied lengths
            // Varied lengths give PSO more to optimize
            cloudletList = new ArrayList<Cloudlet>();
            List<Task> tasks = new ArrayList<Task>();
            int numTasks = 50;
            UtilizationModel utilizationModel =
                new UtilizationModelFull();

            int[] taskLengths = {
                2000, 5000, 3000, 8000, 1500,
                6000, 4000, 7000, 2500, 9000,
                3500, 5500, 1000, 8500, 4500,
                6500, 2000, 7500, 3000, 9500,
                1500, 5000, 4000, 8000, 2500,
                6000, 3500, 7000, 1000, 9000,
                4500, 5500, 2000, 8500, 3000,
                6500, 1500, 7500, 4000, 9500,
                2500, 5000, 3500, 8000, 1000,
                6000, 4500, 7000, 2000, 9000
            };

            for (int i = 0; i < numTasks; i++) {
                Cloudlet cloudlet = new Cloudlet(
                    i, taskLengths[i], 1, 300, 300,
                    utilizationModel,
                    utilizationModel,
                    utilizationModel
                );
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
                tasks.add(new Task(i, taskLengths[i]));
            }

            // STEP 6: Run PSO Scheduler
            System.out.println(
                "\n--- Running PSO Scheduler ---");
            PSO_Scheduler pso =
                new PSO_Scheduler(tasks, vmWrappers);
            int[] mapping = pso.runPSO();

            System.out.println(
                "\n--- PSO Task-to-VM Mapping ---");
            for (int i = 0; i < numTasks; i++) {
                System.out.println(
                    "Task " + i + " --> VM " + mapping[i]
                    + " (MIPS: " + mipsValues[mapping[i]]
                    + ")");
            }

            // STEP 7: Run Round Robin and Sequential
            System.out.println(
                "\n--- Running Round Robin Scheduler ---");
            RoundRobin_Scheduler rr =
                new RoundRobin_Scheduler(tasks, vmWrappers);
            int[] rrMapping = rr.runRoundRobin();
            double rrMakespan =
                rr.calculateMakespan(rrMapping);
            double seqMakespan =
                rr.calculateSequentialMakespan();

            System.out.println(
                "Round Robin Makespan : "
                + rrMakespan + " seconds");
            System.out.println(
                "Sequential Makespan  : "
                + seqMakespan + " seconds");

            // STEP 8: Inject Failures and Handle
            FaultTolerance ft = new FaultTolerance();
            ft.injectFailures(vmWrappers);
            ft.handleFaults(mapping, tasks, vmWrappers);

            System.out.println(
                "\n--- Final Mapping After Fault Recovery ---");
            for (int i = 0; i < numTasks; i++) {
                System.out.println(
                    "Task " + i + " --> VM " + mapping[i]
                    + (tasks.get(i).failed
                        ? "  [FAILED]" : "  [OK]"));
            }

            // STEP 9: Bind Cloudlets to VMs
            for (int i = 0; i < numTasks; i++) {
                if (!tasks.get(i).failed) {
                    cloudletList.get(i).setGuestId(
                        mapping[i]);
                }
            }
            broker.submitCloudletList(cloudletList);

            // STEP 10: Start Simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // STEP 11: Print Results
            List<Cloudlet> resultList =
                broker.getCloudletReceivedList();

            System.out.println(
                "\n=== Simulation Results ===");
            System.out.printf(
                "%-10s %-8s %-12s %-12s %-12s%n",
                "TaskID", "VM_ID",
                "StartTime", "FinishTime", "ExecTime");

            double totalMakespan = 0;
            int successCount = 0;

            List<Double> execTimes =
                new ArrayList<Double>();
            List<Integer> vmIds =
                new ArrayList<Integer>();

            for (Cloudlet c : resultList) {
                System.out.printf(
                    "%-10d %-8d %-12.2f %-12.2f %-12.2f%n",
                    c.getCloudletId(),
                    c.getGuestId(),
                    c.getExecStartTime(),
                    c.getFinishTime(),
                    c.getActualCPUTime());

                if (c.getFinishTime() > totalMakespan)
                    totalMakespan = c.getFinishTime();

                successCount++;
                execTimes.add(c.getActualCPUTime());
                vmIds.add(c.getGuestId());
            }

            int recoveredCount = 0;
            int failedCount = 0;
            for (Task t : tasks) {
                if (t.failed) failedCount++;
                else if (t.completed) recoveredCount++;
            }

            System.out.println(
                "\n=== Performance Metrics ===");
            System.out.println(
                "Total Makespan (PSO) : "
                + totalMakespan + " sec");
            System.out.println(
                "Total Makespan (RR)  : "
                + rrMakespan + " sec");
            System.out.println(
                "Total Makespan (Seq) : "
                + seqMakespan + " sec");
            System.out.println(
                "Tasks Completed : " + successCount
                + "/" + numTasks);
            System.out.println(
                "Recovery Rate   : "
                + (int)((successCount / (double) numTasks)
                * 100) + "%");

            // STEP 12: Show Graphs
            System.out.println(
                "\n--- Generating Charts ---");
            ResultsChart.showAllCharts(
                execTimes, vmIds, numTasks,
                recoveredCount, failedCount,
                totalMakespan, rrMakespan, seqMakespan
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(
            String name) throws Exception {

        List<Host> hostList = new ArrayList<Host>();

        for (int i = 0; i < 5; i++) {
            List<Pe> peList = new ArrayList<Pe>();
            peList.add(new Pe(0,
                new PeProvisionerSimple(10000)));
            hostList.add(new Host(
                i,
                new RamProvisionerSimple(16384),
                new BwProvisionerSimple(100000),
                1000000, peList,
                new VmSchedulerTimeShared(peList)
            ));
        }

        DatacenterCharacteristics characteristics =
            new DatacenterCharacteristics(
                "x86", "Linux", "Xen",
                hostList, 10.0,
                3.0, 0.05, 0.001, 0.1
            );

        Datacenter datacenter = new Datacenter(
            name, characteristics,
            new VmAllocationPolicySimple(hostList),
            new LinkedList<Storage>(), 0
        );

        return datacenter;
    }
}