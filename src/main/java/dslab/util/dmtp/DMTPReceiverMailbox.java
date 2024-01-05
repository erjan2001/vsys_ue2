package dslab.util.dmtp;

import dslab.mailbox.MailboxManager;
import dslab.util.Email;
import dslab.util.ReceiveUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class DMTPReceiverMailbox extends Thread {

    private Socket clientSocket;
    private BlockingQueue<Email> emailBlockingQueue;
    private  String domain;
    private  Set<String> usernames;
    private  MailboxManager mailboxManager;
    private ReceiveUtils receiveUtils;


    public DMTPReceiverMailbox(Socket clientSocket, BlockingQueue<Email> emailBlockingQueue, String domain, Set<String> usernames, MailboxManager mailboxManager) {
        this.clientSocket = clientSocket;
        this.emailBlockingQueue = emailBlockingQueue;
        this.domain = domain;
        this.usernames = usernames;
        this.mailboxManager = mailboxManager;
        mailboxManager.start();
    }

    @Override
    public void run() {
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(this.clientSocket.getOutputStream(), true);) {

            this.receiveUtils = new ReceiveUtils(printWriter);

            Email email = new Email();

            printWriter.println("ok DMTP");

            boolean fbegin = false;
            boolean fto = false;
            boolean ffrom = false;
            boolean fsubject = false;
            String line = "";

            while ((line = bufferedReader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                String[] parts = line.split(" ");
                int indexOfSpace = line.indexOf(" ");
                if (!fbegin) {
                    fbegin = receiveUtils.handleNoBegin(parts[0]);
                    if(!fbegin){
                        this.shutdown();
                        return;
                    }
                } else {
                    switch (parts[0]) {
                        case "begin":
                            printWriter.println("ok");
                            break;
                        case "to":
                            fto = receiveUtils.handleToCommand(parts[1], this.domain, this.usernames, email);
                            break;
                        case "from":
                            ffrom = receiveUtils.handleFromCommand(parts[1], email);
                            break;
                        case "subject":
                            email.setSubject(line.substring(indexOfSpace + 1));
                            fsubject = true;
                            printWriter.println("ok");
                            break;
                        case "data":
                            email.setData(line.substring(indexOfSpace + 1));
                            printWriter.println("ok");
                            break;
                        case "send":
                            boolean err = receiveUtils.handleSendCommand(fto,
                                    ffrom,
                                    fsubject,
                                    email,
                                    email.getServerTos().size(),
                                    this.emailBlockingQueue);
                            if(!err){
                                email = new Email();
                                ffrom = false;
                                fto = false;
                                fsubject = false;
                            }
                            break;
                        case "quit":
                            printWriter.println("ok bye");
                            this.shutdown();
                            return;
                        default:
                            printWriter.println("error protocol error");
                            this.shutdown();
                            return;
                    }
                }
            }
        }catch (SocketException e){
            //System.out.println("socketexc: DMTPReceiverMail");
        } catch (InterruptedException e) {
            // only gets here if put emailBlockingQueue gets interrupted
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            this.shutdown();
        }
    }

    private void shutdown(){
        this.mailboxManager.quit();
        if(this.clientSocket != null) {
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
