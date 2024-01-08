package dslab.util;

import java.util.HashSet;
import java.util.Set;

public class Email {
    private String to = "";

    private final Set<String> serverTos = new HashSet<>();

    private String from = "";
    private String subject = "";
    private String data = "";

    private String hash = "";

    public Email(){}

    public String getFrom() {
        return this.from;
    }

    public String getData() {
        return this.data;
    }

    public String getSubject() {
        return this.subject;
    }

    public String[] getTo() {
        return this.to.split(",");
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void appendServerTos(String mail) {
        this.serverTos.add(mail);
    }

    public Set<String> getServerTos() {
        return this.serverTos;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        StringBuilder email = new StringBuilder();

        email.append("from ").append(this.from).append("\n\r");
        email.append("to ");
        for (String t : this.getTo()) {
            email.append(t).append(",");
        }
        email = new StringBuilder(email.substring(0, email.length() - 1));
        email.append("\n\r");

        email.append("subject ").append(this.subject).append("\n\r");
        email.append("data ").append(this.data).append("\n\r");
        email.append("hash ").append(this.hash);
        return email.toString();
    }
}
