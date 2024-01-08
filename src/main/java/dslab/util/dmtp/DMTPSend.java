package dslab.util.dmtp;

import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;
import dslab.util.Config;
import dslab.util.Email;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class DMTPSend extends Thread {

    private final INameserverRemote rootNameserver;
    private final BlockingQueue<Email> emailBlockingQueue;
    private DatagramSocket datagramSocket;

    private final Config config;

    private final ServerSocket transerServerSocket;
    private boolean quit;

    public DMTPSend(INameserverRemote rootNameserver, BlockingQueue<Email> emailBlockingQueue, Config config, ServerSocket transferServerSocket) {
        this.rootNameserver = rootNameserver;
        this.emailBlockingQueue = emailBlockingQueue;
        this.config = config;
        this.transerServerSocket = transferServerSocket;
        this.quit = false;
        try {
            this.datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            //nothing to do
            //for testing monitoring doesn't need to run
        }
    }

    @Override
    public void run() {
        try {

            while ((!Thread.currentThread().isInterrupted() && !this.quit) || !this.emailBlockingQueue.isEmpty()) {

                Email email = this.emailBlockingQueue.take();
                HashMap<String, String> alreadySendDomains = new HashMap<>();
                Email errorMail = new Email();
                errorMail.setTo(email.getFrom());
                String errorToDomain = email.getFrom().split("@")[1];
                String errorReceiverMailboxAddress = null;
                errorMail.setSubject("email delivery failed");




                for (String to :
                        email.getTo()) {

                    String[] parts = to.split("@");
                    String domain = parts[1];
                    errorMail.setFrom("mailer@" + domain);

                    String receiverMailboxAddress = null;

                    if (!alreadySendDomains.containsKey(domain)) { // this is merely a check to not make a double lookup if the domain exists
                        try {
                            receiverMailboxAddress = getReceiverMailboxAddress(domain);
                            alreadySendDomains.put(domain, receiverMailboxAddress);
                        } catch (RemoteException e) {
                            System.err.println("Remote operation failed: " + e.getMessage());
                            quit();
                            continue;
                        } catch (InvalidDomainException e) {
                            errorMail.setData(parts[1] + " is unknown");
                        }
                    } else {
                        receiverMailboxAddress = alreadySendDomains.get(domain);
                    }

                    if (receiverMailboxAddress != null) {
                        String errorData = this.forwardEmailToMailbox(email, receiverMailboxAddress, to);
                        if (errorData != null) { // if this is not the case then the mailbox server returned something not defined in dmtp
                            if (errorData.isEmpty()) { // this means it was successful
                                if (this.datagramSocket != null) {
                                    this.sendToMonitoring(email);
                                }
                            } else { // this means there was an error response at from or send
                                this.forwardEmailToMailbox(errorMail, errorReceiverMailboxAddress, Arrays.toString(errorMail.getTo()));
                                if (this.datagramSocket != null) {
                                    this.sendToMonitoring(errorMail);
                                }
                            }
                        }
                    } else { // this means there was an error while finding the mailbox
                        try {
                            errorReceiverMailboxAddress = getReceiverMailboxAddress(errorToDomain);
                            alreadySendDomains.put(errorToDomain, errorReceiverMailboxAddress);
                        } catch (RemoteException e) {
                            System.err.println("Remote operation failed: " + e.getMessage());
                            quit();
                            continue;
                        } catch (InvalidDomainException e) {
                            System.err.println(e.getMessage());
                        }
                        if (errorReceiverMailboxAddress != null) {
                            this.forwardEmailToMailbox(errorMail, errorReceiverMailboxAddress, Arrays.toString(errorMail.getTo()));
                            if (this.datagramSocket != null) {
                                this.sendToMonitoring(errorMail);
                            }
                        }

                    }
                }
            }
        } catch (InterruptedException e) {
            // only gts here when take() is interrupted
            System.out.println("InterruptedException while waiting for emailQueue: " + e.getMessage());
            Thread.currentThread().interrupt();  // Restore the interrupted status
        }
    }

    private String getReceiverMailboxAddress(String domain) throws RemoteException, InvalidDomainException {
        String lookupDomain = domain;
        INameserverRemote nameserverRemote = rootNameserver;
        while (lookupDomain.contains(".")) {
            int lastDotIndex = lookupDomain.indexOf(".");
            nameserverRemote = nameserverRemote.getNameserver(lookupDomain.substring(lastDotIndex + 1));
            if (nameserverRemote == null) {
                throw new InvalidDomainException("Nameserver with subdomain " + lookupDomain + " not found");
            }
            lookupDomain = lookupDomain.substring(0, lastDotIndex);
        }
        String receiverMailboxAddress = nameserverRemote.lookup(lookupDomain);
        if (receiverMailboxAddress == null) {
            throw new InvalidDomainException("Mailbox with domain " + domain + " not found");
        }
        return receiverMailboxAddress;
    }

    private String forwardEmailToMailbox(Email email, String mailServerAddress, String to) {
        if (mailServerAddress == null) {
            System.out.println("DMTPSEND mailserver==null!!");
            return null;
        }

        String[] splitAddress = mailServerAddress.split(":");
        String errMsg = "";

        try (
                Socket mailServer = new Socket(splitAddress[0], Integer.parseInt(splitAddress[1]));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
                PrintWriter printWriter = new PrintWriter(mailServer.getOutputStream(), true)
        ) {
            String res;

            String line = bufferedReader.readLine();
            if (!line.equals("ok DMTP")) {
                return null;
            }

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

            printWriter.println("hash " + email.getHash());
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                return null;
            }

            printWriter.println("send");
            res = bufferedReader.readLine();
            if (!res.equals("ok")) {
                if (res.startsWith("error")) {
                    errMsg += res;
                } else {
                    return null;
                }
            }
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



    public void quit() {
        this.quit = true;
    }
}
