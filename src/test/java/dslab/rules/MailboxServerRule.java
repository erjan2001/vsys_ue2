package dslab.rules;

import dslab.ComponentFactory;
import dslab.ComponentRule;
import dslab.Sockets;
import dslab.mailbox.IMailboxServer;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketTimeoutException;

/**
 * MailboxServerRule.
 */
public class MailboxServerRule extends ComponentRule<IMailboxServer> {

    private static final Log LOG = LogFactory.getLog(MailboxServerRule.class);

    public MailboxServerRule(String componentId) {
        super(componentId);
    }

    @Override
    protected IMailboxServer createComponent(String componentId, InputStream in, PrintStream out) throws Exception {
        return ComponentFactory.createMailboxServer(componentId, in, out);
    }

    @Override
    protected void waitForStartup() {
        int dmapServerPort = getDmapPort();
        int dmtpServerPort = getDmtpPort();

        try {
            LOG.info("Waiting for DMAP server socket to appear for " + componentId);
            Sockets.waitForSocket("localhost", dmapServerPort, SOCKET_WAIT_TIME);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Gave up waiting for DMAP server port", e);
        }

        try {
            LOG.info("Waiting for DMTP server socket to appear for " + componentId);
            Sockets.waitForSocket("localhost", dmtpServerPort, SOCKET_WAIT_TIME);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Gave up waiting for DMTP server port", e);
        }

        sleep(500);
    }

    public int getDmapPort() {
        return new Config(componentId).getInt("dmap.tcp.port");
    }

    public int getDmtpPort() {
        return new Config(componentId).getInt("dmtp.tcp.port");
    }
}
