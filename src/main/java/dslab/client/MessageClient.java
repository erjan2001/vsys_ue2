package dslab.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.Email;
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
        this.dmapExecuter = Executors.newSingleThreadExecutor();
        this.loadKey();
        this.startDMAPClient();
    }

    private void startDMAPClient() {
        this.dmapClient = new DMAPClient(this, this.config, this.shell, this.componentId);
        this.dmapExecuter.submit(this.dmapClient);
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
        System.out.println("here");
        this.dmapClient.inbox();
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
        String resp;
        try (Socket socket = new Socket(this.transferHost, this.transferPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("begin");
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            writer.println("to " + to);
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            writer.println("from " + config.getString("transfer.email"));
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            writer.println("subject " + subject);
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            writer.println("data " + data);
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            writer.println("hash " + Email.generateHash(this.secretKey, subject, data, config.getString("transfer.email"), to));
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            writer.println("send");
            resp = reader.readLine();
            if(!resp.equals("ok")) {
                this.shell.out().println(resp);
                return;
            }

            shell.out().println("ok");
        } catch (IOException e) {
            this.shell.out().println(e.getMessage());
        }
    }

    @Command
    @Override
    public void shutdown() {
        try {
            this.dmapClient.shutdown();
        } catch (IOException e) {
            // nothing to do
        }
        this.dmapExecuter.shutdown();
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
