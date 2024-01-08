package dslab.client;

import java.io.*;
import java.util.concurrent.ExecutorService;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.Keys;

import javax.crypto.SecretKey;

public class MessageClient implements IMessageClient, Runnable {

    private final Shell shell;
    private final int transferPort;

    private final String transferHost;

    private final String componentId;

    private final Config config;
    private SecretKey secretKey;

    private ExecutorService dmapExecuter;

    private DMAPClient dmapClient;


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
        this.transferHost = config.getString("transfer.host");
        this.shell = new Shell(in, out);

        this.shell.register(this);
        this.shell.setPrompt(componentId + " < ");
        this.loadKey();
        this.startDMAPClient();
    }

    private void startDMAPClient() {
        this.dmapClient = new DMAPClient(this, this.config, this.shell);
        this.dmapExecuter.submit(dmapClient);
    }

    private void loadKey() {
        try{
            this.secretKey = Keys.readSecretKey(new File("./keys/hmac.key"));
        } catch (IOException e) {
            this.secretKey = null;
            throw new UncheckedIOException("Error while loading secret key", e);
        }
    }

    @Override
    public void run() {
        this.shell.run();
    }

    @Command
    @Override
    public void inbox() {

    }

    @Command
    @Override
    public void delete(String id) {

    }

    @Command
    @Override
    public void verify(String id) {

    }

    @Command
    @Override
    public void msg(String to, String subject, String data) {

    }

    @Command
    @Override
    public void shutdown() {
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
