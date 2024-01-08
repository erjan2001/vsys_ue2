package dslab.nameserver;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.ComponentFactory;
import dslab.util.Config;

public class Nameserver implements INameserver {

    private final Config config;
    private final Shell shell;
    private final boolean isRoot;
    private final ConcurrentHashMap<String, INameserverRemote> childNameServers;
    private final ConcurrentHashMap<String, String> mailboxServers;
    private INameserverRemote nameserverRemote;
    private Registry registry;


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + "> ");
        this.config = config;
        this.isRoot = !config.containsKey("domain");

        this.childNameServers = new ConcurrentHashMap<>();
        this.mailboxServers = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        nameserverRemote = new NameserverRemote(childNameServers, mailboxServers);
        try {
            INameserverRemote nameserverStub = (INameserverRemote) UnicastRemoteObject.exportObject(nameserverRemote, 0);
            if (isRoot) {
                registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
                registry.rebind(config.getString("root_id"), nameserverStub);
            } else {
                registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
                INameserverRemote rootNameserver = (INameserverRemote) registry.lookup(config.getString("root_id"));
                rootNameserver.registerNameserver(config.getString("domain"), nameserverStub);
            }
        } catch (RemoteException e) {
            System.err.println("Remote operation failed: " + e.getMessage());
            shutdown();
        } catch (NotBoundException e) {
            System.err.println("Root Nameserver not bound: " + e.getMessage());
            shutdown();
        } catch (AlreadyRegisteredException e) {
            System.err.println("Nameserver already registered: " + e.getMessage());
            shutdown();
        } catch (InvalidDomainException e) {
            System.err.println("Invalid domain: " + e.getMessage());
            shutdown();
        }


        System.out.println("Nameserver is up! Use <shutdown> to exit!");
        shell.run();
    }

    @Override
    public void nameservers() {
        // TODO
    }

    @Override
    public void addresses() {
        // TODO
    }

    @Override
    public void shutdown() {
        try {
            UnicastRemoteObject.unexportObject(nameserverRemote, true);

            if (isRoot) {
                registry.unbind(config.getString("root_id"));
                UnicastRemoteObject.unexportObject(registry, true);
            }

            System.out.println("Shutdown successful");
        } catch (NoSuchObjectException e) {
            System.err.println("The object does not exist or has already been unexported: " + e);
        } catch (AccessException e) {
            System.err.println("Access to the object or registry is denied: " + e);
        } catch (NotBoundException e) {
            System.err.println("The requested name is not currently bound in the registry: " + e);
        } catch (RemoteException e) {
            System.err.println("A general communication error occurred: " + e);
        }

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }

}
