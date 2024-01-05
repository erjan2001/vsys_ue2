package dslab.mailbox;

import dslab.util.Config;
import dslab.util.Email;
import dslab.util.Globals;
import dslab.util.dmtp.DMTPReceiverMailbox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class MailboxDMTPServerThread extends Thread implements IMailboxServer, Globals {

    private final ServerSocket dmtpServer;
    private Set<String> usernames;
    private Config config;
    private ConcurrentHashMap<String, Map<Integer, Email>> userInbox;
    private Socket clientSocket;
    private ThreadPoolExecutor threadPoolExecutor;
    private BlockingQueue<Email> emailBlockingQueue;

    public MailboxDMTPServerThread(ServerSocket dmtpServer, Set<String> usernames, Config config, ConcurrentHashMap<String, Map<Integer, Email>> userInbox) {
        this.dmtpServer = dmtpServer;
        this.usernames = usernames;
        this.config = config;
        this.userInbox = userInbox;
        this.clientSocket = null;
        this.threadPoolExecutor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADPOOL_SIZE);
        this.emailBlockingQueue = new LinkedBlockingQueue<>(CAPACITY_BLOCKING_QUEUE);
    }

    @Override
    public void run() {
        this.threadPoolExecutor.execute(new MailboxManager(this.emailBlockingQueue,
                this.userInbox));
        while (true) {
            try {
                this.clientSocket = this.dmtpServer.accept();
                MailboxManager mailboxManager = new MailboxManager(this.emailBlockingQueue,
                        this.userInbox);
                this.threadPoolExecutor.execute(new DMTPReceiverMailbox(this.clientSocket,
                        this.emailBlockingQueue,
                        this.config.getString("domain"),
                        this.usernames,
                        mailboxManager));
            } catch (SocketException e) {
                this.shutdown();
                break;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            this.threadPoolExecutor.shutdown();
            this.threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);
            if (!this.threadPoolExecutor.isTerminated()) {
                this.threadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // should never get here
        }
    }
}
