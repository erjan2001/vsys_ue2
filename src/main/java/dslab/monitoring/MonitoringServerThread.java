package dslab.monitoring;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringServerThread extends Thread {

    private ConcurrentHashMap<String, Integer> users;
    private ConcurrentHashMap<String, Integer> domains;
    private DatagramSocket datagramSocket;

    public MonitoringServerThread(DatagramSocket datagramSocket, ConcurrentHashMap<String, Integer> domains, ConcurrentHashMap<String, Integer> users) {
        this.users = users;
        this.domains = domains;
        this.datagramSocket = datagramSocket;
    }

    @Override
    public void run() {
        byte[] buffer;
        DatagramPacket packet;
        try {
            while (true) {
                buffer = new byte[1024];

                packet = new DatagramPacket(buffer, buffer.length);

                this.datagramSocket.receive(packet);

                String request = new String(packet.getData());

                System.out.println("Received request-packet from client: " + request);

                //<host>:<port> <email-address>
                String[] parts = request.split(" ");
                if(parts.length == 2) {
                    if (this.domains.containsKey(parts[0])) {
                        int number = this.domains.get(parts[0]);
                        this.domains.put(parts[0], ++number);
                    } else {
                        this.domains.put(parts[0], 1);
                    }

                    if (this.users.containsKey(parts[1])) {
                        int number = this.users.get(parts[1]);
                        this.users.put(parts[1], ++number);
                    } else {
                        this.users.put(parts[1], 1);
                    }
                }
            }
        } catch (SocketException e) {
            // nothing to do
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
