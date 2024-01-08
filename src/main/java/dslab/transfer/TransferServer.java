package dslab.transfer;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.nameserver.AlreadyRegisteredException;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;
import dslab.util.Config;
import dslab.util.Globals;

public class TransferServer implements ITransferServer, Runnable, Globals {

    private final ServerSocket transferServer;
    private final Config config;
    private final Shell shell;
    private INameserverRemote rootNameserver;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + " < ");

        try {
            transferServer = new ServerSocket(config.getInt("tcp.port"));
        } catch (IOException e) {
            throw new UncheckedIOException("Error starting" + componentId, e);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
            rootNameserver = (INameserverRemote) registry.lookup(config.getString("root_id"));
        } catch (NotBoundException | RemoteException e) {
            System.err.println("Root Nameserver could not be reached: " + e.getMessage());
            System.err.println("Shutting down...");
            shutdown();
        }
    }

    @Override
    public void run() {
        new TransferServerListenerThread(this.transferServer, config, rootNameserver).start();
        this.shell.run();
    }

    @Override
    @Command
    public void shutdown() throws StopShellException {
        if (this.transferServer != null) {
            try {
                this.transferServer.close();
            } catch (IOException e) {
                System.err.println("Closing TransferServer failed!" + e);
            }
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }
}
