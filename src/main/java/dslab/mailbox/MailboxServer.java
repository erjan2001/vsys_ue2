package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.nameserver.AlreadyRegisteredException;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;
import dslab.util.Config;
import dslab.util.Email;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MailboxServer implements IMailboxServer, Runnable {

    private final Config config;
    private final Config users;
    private final ServerSocket dmtpServer;
    private final ServerSocket dmapServer;
    private final Shell shell;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, Config users, InputStream in, PrintStream out) {
        this.config = config;
        this.users = users;
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + " < ");

        try {
            this.dmtpServer = new ServerSocket(this.config.getInt("dmtp.tcp.port"));
            this.dmapServer = new ServerSocket(this.config.getInt("dmap.tcp.port"));
        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating dmtp/dmap server", e);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
            INameserverRemote rootNameserver = (INameserverRemote) registry.lookup(config.getString("root_id"));
            rootNameserver.registerMailboxServer(config.getString("domain"), InetAddress.getLocalHost().getHostAddress() + ":" + config.getInt("dmtp.tcp.port"));
        } catch (RemoteException e) {
            System.err.println("Remote operation failed: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Root Nameserver not bound: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Unable to determine local host: " + e.getMessage());
        } catch (AlreadyRegisteredException e) {
            System.err.println("Domain is already registered: " + e.getMessage());
        } catch (InvalidDomainException e) {
            System.err.println("Invalid domain: " + e.getMessage());
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
        new MailboxDMAPServerThread(this.dmapServer, userInbox, userPassword, this.componentId).start();
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
