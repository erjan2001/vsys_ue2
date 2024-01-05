package dslab.transfer;

import dslab.*;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TransferServerProtocolTest.
 */
@RunWith(PointsTestRunner.class)
public class TransferServerProtocolTest extends TestBase {

    private static final Log LOG = LogFactory.getLog(TransferServerProtocolTest.class);

    private final String componentId = "transfer-1";

    private ITransferServer component;
    private int serverPort;

    @Before
    public void setUp() throws Exception {
        component = ComponentFactory.createTransferServer(componentId, in, out);
        serverPort = new Config(componentId).getInt("tcp.port");
        new Thread(component).start();

        LOG.info("Waiting for server socket to appear");
        Sockets.waitForSocket("localhost", serverPort, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        in.addLine("shutdown"); // send "shutdown" command to command line
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)
    @TestPoints(1.5)
    public void tfs_01_dmtp_simple() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(serverPort)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from arthur@earth.planet", "ok");
            client.sendAndVerify("to zaphod@univer.ze", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void tfs_02_sendWithoutData_returnsErrorOnSend() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(serverPort)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void tfs_03_sendWithoutRecipient_returnsErrorOnSend() throws Exception {
        try (dslab.JunitSocketClient client = new dslab.JunitSocketClient(serverPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok");
        }
    }

}
