package dslab.util.dmtp;

import dslab.transfer.TransferServer;
import dslab.util.Config;
import dslab.util.Email;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class DMTPSend extends Thread {

    private final Config domains;
    private final BlockingQueue<Email> emailBlockingQueue;
    private final ConcurrentHashMap<String, Socket> mailServers;
    private DatagramSocket datagramSocket;

    private final Config config;

    private final ServerSocket transerServerSocket;
    private boolean quit;
    public DMTPSend(Config domains, BlockingQueue<Email> emailBlockingQueue, ConcurrentHashMap<String, Socket> mailServers, Config config, ServerSocket transferServerSocket) {
        this.domains = domains;
        this.emailBlockingQueue = emailBlockingQueue;
        this.mailServers = mailServers;
        this.config = config;
        this.transerServerSocket = transferServerSocket;
        this.quit = false;
        try {
            this.datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            //nothing to do
            //for testing monitoring doesn't need to run
        }

        BufferedReader bufferedReader = null;
        for (String domain :
                this.domains.listKeys()) {

            if (this.mailServers.containsKey(domain)) {
                continue;
            }

            String[] port = this.domains.getString(domain).split(":");
            Socket mailServer;
            try {
                mailServer = new Socket(port[0], Integer.parseInt(port[1]));
                bufferedReader = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
            } catch (ConnectException e) {
                //So it won't crash if one mailbox server isn't running
                continue;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            try {
                String line = bufferedReader.readLine();
                if (!line.equals("ok DMTP")) {
                    continue;
                }
            } catch (IOException e) {
                continue;
            }
            this.mailServers.put(domain, mailServer);
        }
    }

    @Override
    public void run() {
        try {

            while ((!Thread.currentThread().isInterrupted() && !this.quit) || !this.emailBlockingQueue.isEmpty()) {

                Email email = this.emailBlockingQueue.take();

                Email errorMail = new Email();
                errorMail.setTo(email.getFrom());
                String errorToDomain = email.getFrom().split("@")[1];
                errorMail.setSubject("email delivery failed");

                boolean err;

                HashSet<String> alreadySendDomains = new HashSet<>();

                for (String to :
                        email.getTo()) {
                    err = false;
                    String[] parts = to.split("@");
                    String domain = parts[1];
                    errorMail.setFrom("mailer@" + domain);

                    if (!alreadySendDomains.contains(domain)) {
                        if (!this.domains.containsKey(domain)) {
                            errorMail.setData(parts[1] + " is unknown");
                            err = true;
                        }
                    }

                    if (!err) {

                        this.mailServers.get(domain);

                        String errorData = this.forwardEmailToMailbox(email, this.mailServers.get(domain), to);
                        if (errorData == null) {
                            continue;
                        } else {
                            if (errorData.isEmpty()) {
                                if (this.datagramSocket != null) {
                                    this.sendToMonitoring(email);
                                }
                            } else {
                                this.forwardEmailToMailbox(errorMail, this.mailServers.get(errorToDomain), Arrays.toString(errorMail.getTo()));
                                if (this.datagramSocket != null) {
                                    this.sendToMonitoring(errorMail);
                                }
                            }
                        }
                    } else {
                        this.forwardEmailToMailbox(errorMail, this.mailServers.get(errorToDomain), Arrays.toString(errorMail.getTo()));
                        if (this.datagramSocket != null) {
                            this.sendToMonitoring(errorMail);
                        }
                    }
                    alreadySendDomains.add(domain);
                }
            }
            this.shutdown();
        } catch (SocketException e) {
            this.shutdown();
        } catch (NullPointerException e) {
            //e.printStackTrace();
        } catch (InterruptedException e) {
            // only gts here when take() is interrupted
            }
    }

    private String forwardEmailToMailbox(Email email, Socket mailServer, String to) throws SocketException {
        if (mailServer == null) {
            System.out.println("DMTPSEND mailserver==null!!");
            return null;
        }

        String errMsg = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
            PrintWriter printWriter = new PrintWriter(mailServer.getOutputStream(), true);

            String res;

            printWriter.println("begin");
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                return null;
            }

            printWriter.println("to " + to);
            res = bufferedReader.readLine();
            if (!res.startsWith("ok")) {
                return null;
            }

            printWriter.println("from " + email.getFrom());
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                if (res.startsWith("error")) {
                    errMsg += res;
                } else {
                    return null;
                }
            }

            printWriter.println("subject " + email.getSubject());
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                return null;
            }

            printWriter.println("data " + email.getData());
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                return null;
            }

            printWriter.println("send");
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                if (res.startsWith("error")) {
                    errMsg += res;
                }
            }
        } catch (InterruptedIOException e) {
            this.shutdown();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return errMsg;
    }

    private void sendToMonitoring(Email email) {
        if (this.datagramSocket != null) {
            byte[] buffer;
            DatagramPacket packet;

            String data;

            data = this.transerServerSocket.getInetAddress().getHostAddress()
                    + ":" + this.transerServerSocket.getLocalPort() + " " + email.getFrom();
            buffer = data.getBytes();
            try {
                packet = new DatagramPacket(buffer,
                        buffer.length,
                        InetAddress.getByName(this.config.getString("monitoring.host")),
                        this.config.getInt("monitoring.port"));
                this.datagramSocket.send(packet);
            } catch (NullPointerException e) {
                //only occurs if monitoringServer isn't running
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.out.println("datagramSocket is null. Cannot send to monitoring.");
        }
    }

    public void shutdown() {
        for (Socket mailbox :
                this.mailServers.values()) {
            if (mailbox != null) {
                try {
                    mailbox.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        if (this.datagramSocket != null) {
            this.datagramSocket.close();
        }
    }

    public void quit() {
        this.quit = true;
    }
}
