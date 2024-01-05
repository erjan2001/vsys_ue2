package dslab;

import dslab.mailbox.MailboxServerProtocolTest;
import dslab.mailbox.MailboxServerTest;
import dslab.monitoring.MonitoringServerProtocolTest;
import dslab.monitoring.MonitoringServerTest;
import dslab.scenario.lab1.MonitoringScenarioTest;
import dslab.scenario.lab1.TransferMailboxScenarioTest;
import dslab.transfer.TransferServerProtocolTest;
import dslab.transfer.TransferServerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(GlobalTestResultAggregator.class)
@Suite.SuiteClasses({
        TransferServerTest.class,
        TransferServerProtocolTest.class,
        MailboxServerTest.class,
        MailboxServerProtocolTest.class,
        MonitoringServerTest.class,
        MonitoringServerProtocolTest.class,
        TransferMailboxScenarioTest.class,
        MonitoringScenarioTest.class
})
public class Lab1Suite {
}
