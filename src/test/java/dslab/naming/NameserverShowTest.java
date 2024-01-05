package dslab.naming;

import dslab.PointsTestRunner;
import dslab.TestPoints;
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
 * NameserverTest.
 */
@RunWith(PointsTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NameserverShowTest {

    @Rule
    public ErrorCollector err = new ErrorCollector();
    private final NameserverRule nsRoot = new NameserverRule("ns-root");
    private final NameserverRule nsPlanet = new NameserverRule("ns-planet");
    private final NameserverRule nsEarthPlanet = new NameserverRule("ns-earth-planet");
    @Rule
    public RuleChain serverRules = RuleChain
            .outerRule(nsRoot)
            .around(nsPlanet)
            .around(nsEarthPlanet);

    @Test(timeout = 15000)
    @TestPoints(1.0)
    public void ns_02_nameserver_returnsCorrectNameservers() throws Exception {
        nsRoot.getIn().addLine("nameservers");

        String output;

        output = nsRoot.getOut().listen();
        err.checkThat("root nameserver should list planet nameserver", output, containsString("planet"));
        err.checkThat("root nameserver should not list earth.planet nameserver", output, not(containsString("earth")));

        nsPlanet.getIn().addLine("nameservers");
        output = nsPlanet.getOut().listen();
        err.checkThat("planet nameserver should list earth.planet nameserver", output, containsString("earth"));
    }
}
