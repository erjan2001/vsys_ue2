package dslab.scenario.lab1;

import dslab.JunitSocketClient;
import dslab.PointsTestRunner;
import dslab.TestPoints;
import dslab.rules.MailboxServerRule;
import dslab.rules.MonitoringServerRule;
import dslab.rules.TransferServerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * ScenarioWithMonitoringTest.
 */
@RunWith(PointsTestRunner.class)
public class MonitoringScenarioTest {

    @Rule
    public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

    @Rule
    public MailboxServerRule mailboxEarth = new MailboxServerRule("mailbox-earth-planet");

    @Rule
    public TransferServerRule transferServer1 = new TransferServerRule("transfer-1");

    @Rule
    public TransferServerRule transferServer2 = new TransferServerRule("transfer-2");

    @Rule
    public MonitoringServerRule monitoringServer = new MonitoringServerRule("monitoring");

    @Rule
    public ErrorCollector err = new ErrorCollector();

    @Test
    @TestPoints(2)
    public void scen_04_transferServerSendsMonitoringData() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(transferServer1.getDmtpPort())) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from zaphod@univer.ze", "ok");
            client.sendAndVerify("to arthur@earth.planet", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }

        try (JunitSocketClient client = new JunitSocketClient(transferServer1.getDmtpPort())) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from zaphod@univer.ze", "ok");
            client.sendAndVerify("to arthur@earth.planet", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }

        try (JunitSocketClient client = new JunitSocketClient(transferServer1.getDmtpPort())) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to arthur@earth.planet", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }

        try (JunitSocketClient client = new JunitSocketClient(transferServer2.getDmtpPort())) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to arthur@earth.planet", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }

        Thread.sleep(2000);

        monitoringServer.getIn().addLine("addresses");
        Thread.sleep(500);
        String addresses = String.join("\n", monitoringServer.getOut().listen());
        err.checkThat("addresses output did not match", addresses, containsString("zaphod@univer.ze 2"));
        err.checkThat("addresses output did not match", addresses, containsString("trillian@earth.planet 2"));

        monitoringServer.getIn().addLine("servers");
        Thread.sleep(500);
        String servers = String.join("\n", monitoringServer.getOut().listen());

        int t1port = transferServer1.getDmtpPort();
        int t2port = transferServer2.getDmtpPort();

        err.checkThat("servers output did not match", servers, containsString(":" + t1port + " 3"));
        err.checkThat("servers output did not match", servers, containsString(":" + t2port + " 1"));
    }

}
