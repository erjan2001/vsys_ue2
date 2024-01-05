package dslab;

import dslab.client.MessageClientScenarioTest;
import dslab.client.MessageClientStartupTest;
import dslab.mailbox.MailboxStartsecureTest;
import dslab.naming.NameserverMailboxserverTest;
import dslab.naming.NameserverRmiTest;
import dslab.naming.NameserverShowTest;
import dslab.scenario.lab2.ScenarioTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(GlobalTestResultAggregator.class)
@Suite.SuiteClasses({
        MailboxStartsecureTest.class,
        MessageClientScenarioTest.class,
        MessageClientStartupTest.class,
        NameserverMailboxserverTest.class,
        NameserverRmiTest.class,
        NameserverShowTest.class,
        ScenarioTest.class
})
public class Lab2Suite {
}
