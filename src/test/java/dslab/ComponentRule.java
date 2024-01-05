package dslab;

import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * ComponentRule.
 */
public abstract class ComponentRule<T extends Runnable> extends ExternalResource {

    public static final long SOCKET_WAIT_TIME = 2000;

    private static final Log LOG = LogFactory.getLog(ComponentRule.class);

    protected Config config;
    protected String componentId;

    protected TestInputStream in;
    protected TestOutputStream out;

    protected Thread componentThread;

    public ComponentRule(String componentId) {
        this.componentId = componentId;
        this.config = new Config(componentId);
        this.in = new TestInputStream();
        this.out = new TestOutputStream();
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public TestInputStream getIn() {
        return in;
    }

    public TestOutputStream getOut() {
        return out;
    }

    public Config getConfig() {
        return config;
    }

    public String getComponentId() {
        return componentId;
    }

    @Override
    protected void before() throws Throwable {
        LOG.info("Starting up component " + componentId);
        try {
            T component;
            try {
                component = createComponent(componentId, in, out);
            } catch (Exception e) {
                throw new IllegalStateException("Exception during setup of component " + componentId, e);
            }

            componentThread = new Thread(component);
            LOG.info("Starting thread with component " + component);
            componentThread.start();

            // Thread.sleep(Constants.COMPONENT_STARTUP_WAIT);

            waitForStartup();
        } catch (Exception e) {
            try {
                after();
            } catch (Exception e1) {
                LOG.error("Error while shutting down component after setup exception");
            }
            throw e;
        }
    }

    @Override
    protected void after() {
        boolean wasActive = componentThread.isAlive();

        in.addLine("shutdown");
        try {
            componentThread.join(SOCKET_WAIT_TIME);
            if (!wasActive) {
                Thread.sleep(SOCKET_WAIT_TIME);
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting on component thread " + componentId, e);
        }

        sleep(500);
    }

    protected abstract T createComponent(String componentId, InputStream in, PrintStream out) throws Exception;

    protected void waitForStartup() {
        // hook to wait for, e.g., sockets to appear
    }
}
