package dslab.scenario.lab1;

import dslab.JunitSocketClient;
import dslab.Messages;
import dslab.PointsTestRunner;
import dslab.TestPoints;
import dslab.rules.MailboxServerRule;
import dslab.rules.TransferServerRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

@RunWith(PointsTestRunner.class)
public class TransferMailboxScenarioTest {

    private static final Log LOG = LogFactory.getLog(TransferMailboxScenarioTest.class);

    @Rule
    public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

    @Rule
    public MailboxServerRule mailboxEarth = new MailboxServerRule("mailbox-earth-planet");

    @Rule
    public MailboxServerRule mailboxUniver = new MailboxServerRule("mailbox-univer-ze");

    @Rule
    public TransferServerRule transferServer = new TransferServerRule("transfer-1");

    @Rule
    public ErrorCollector err = new ErrorCollector();

    @Test
    @TestPoints(1.5)
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

    @Test
    @TestPoints(1.5)
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


        LOG.info("Checking mailbox server earth.planet");
        try (JunitSocketClient client = new JunitSocketClient(mailboxUniver.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login zaphod 12345", "ok");

            client.send("list");
            String listResult = client.listen();

            err.checkThat("list command output did not match", listResult, containsString("trillian@earth.planet"));
            err.checkThat("list command output did not match", listResult, containsString("hello there"));

            client.send("logout");
            client.send("quit");
        }

    }

    @Test
    @TestPoints(2)
    public void scen_03_failureMessage() throws Exception {

        LOG.info("Sending message to non-existing email address");
        try (JunitSocketClient client = new JunitSocketClient(transferServer.getDmtpPort())) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");

            client.send("to zaphod@notexist.com");
            err.checkThat("Expected transfer server to optimistically accept non-existing domain",
                    client.read(), containsString("ok 1"));

            client.sendAndVerify("subject hello there", "ok");
            client.sendAndVerify("data this never arrives", "ok");
            client.sendAndVerify("send", "ok");
        }

        Thread.sleep(2000); // wait a bit for message to arrive

        LOG.info("Checking mailbox server for error mail");
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");

            client.send("list");
            String listResult = client.listen();

            err.checkThat("expected a mail from mailer@ in inbox", listResult, containsString("mailer@"));

            client.send("logout");
            client.send("quit");
        }
    }


}
