package dslab.naming;

import dslab.PointsTestRunner;
import dslab.TestPoints;
import dslab.rules.MailboxServerRule;
import dslab.rules.NameserverRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

/**
 * NameserverMailboxserverTest.
 */
@RunWith(PointsTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NameserverMailboxserverTest {

    @Rule
    public ErrorCollector err = new ErrorCollector();
    private final NameserverRule nsRoot = new NameserverRule("ns-root");
    private final NameserverRule nsPlanet = new NameserverRule("ns-planet");
    private final NameserverRule nsZe = new NameserverRule("ns-ze");
    private final MailboxServerRule mbxEarth = new MailboxServerRule("mailbox-earth-planet");
    private final MailboxServerRule mbxUniver = new MailboxServerRule("mailbox-univer-ze");
    @Rule
    public RuleChain serverRules = RuleChain
            .outerRule(nsRoot)
            .around(nsPlanet)
            .around(nsZe)
            .around(mbxEarth)
            .around(mbxUniver);

    @Test
    @TestPoints(1.0)
    public void ns_03_addresses_returnsCorrectAddresses() throws Exception {
        String output;

        nsRoot.getIn().addLine("addresses");
        output = nsRoot.getOut().listen();
        err.checkThat("root nameserver should not list mailbox servers", output, not(containsString("earth")));
        err.checkThat("root nameserver should not list mailbox servers", output, not(containsString("univer")));

        nsPlanet.getIn().addLine("addresses");
        output = nsPlanet.getOut().listen();
        err.checkThat("ze nameserver should list earth.planet mailbox server", output, containsString("earth"));
        err.checkThat("ze nameserver should not list univer.ze mailbox server", output, not(containsString("univer")));

        nsZe.getIn().addLine("addresses");
        output = nsZe.getOut().listen();
        err.checkThat("ze nameserver should list univer.ze mailbox server", output, containsString("univer"));
        err.checkThat("ze nameserver should not list earth.planet mailbox server", output, not(containsString("earth")));
    }

}
