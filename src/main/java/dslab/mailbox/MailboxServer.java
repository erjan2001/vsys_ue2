package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.Email;

public class MailboxServer implements IMailboxServer, Runnable {

    private String componentId;
    private Config config;
    private Config users;
    private ServerSocket dmtpServer;
    private ServerSocket dmapServer;
    private Shell shell;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, Config users, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.users = users;
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(this.componentId + " < ");

        try {
            this.dmtpServer = new ServerSocket(this.config.getInt("dmtp.tcp.port"));
            this.dmapServer = new ServerSocket(this.config.getInt("dmap.tcp.port"));
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating dmtp/dmap server", e);
        }
    }

    @Override
    public void run() {
        HashMap<String, String> userPassword = new HashMap<>();
        ConcurrentHashMap<String, Map<Integer, Email>> userInbox = new ConcurrentHashMap<>();

        for(String user : this.users.listKeys()){
            userPassword.put(user, this.users.getString(user));
            userInbox.put(user, new ConcurrentHashMap<>());
        }

        new MailboxDMTPServerThread(this.dmtpServer, this.users.listKeys(), this.config,userInbox).start();
        new MailboxDMAPServerThread(this.dmapServer, userInbox, userPassword).start();
        this.shell.run();
    }

    @Override
    @Command
    public void shutdown() {
        if(this.dmtpServer != null){
            try {
                this.dmtpServer.close();
            } catch (IOException e) {
                System.err.println("Error while closing dmtp server.\n" + e.getMessage());
            }
        }
        if (this.dmapServer != null){
            try {
                this.dmapServer.close();
            } catch (IOException e) {
                System.err.println("Error while closing dmap server.\n" + e.getMessage());
            }
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
