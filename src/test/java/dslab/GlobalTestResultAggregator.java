package dslab;

import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.TreeMap;


public class GlobalTestResultAggregator extends Suite {


    private static final Map<String, TestResult> globalResults = new TreeMap<>();

    public static synchronized void addResult(String testName, double maxPoints, double earnedPoints) {
        globalResults.put(testName, new TestResult(maxPoints, earnedPoints));
    }

    public GlobalTestResultAggregator(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
        notifier.addListener(new RunListener(){
            @Override
            public void testRunFinished(Result result) throws Exception {
                GlobalTestResultAggregator.printGlobalResults();
                GlobalTestResultAggregator.saveReportToFile();
            }
        });
    }

    public static void saveReportToFile() {
        String report = generateReport();
        Path outputPath = Paths.get("test_report.txt");
        try {
            Files.write(outputPath, report.getBytes());
            System.out.println("Report saved to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing report to file: " + e.getMessage());
        }
    }

    private static String generateReport() {
        StringBuilder report = new StringBuilder();
        String format = "| %-80s | %-15s | %-15s |\n";

        report.append(String.format(format, "Test Name", "Max Points", "Achieved Points"));
        report.append("|------------------------------------------------------------------------------------"
                + "-------------------------------|\n");

        for (Map.Entry<String, TestResult> entry : globalResults.entrySet()) {
            String testName = entry.getKey();
            TestResult result = entry.getValue();
            report.append(String.format(format, testName, result.maxPoints, result.earnedPoints));
        }

        double totalPoints = globalResults.values().stream().mapToDouble(res -> res.maxPoints).sum();
        double earnedPoints = globalResults.values().stream().mapToDouble(res -> res.earnedPoints).sum();
        report.append("\n");
        report.append("Summary:\n");
        report.append(String.format("Total points possible: %.2f%n", totalPoints));
        report.append(String.format("Total points earned: %.2f%n", earnedPoints));

        return report.toString();
    }
    public static void printGlobalResults() {
        System.out.println("Global Test Results:");
        System.out.printf("%-80s %-15s %-15s%n", "Test Name", "Max Points", "Earned Points");
        System.out.printf("%-80s %-15s %-15s%n", "---------", "----------", "------------");

        globalResults.forEach((name, res) ->
                System.out.printf("%-80s %-15.2f %-15.2f%n", name, res.maxPoints, res.earnedPoints)
        );

        double totalPoints = globalResults.values().stream().mapToDouble(res -> res.maxPoints).sum();
        double earnedPoints = globalResults.values().stream().mapToDouble(res -> res.earnedPoints).sum();

        System.out.printf("Total points possible: %.2f%n", totalPoints);
        System.out.printf("Total points earned: %.2f%n", earnedPoints);
    }

    private static class TestResult {
        double maxPoints;
        double earnedPoints;

        TestResult(double maxPoints, double earnedPoints) {
            this.maxPoints = maxPoints;
            this.earnedPoints = earnedPoints;
        }


    }
}