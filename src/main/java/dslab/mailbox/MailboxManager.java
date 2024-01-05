package dslab.mailbox;

import dslab.util.Email;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MailboxManager extends Thread{

    private BlockingQueue<Email> emailBlockingQueue;
    private ConcurrentHashMap<String, Map<Integer, Email>> userInbox;

    private boolean quit;

    public MailboxManager(BlockingQueue<Email> emailBlockingQueue, ConcurrentHashMap<String, Map<Integer, Email>> userInbox) {
        this.emailBlockingQueue = emailBlockingQueue;
        this.userInbox = userInbox;
        this.quit = false;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && !this.quit) {
            if (!this.emailBlockingQueue.isEmpty()) {
                try {
                    Email email = this.emailBlockingQueue.take();
                    for (String to : email.getServerTos()) {
                        synchronized (this.userInbox) {
                            Map<Integer, Email> emailHashMap = this.userInbox.get(to);
                            if (emailHashMap != null) {
                                int key = this.getHighestKey(emailHashMap) + 1;
                                emailHashMap.put(key, email);
                                userInbox.put(to, emailHashMap);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // should never get here
                }
            }
        }
    }

    private int getHighestKey(Map<Integer, Email> emailHashMap){
        if (emailHashMap != null && !emailHashMap.isEmpty()) {
            List<Integer> keyList = new ArrayList<>(emailHashMap.keySet());
            keyList.sort(Collections.reverseOrder());

            return keyList.get(0);
        }
        return 0;
    }

    public void quit() {
        this.quit = true;
    }
}
