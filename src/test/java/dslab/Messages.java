package dslab;


import java.io.IOException;

/**
 * Messages.
 */
public final class Messages {

    private Messages() {

    }

    /**
     * Sends a DMTP message with the given arguments to the given client. Each response is verified to contain "ok".
     *
     * @param client  the client to send to
     * @param from    dmtp from
     * @param to      dmtp to
     * @param subject dmtp subject
     * @param data    dmtp data
     * @throws IOException propagated IO exceptions
     */
    public static void send(JunitSocketClient client, String from, String to, String subject, String data) throws IOException {
        client.sendAndVerify("begin", "ok");
        client.sendAndVerify("from " + from, "ok");
        client.sendAndVerify("to " + to, "ok");
        client.sendAndVerify("subject " + subject, "ok");
        client.sendAndVerify("data " + data, "ok");
        client.sendAndVerify("send", "ok");
    }

    /**
     * Sends a DMTP message with the given arguments to the given client. Each response is verified to contain "ok".
     *
     * @param client  the client to send to
     * @param from    dmtp from
     * @param to      dmtp to
     * @param subject dmtp subject
     * @param data    dmtp data
     * @param hash    hash
     * @throws IOException propagated IO exceptions
     */
    public static void send(JunitSocketClient client, String from, String to, String subject, String data, String hash) throws IOException {
        client.sendAndVerify("begin", "ok");
        client.sendAndVerify("from " + from, "ok");
        client.sendAndVerify("to " + to, "ok");
        client.sendAndVerify("subject " + subject, "ok");
        client.sendAndVerify("data " + data, "ok");
        client.sendAndVerify("hash " + hash, "ok");
        client.sendAndVerify("send", "ok");
    }
}
