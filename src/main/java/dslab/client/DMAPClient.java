package dslab.client;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.util.Config;

public class DMAPClient implements Runnable{

    private final MessageClient messageClient;

    private final Config config;

    private final Shell shell;
    public DMAPClient(MessageClient messageClient, Config config, Shell shell) {
        this.messageClient = messageClient;
        this.config = config;
        this.shell = shell;
    }

    @Override
    public void run() {

    }
}
