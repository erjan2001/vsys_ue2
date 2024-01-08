package dslab.client;

import java.io.InputStream;
import java.io.PrintStream;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.ComponentFactory;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.SecretKey;

public class MessageClient implements IMessageClient, Runnable {


    private static final Log LOG = LogFactory.getLog(MessageClient.class);

    private final Shell shell;
    private final int transferPort;

    private final String transferHost;

    private final String componentId;

    private final Config config;
    private SecretKey secretKey;

    /**
     * Creates a new client instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.componentId = componentId;
        this.transferPort = config.getInt("transfer.port");
        this.transferHost = config.getString("tranfer.host");
        this.shell = new Shell(in, out);

        shell.register(this);
        shell.setPrompt(componentId + " < ");

    }

    @Override
    public void run() {

    }

    @Override
    public void inbox() {

    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void verify(String id) {

    }

    @Override
    public void msg(String to, String subject, String data) {

    }

    @Override
    public void shutdown() {

    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
