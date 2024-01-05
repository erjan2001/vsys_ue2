package dslab.rules;

import dslab.ComponentFactory;
import dslab.ComponentRule;
import dslab.monitoring.IMonitoringServer;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * MonitoringServerRule.
 */
public class MonitoringServerRule extends ComponentRule<IMonitoringServer> {

    public MonitoringServerRule(String componentId) {
        super(componentId);
    }

    @Override
    protected IMonitoringServer createComponent(String componentId, InputStream in, PrintStream out) throws Exception {
        return ComponentFactory.createMonitoringServer(componentId, in, out);
    }

    @Override
    protected void waitForStartup() {
        sleep(1000);
    }
}
