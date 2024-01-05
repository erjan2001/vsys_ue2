package dslab.rules;

import dslab.ComponentFactory;
import dslab.ComponentRule;
import dslab.client.IMessageClient;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * MessageClientRule.
 */
public class MessageClientRule extends ComponentRule<IMessageClient> {

    public MessageClientRule(String componentId) {
        super(componentId);
    }

    @Override
    protected IMessageClient createComponent(String componentId, InputStream in, PrintStream out) throws Exception {
        return ComponentFactory.createMessageClient(componentId, in, out);
    }

    @Override
    protected void waitForStartup() {
        sleep(1000);
    }
}
