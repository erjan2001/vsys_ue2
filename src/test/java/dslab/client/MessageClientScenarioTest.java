package dslab.client;

import dslab.JunitSocketClient;
import dslab.Messages;
import dslab.PointsTestRunner;
import dslab.TestPoints;
import dslab.rules.MailboxServerRule;
import dslab.rules.MessageClientRule;
import dslab.rules.NameserverRule;
import dslab.rules.TransferServerRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;

/**
 * MessageClientScenarioTest.
 */
@RunWith(PointsTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MessageClientScenarioTest {

    private static final String ID_REGEX = "^([0-9a-zA-Z-_]+)";

    private static final Log LOG = LogFactory.getLog(MessageClientScenarioTest.class);
    @Rule
    public ErrorCollector err = new ErrorCollector();
    @Rule
    public Timeout timeout = new Timeout(45, TimeUnit.SECONDS);
    private final NameserverRule nsRoot = new NameserverRule("ns-root");
    private final NameserverRule nsZe = new NameserverRule("ns-ze");
    private final NameserverRule nsPlanet = new NameserverRule("ns-planet");
    private final MailboxServerRule mailboxEarth = new MailboxServerRule("mailbox-earth-planet");
    private final MailboxServerRule mailboxUniver = new MailboxServerRule("mailbox-univer-ze");
    private final TransferServerRule transferServer = new TransferServerRule("transfer-1");
    private final MessageClientRule msgClientArthur = new MessageClientRule("client-arthur");
    private final MessageClientRule msgClientTrillian = new MessageClientRule("client-trillian");
    private final MessageClientRule msgClientZaphod = new MessageClientRule("client-zaphod");
    @Rule
    public RuleChain serverRules = RuleChain
            .outerRule(nsRoot)
            .around(nsZe)
            .around(nsPlanet)
            .around(mailboxEarth)
            .around(mailboxUniver)
            .around(transferServer)
            .around(msgClientArthur)
            .around(msgClientTrillian)
            .around(msgClientZaphod);

    @Test
    @TestPoints(2)
    public void mc_01_msg_sendsAndMessageCorrectly() throws Exception {
        String output;

        msgClientTrillian.getIn().addLine("msg arthur@earth.planet \"hello from trillian\" \"some data from trillian\"");
        output = msgClientTrillian.getOut().listen(2, TimeUnit.SECONDS);

        LOG.info("msg command returned " + output);

        LOG.info("Checking mailbox server earth.planet");
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort())) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            client.send("list");
            String listResult = client.listen(2, TimeUnit.SECONDS);

            err.checkThat("list command output did not match", listResult, containsString("trillian@earth.planet"));
            err.checkThat("list command output did not match", listResult, containsString("hello from trillian"));

            client.send("logout");
            client.send("quit");
        }
    }


    @Test
    @TestPoints(1)
    public void mc_02_inbox_displaysMessagesCorrectly() throws Exception {
        LOG.info("Sending message");
        try (JunitSocketClient client = new JunitSocketClient(transferServer.getDmtpPort())) {
            client.verify("ok DMTP2.0");
            Messages.send(client,
                    "trillian@earth.planet",
                    "arthur@earth.planet",
                    "from trillian 1",
                    "this is test message1");

            Messages.send(client,
                    "trillian@earth.planet",
                    "arthur@earth.planet",
                    "from trillian 2",
                    "this is test message2");

            Messages.send(client,
                    "trillian@earth.planet",
                    "zaphod@univer.ze",
                    "from trillian 3",
                    "this is test message3");

            client.send("quit");
        }

        Thread.sleep(3000);

        msgClientArthur.getIn().addLine("inbox");
        String output = msgClientArthur.getOut().listen(5, TimeUnit.SECONDS);

        LOG.info("Inbox output is: " + output);

        err.checkThat(output, containsString("from trillian 1"));
        err.checkThat(output, containsString("this is test message1"));
        err.checkThat(output, containsString("from trillian 2"));
        err.checkThat(output, containsString("this is test message2"));
        err.checkThat("arthur's inbox should not show mails to zaphod", output, not(containsString("from trillian 3")));
        err.checkThat("arthur's inbox should not show mails to zaphod", output, not(containsString("this is test message3")));
    }

    @Test
    @TestPoints(1)
    public void mc_03_verify_withValidSignature_returnsOk() throws Exception {
        String output;
        msgClientTrillian.getIn().addLine("msg arthur@earth.planet \"verifytest1\" \"some data from trillian\"");
        output = msgClientTrillian.getOut().listen(2, TimeUnit.SECONDS);

        LOG.info("msg command returned " + output);

        String id = null;
        // list arthurs's messages via DMAP list and find ID
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort(), err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            id = findId("verifytest1", client);

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }

        assertNotNull("couldn't find id of message, can't verify", id);

        LOG.info("Verifying message with id " + id);
        msgClientArthur.getIn().addLine("verify " + id);
        err.checkThat(msgClientArthur.getOut().listen(2, TimeUnit.SECONDS), containsString("ok"));
    }

    @Test
    @TestPoints(1)
    public void mc_04_verify_withIllegalSignature_returnsFalse() throws Exception {
        LOG.info("Sending message");
        try (JunitSocketClient client = new JunitSocketClient(transferServer.getDmtpPort())) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "trillian@earth.planet",
                    "arthur@earth.planet",
                    "verifytest2",
                    "this is a test message",
                    "ly38u+dnK1tyKhnR63sC2tW8FsCyFoIyR7yGHmre2Jo=" // invalid hash
            );

            client.send("quit");
        }

        Thread.sleep(2000);

        String id = null;
        // list arthurs's messages via DMAP list and find ID
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort(), err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            id = findId("verifytest2", client);

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }

        assertNotNull("couldn't find id of message, can't verify", id);

        msgClientArthur.getIn().addLine("verify " + id);
        err.checkThat(msgClientArthur.getOut().listen(2, TimeUnit.SECONDS), containsString("error"));
    }

    @Test
    @TestPoints(1)
    public void mc_05_delete_deletesMessageFromServer() throws Exception {
        LOG.info("Sending message");
        try (JunitSocketClient client = new JunitSocketClient(transferServer.getDmtpPort())) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "trillian@earth.planet",
                    "arthur@earth.planet",
                    "deletetest1",
                    "this is a test message",
                    "ly38u+dnK1tyKhnR63sC2tW8FsCyFoIyR7yGHmre2Jo=" // invalid hash
            );

            client.send("quit");
        }

        Thread.sleep(2000);

        String id = null;
        // list arthurs's messages via DMAP list and find ID
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort(), err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            id = findId("deletetest1", client);

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }

        assertNotNull("couldn't find id of message, can't verify", id);

        msgClientArthur.getIn().addLine("delete " + id);
        err.checkThat(msgClientArthur.getOut().listen(2, TimeUnit.SECONDS), containsString("ok"));

        // check that it removes the message from the server
        try (JunitSocketClient client = new JunitSocketClient(mailboxEarth.getDmapPort(), err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");

            String list = client.sendAndListen("list");
            err.checkThat("list still shows deleted message", list, not(containsString("deletetest1")));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }

    private String findId(String searchString, JunitSocketClient client) throws IOException {
        client.send("list");

        while (true) {
            String listResult = client.read();
            if (listResult == null || "ok".equals(listResult)) {
                break;
            }

            if (listResult.contains(searchString)) {
                Pattern p = Pattern.compile(ID_REGEX);
                Matcher matcher = p.matcher(listResult);

                try {
                    if (!matcher.find()) {
                        throw new IllegalStateException();
                    }
                    return matcher.group();
                } catch (IllegalStateException e) {
                    throw new AssertionError("Could not extract ID from list result: '" + listResult + "'");
                }
            }
        }

        return null;
    }
}
