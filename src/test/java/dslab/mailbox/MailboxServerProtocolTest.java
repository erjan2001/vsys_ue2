package dslab.mailbox;

import dslab.*;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

@RunWith(PointsTestRunner.class)
public class MailboxServerProtocolTest extends TestBase {

    private static final String ID_REGEX = "^([0-9a-zA-Z-_]+)";

    private static final Log LOG = LogFactory.getLog(MailboxServerProtocolTest.class);

    private final String componentId = "mailbox-earth-planet";

    private IMailboxServer component;
    private int dmapServerPort;
    private int dmtpServerPort;

    @Before
    public void setUp() throws Exception {
        component = ComponentFactory.createMailboxServer(componentId, in, out);
        dmapServerPort = new Config(componentId).getInt("dmap.tcp.port");
        dmtpServerPort = new Config(componentId).getInt("dmtp.tcp.port");

        new Thread(component).start();

        LOG.info("Waiting for server sockets to appear");
        Sockets.waitForSocket("localhost", dmapServerPort, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", dmtpServerPort, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        in.addLine("shutdown"); // send "shutdown" command to command line
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)

    @TestPoints(0.5)
    public void mbx_01_dmtp_simple() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from zaphod@univer.ze", "ok");
            client.sendAndVerify("to arthur@earth.planet", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }

    @Test(timeout = 15000)

    @TestPoints(0.5)
    public void mbx_02_dmtp_ignoresForeignAddresses() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to arthur@earth.planet,zaphod@univer.ze", "ok 1");
            client.sendAndVerify("subject testsubject", "ok");
            client.sendAndVerify("data testdata", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void mbx_03_dmtp_rejectsUnknownAddresses() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to unknown@earth.planet", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void mbx_04_login_withIllegalCredentials() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian WRONGPW", "error");
            client.send("quit");
        }

        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login zaphod 12345", "error"); // zaphod does not exist on earth
            client.send("quit");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void mbx_05_login_Logout() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void mbx_06_list_showsAcceptedMailCorrectly() throws Exception {

        // accept a message via DMTP (to trillian)
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from arthur@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok 1");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit", "ok bye");
        }

        // list the message via DMAP list
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");

            client.send("list");
            String listResult = client.listen();
            err.checkThat(listResult, containsString("arthur@earth.planet hello"));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok bye");
        }
    }


    @Test(timeout = 15000)
    @TestPoints(1)
    public void mbx_07_list_showsOnlyUsersEmails() throws Exception {
        // accept a message via DMTP (to trillian)
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "arthur@earth.planet",
                    "trillian@earth.planet",
                    "to trillian", "hello from arthur"
            );
            client.send("quit");
        }
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "zaphod@univer.ze",
                    "arthur@earth.planet",
                    "to arthur", "hello from zaphod"
            );
            client.send("quit");
        }

        // list trillian's messages via DMAP list
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");
            client.send("list");

            String listResult = client.listen();

            err.checkThat("list should list trillian's mails", listResult, containsString("to trillian"));
            err.checkThat("list should  not list arthur's mails", listResult, not(containsString("to arthur")));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }


        // list arthurs's messages via DMAP list
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login arthur 23456", "ok");
            client.send("list");

            String listResult = client.listen();

            err.checkThat("list should not list arthur's mails", listResult, not(containsString("to trillian")));
            err.checkThat("list should list arthur's mails", listResult, containsString("to arthur"));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(1)
    public void mbx_08_show_showsEmailCorrectly() throws Exception {

        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "arthur@earth.planet",
                    "trillian@earth.planet",
                    "to trillian", "hello from arthur"
            );
        }

        String id = null;
        // list trillian's messages via DMAP list
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");
            client.send("list");

            String listResult = client.listen();
            Pattern p = Pattern.compile(ID_REGEX);

            Matcher matcher = p.matcher(listResult);

            try {
                if (!matcher.find()) {
                    throw new IllegalStateException();
                }
                id = matcher.group();
            } catch (IllegalStateException e) {
                throw new AssertionError("Could not extract ID from list result: '" + listResult + "'");
            }

            client.send("show " + id);

            String showResult = client.listen();

            err.checkThat("expected show to contain from", showResult, containsString("arthur@earth.planet"));
            err.checkThat("expected show to contain to", showResult, containsString("trillian@earth.planet"));
            err.checkThat("expected show to contain subject", showResult, containsString("to trillian"));
            err.checkThat("expected show to contain data", showResult, containsString("hello from arthur"));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }

    @Test(timeout = 15000)
    @TestPoints(0.5)
    public void mbx_09_delete_removesMail() throws Exception {

        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            client.verify("ok DMTP");
            Messages.send(client,
                    "arthur@earth.planet",
                    "trillian@earth.planet",
                    "to trillian", "hello from arthur"
            );
            client.send("quit");
        }

        String id = null;
        // list trillian's messages via DMAP list
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");
            client.send("list");

            String listResult = client.listen();
            Pattern p = Pattern.compile(ID_REGEX);
            Matcher matcher = p.matcher(listResult);

            try {
                if (!matcher.find()) {
                    throw new IllegalStateException();
                }
                id = matcher.group();
            } catch (IllegalStateException e) {
                throw new AssertionError("Could not extract ID from list result: '" + listResult + "'");
            }

            client.sendAndVerify("delete " + id, "ok");
            client.sendAndVerify("show " + id, "error");
            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok");
        }
    }


}
