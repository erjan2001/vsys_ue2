package dslab.util;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public final class ReceiveUtils {

    private final PrintWriter printWriter;
    public ReceiveUtils(PrintWriter printWriter){
        this.printWriter = printWriter;
    }

    public boolean handleSendCommand(boolean fto, boolean ffrom, boolean fsubject, Email email, int length, BlockingQueue<Email> emailBlockingQueue) throws InterruptedException {
        if (!fto) {
            this.printWriter.println("error no recipients");
            return false;
        } else if (!ffrom) {
            System.out.println("!from:" + email);
            this.printWriter.println("error no sender");
            return false;
        } else if (!fsubject) {
            this.printWriter.println("error no subject");
            return false;
        } else {
            if (length > 0) {
                emailBlockingQueue.put(email);
                this.printWriter.println("ok");
            } else {
                this.printWriter.println("error no to");
                return false;
            }
        }
        return true;
    }


    public boolean handleToCommand(String to, String domain, Set<String> usernames, Email email) {
        int length;
        // both null if DMTPReceiverTransfer calls method, therefore no need to check for domains etc
        if(usernames != null && domain != null) {
            length = this.handleFromCommandWithDomainAndUsernames(to, email, domain, usernames);
        } else {
            email.setTo(to);
            length = email.getTo().length;
        }
        if(length >=0) {
            this.printWriter.println("ok " + length);
        }
        return true;
    }

    private int handleFromCommandWithDomainAndUsernames(String to, Email email, String domain, Set<String> usernames) {
        to = to.replace("[", "").replace("]", "");
        String[] tos = to.split(",");
        StringBuilder errorMsg = new StringBuilder("error unknown recipients ");
        boolean error = false;
        for (String singleTo :
                tos) {
            if (singleTo.endsWith(domain)) {
                String name = singleTo.substring(0, singleTo.indexOf("@" + domain));
                if (!usernames.contains(name)) {
                    error = true;
                    errorMsg.append(singleTo).append(", ");
                } else {
                    email.appendServerTos(name);
                }
            }
        }
        email.setTo(to);
        if(error) {
            this.printWriter.println(errorMsg.substring(0, errorMsg.length() - 2));
            return -1;
        }
        return email.getServerTos().size();
    }

    public boolean handleNoBegin(String cmd) {
        switch (cmd) {
            case "quit":
                this.printWriter.println("ok bye");
                 return false;
            case "begin":
                this.printWriter.println("ok");
                return true;
            default:
                this.printWriter.println("error protocol error");
                return false;
        }
    }

    public boolean handleFromCommand(String from, Email email) {
        if(from.split("@").length<2){
            this.printWriter.println("error no email");
            return false;
        } else {
            email.setFrom(from);
            this.printWriter.println("ok");
            return true;
        }
    }
}
