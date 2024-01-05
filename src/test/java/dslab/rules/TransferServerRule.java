package dslab.rules;

import dslab.ComponentFactory;
import dslab.ComponentRule;
import dslab.Sockets;
import dslab.transfer.ITransferServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketTimeoutException;

/**
 * TransferServerRule.
 */
public class TransferServerRule extends ComponentRule<ITransferServer> {

    private static final Log LOG = LogFactory.getLog(TransferServerRule.class);

    public TransferServerRule(String componentId) {
        super(componentId);
    }

    @Override
    protected ITransferServer createComponent(String componentId, InputStream in, PrintStream out) throws Exception {
        return ComponentFactory.createTransferServer(componentId, in, out);
    }

    @Override
    protected void waitForStartup() {
        int serverPort = config.getInt("tcp.port");

        try {
            LOG.info("Waiting for DMTP server socket to appear for " + componentId);
            Sockets.waitForSocket("localhost", serverPort, SOCKET_WAIT_TIME);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Gave up waiting for DMTP server port", e);
        }

        sleep(500);
    }

    public int getDmtpPort() {
        return config.getInt("tcp.port");
    }
}
