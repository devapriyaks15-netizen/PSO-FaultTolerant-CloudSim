package com.cloudpso;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ResultsChart {

    public static void showAllCharts(
            List<Double> execTimes,
            List<Integer> vmIds,
            int totalTasks,
            int recoveredTasks,
            int failedTasks,
            double psoMakespan,
            double rrMakespan,
            double seqMakespan) {

        JFrame frame = new JFrame(
            "PSO + Fault Tolerant Scheduling - Results (V2)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 2));
        frame.setSize(1400, 900);

        frame.add(new ChartPanel(
            createExecutionTimeChart(execTimes, vmIds)));
        frame.add(new ChartPanel(
            createFaultToleranceChart(
                recoveredTasks, failedTasks, totalTasks)));
        frame.add(new ChartPanel(
            createMakespanChart(
                psoMakespan, rrMakespan, seqMakespan)));
        frame.add(new ChartPanel(
            createRecoveryRateChart(
                recoveredTasks, failedTasks)));

        frame.setVisible(true);
    }

    private static JFreeChart createExecutionTimeChart(
            List<Double> execTimes,
            List<Integer> vmIds) {

        DefaultCategoryDataset dataset =
            new DefaultCategoryDataset();

        for (int i = 0; i < execTimes.size(); i++) {
            dataset.addValue(
                execTimes.get(i),
                "Execution Time",
                "T" + i + "(VM" + vmIds.get(i) + ")"
            );
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Execution Time per Task (50 Tasks, 10 VMs)",
            "Task ID (VM Assigned)",
            "Execution Time (sec)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.getDomainAxis().setCategoryLabelPositions(
            org.jfree.chart.axis.CategoryLabelPositions
                .UP_45
        );
        BarRenderer renderer = 
            (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(79, 129, 189));

        return chart;
    }

    private static JFreeChart createFaultToleranceChart(
            int recovered,
            int failed,
            int total) {

        DefaultCategoryDataset dataset =
            new DefaultCategoryDataset();

        int successful = total - recovered - failed;
        dataset.addValue(successful, 
            "Tasks", "Completed Normally");
        dataset.addValue(recovered,  
            "Tasks", "Recovered by FT");
        dataset.addValue(failed,     
            "Tasks", "Permanently Failed");

        JFreeChart chart = ChartFactory.createBarChart(
            "Fault Tolerance Results (50 Tasks, 10 VMs)",
            "Task Status",
            "Number of Tasks",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        BarRenderer renderer = 
            (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(0, 176, 80));

        return chart;
    }

    private static JFreeChart createMakespanChart(
            double psoMakespan,
            double rrMakespan,
            double seqMakespan) {

        DefaultCategoryDataset dataset =
            new DefaultCategoryDataset();

        dataset.addValue(psoMakespan, 
            "Time (sec)", "PSO Scheduled");
        dataset.addValue(seqMakespan, 
            "Time (sec)", "Sequential");
        dataset.addValue(rrMakespan,  
            "Time (sec)", "Round Robin");

        JFreeChart chart = ChartFactory.createBarChart(
            "Makespan Comparison (Real Values)",
            "Scheduling Method",
            "Total Time (sec)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        BarRenderer renderer = 
            (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(255, 102, 0));

        return chart;
    }

    private static JFreeChart createRecoveryRateChart(
            int recovered,
            int failed) {

        DefaultCategoryDataset dataset =
            new DefaultCategoryDataset();

        int total = recovered + failed;
        double recoveryRate = total > 0
            ? (recovered / (double) total) * 100 : 100;
        double failureRate = 100 - recoveryRate;

        dataset.addValue(recoveryRate, 
            "Rate (%)", "Recovery Rate");
        dataset.addValue(failureRate,  
            "Rate (%)", "Failure Rate");

        JFreeChart chart = ChartFactory.createBarChart(
            "Recovery Rate (%)",
            "Metric",
            "Percentage (%)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        BarRenderer renderer = 
            (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(112, 48, 160));

        return chart;
    }
}