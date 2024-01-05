package dslab.monitoring;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer, Runnable {

    private Shell shell;

    private ConcurrentHashMap<String, Integer> users;

    private ConcurrentHashMap<String, Integer> domains;

    private DatagramSocket datagramSocket;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.shell = new Shell(in, out);
        this.shell.register(this);
        this.shell.setPrompt(componentId + " < ");
        this.users = new ConcurrentHashMap<>();
        this.domains = new ConcurrentHashMap<>();
        try {
            this.datagramSocket = new DatagramSocket(Integer.parseInt(config.getString("udp.port")));
        } catch (SocketException e) {
            this.shutdown();
        }
    }

    @Override
    public void run() {
        new MonitoringServerThread(this.datagramSocket, this.users, this.domains).start();
        this.shell.run();
    }

    @Override
    @Command
    public void addresses() {
        for (Map.Entry<String, Integer> entry : this.domains.entrySet()) {
            String key = entry.getKey().replaceAll("\\u0000", "");
            Integer value = entry.getValue();
            this.shell.out().println(key + " " + value + "\n\r");
        }
    }

    @Override
    @Command
    public void servers() {
        for (Map.Entry<String, Integer> entry : this.users.entrySet()) {
            String key = entry.getKey().replaceAll("\\u0000", "");
            Integer value = entry.getValue();
            this.shell.out().println(key + " " + value + "\n\r");
        }
    }

    @Override
    @Command
    public void shutdown() {
        if(this.datagramSocket != null){
            this.datagramSocket.close();
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
