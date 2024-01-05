package dslab.rules;

import dslab.ComponentFactory;
import dslab.ComponentRule;
import dslab.Sockets;
import dslab.nameserver.INameserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketTimeoutException;

/**
 * NameserverRule.
 */
public class NameserverRule extends ComponentRule<INameserver> {

    private static final Log LOG = LogFactory.getLog(NameserverRule.class);

    public NameserverRule(String componentId) {
        super(componentId);
    }

    @Override
    protected INameserver createComponent(String componentId, InputStream in, PrintStream out) throws Exception {
        LOG.info("Creating nameserver component " + componentId);
        return ComponentFactory.createNameserver(componentId, in, out);
    }

    @Override
    protected void waitForStartup() {
        int registryPort = getConfig().getInt("registry.port");

        try {
            LOG.info("Waiting for registry to accept TCP connections on " + registryPort);
            Sockets.waitForSocket("localhost", registryPort, SOCKET_WAIT_TIME);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Gave up waiting for DMAP server port", e);
        }

        sleep(1500);
    }

    @Override
    protected void after() {
        LOG.info("Shutting down nameserver " + componentId);
        super.after();
    }
}
