package dslab.scenario.lab2;

import dslab.JunitSocketClient;
import dslab.Messages;
import dslab.PointsTestRunner;
import dslab.TestPoints;
import dslab.rules.MailboxServerRule;
import dslab.rules.NameserverRule;
import dslab.rules.TransferServerRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

/**
 * ScenarioTest.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PointsTestRunner.class)
public class ScenarioTest {

    private static final Log LOG = LogFactory.getLog(ScenarioTest.class);
    @Rule
    public ErrorCollector err = new ErrorCollector();
    private final NameserverRule nsRoot = new NameserverRule("ns-root");
    private final NameserverRule nsZe = new NameserverRule("ns-ze");
    private final NameserverRule nsPlanet = new NameserverRule("ns-planet");
    private final MailboxServerRule mailboxEarth = new MailboxServerRule("mailbox-earth-planet");
    private final MailboxServerRule mailboxUniver = new MailboxServerRule("mailbox-univer-ze");
    private final TransferServerRule transferServer = new TransferServerRule("transfer-1");
    @Rule
    public RuleChain serverRules = RuleChain
            .outerRule(nsRoot)
            .around(nsZe)
            .around(nsPlanet)
            .around(mailboxEarth)
            .around(mailboxUniver)
            .around(transferServer);

    @Test(timeout = 30000)
    @TestPoints(0.5)
    public void scen_01_simpleEmailSending() throws Exception {
        LOG.info("Sending message");
        try (JunitSocketClient client = new JunitSocketClient(transferServer.getDmtpPort())) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "trillian@earth.planet",
                    "arthur@earth.planet",
                    "hello there",
                    "this is a test message");
            client.send("quit");
        }

        Thread.sleep(1000); // wait a bit for message to arrive

        LOG.info("Checking mailbox server earth.planet");
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            client.send("list");
            String listResult = client.listen();

            err.checkThat("list command output did not match", listResult, containsString("trillian@earth.planet"));
            err.checkThat("list command output did not match", listResult, containsString("hello there"));

            client.send("logout");
            client.send("quit");
        }

        LOG.info("Checking mailbox server which did not receive a message");
        try (JunitSocketClient client = new JunitSocketClient(mailboxUniver.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login zaphod 12345", "ok");

            client.send("list");
            String listResult = client.listen();

            err.checkThat("expected list for zaphod to be empty", listResult, not(containsString("trillian@earth.planet")));

            client.send("logout");
            client.send("quit");
        }
    }

    @Test(timeout = 30000)
    @TestPoints(0.5)
    public void scen_02_multipleRecipients() throws Exception {
        LOG.info("Sending message");
        try (JunitSocketClient client = new JunitSocketClient(transferServer.getDmtpPort())) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "trillian@earth.planet",
                    "arthur@earth.planet,zaphod@univer.ze",
                    "hello there",
                    "this is a test message"
            );
            client.send("quit");
        }

        Thread.sleep(3000); // wait a bit for message to arrive

        LOG.info("Checking mailbox server earth.planet");
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            client.send("list");
            String listResult = client.listen(2, TimeUnit.SECONDS);

            err.checkThat("list command output did not match", listResult, containsString("trillian@earth.planet"));
            err.checkThat("list command output did not match", listResult, containsString("hello there"));

            client.send("logout");
            client.send("quit");
        }


        LOG.info("Checking mailbox server earth.planet");
        try (JunitSocketClient client = new JunitSocketClient(mailboxUniver.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login zaphod 12345", "ok");

            client.send("list");
            String listResult = client.listen();

            System.out.println(listResult);

            err.checkThat("list command output did not match", listResult, containsString("trillian@earth.planet"));
            err.checkThat("list command output did not match", listResult, containsString("hello there"));

            client.send("logout");
            client.send("quit");
        }

    }

}
