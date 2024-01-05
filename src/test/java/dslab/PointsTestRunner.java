package dslab;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PointsTestRunner extends BlockJUnit4ClassRunner {

    private final Map<String, TestResult> results;
    private final Set<String> failedTests;
    private final String testClassName;


    public PointsTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        this.failedTests = new HashSet<>();
        this.results = new HashMap<>();
        this.testClassName = klass.getSimpleName();
    }

    @Override
    public void run(final RunNotifier notifier) {

        notifier.addFirstListener(new RunListener() {

            @Override
            public void testStarted(Description description) {
                if (description.getTestClass().getSimpleName().equals(testClassName)) {
                    TestPoints testPoints = description.getAnnotation(TestPoints.class);
                    if (testPoints != null) {
                        results.put(description.getMethodName(), new TestResult(testPoints.value(), 0));
                    }
                }

            }

            @Override
            public void testFinished(Description description) {
                if (description.getTestClass().getSimpleName().equals(testClassName)) {
                    TestPoints testPoints = description.getAnnotation(TestPoints.class);
                    if (testPoints != null) {
                        String key = description.getMethodName();
                        if (!failedTests.contains(key)) {
                            TestResult result = results.get(key);
                            if (result != null) {
                                double points = testPoints.value();
                                result.earnedPoints = points;  // Assume success until a failure occurs
                                GlobalTestResultAggregator.addResult(key, points, points); // Assume all points are earned until a failure occurs
                            }
                        }
                    }
                }
            }


            @Override
            public void testFailure(Failure failure) {
                if (failure.getDescription().getTestClass().getSimpleName().equals(testClassName)) {
                    TestPoints testPoints = failure.getDescription().getAnnotation(TestPoints.class);
                    if (testPoints != null) {
                        String key = failure.getDescription().getMethodName();
                        TestResult result = results.get(key);
                        double maxPoints = testPoints.value();

                        if (result != null) {
                            double points = 0;
                            result.earnedPoints = points;  // Update to 0 on failure
                            GlobalTestResultAggregator.addResult(key, maxPoints, points); // Assume 0 points are earned in case of failure
                            failedTests.add(key);
                        }
                    }
                }
            }

            @Override
            public void testRunFinished(org.junit.runner.Result result) {
                System.out.println("Test Run Finished: " + testClassName);
                System.out.printf("%-80s %-15s %-15s%n", "Test Name", "Max Points", "Earned Points");
                System.out.printf("%-80s %-15s %-15s%n", "-------------------------------------------------------------------", "----------", "------------");
                results.forEach((name, res) -> System.out.printf("%-80s %-15.2f %-15.2f%n", name, res.maxPoints, res.earnedPoints));

                double totalPoints = results.values().stream().mapToDouble(res -> res.maxPoints).sum();
                double earnedPoints = results.values().stream().mapToDouble(res -> res.earnedPoints).sum();

                System.out.println("Total points possible: " + totalPoints);
                System.out.println("Total points earned: " + earnedPoints);
                System.out.println();

            }
        });
        super.run(notifier);
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
