package dslab.nameserver;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class NameserverRemote implements INameserverRemote {

    private final ConcurrentHashMap<String, INameserverRemote> childNameServers;
    private final ConcurrentHashMap<String, String> mailboxServers;

    public NameserverRemote(ConcurrentHashMap<String, INameserverRemote> childNameServers, ConcurrentHashMap<String, String> mailboxServers) {
        this.childNameServers = childNameServers;
        this.mailboxServers = mailboxServers;
    }

    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        System.out.println("Registering nameserver for (sub-)domain '" + domain + "'");
        validateDomain(domain);

        int lastDotIndex = domain.lastIndexOf(".");
        if (lastDotIndex != -1) {
            String nextZone = domain.substring(lastDotIndex + 1);
            String nextDomain = domain.substring(0, lastDotIndex);
            validateDomainParts(nextZone, nextDomain);

            INameserverRemote childZone;
            synchronized (this) {
                if (!childNameServers.containsKey(nextZone)) {
                    throw new InvalidDomainException("Intermediary zone " + nextZone + " does not exist");
                }
                childZone = childNameServers.get(nextZone);
            }
            childZone.registerNameserver(nextDomain, nameserver);

        } else {
            if (childNameServers.putIfAbsent(domain, nameserver) != null) {
                throw new AlreadyRegisteredException("Mailbox is already registered");
            }
        }

    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        System.out.println("Registering mailbox server for (sub-)domain '" + domain + "'");
        validateDomain(domain);

        int lastDotIndex = domain.lastIndexOf(".");
        if (lastDotIndex != -1) {
            String nextZone = domain.substring(lastDotIndex + 1);
            String nextDomain = domain.substring(0, lastDotIndex);
            validateDomainParts(nextZone, nextDomain);

            synchronized (this) {
                if (!childNameServers.containsKey(nextZone)) {
                    throw new InvalidDomainException("Intermediary zone " + nextZone + " does not exist");
                }
            }
            childNameServers.get(nextZone).registerMailboxServer(nextDomain, address);
        } else {
            if (mailboxServers.putIfAbsent(domain, address) != null) {
                throw new AlreadyRegisteredException("Mailbox is already registered");
            }
        }
    }

    private void validateDomain(String domain) throws InvalidDomainException {
        if (domain.matches(".*\\s+.*")) {
            throw new InvalidDomainException("Domain can not contain whitespace");
        }
    }

    private void validateDomainParts(String nextZone, String nextDomain) throws InvalidDomainException {
        if (nextZone.isBlank() || nextDomain.isBlank()) {
            throw new InvalidDomainException("Domain must contain characters between dots");
        }
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        System.out.println("Nameserver for '" + zone + "' requested by transfer server");
        return childNameServers.get(zone);
    }

    @Override
    public String lookup(String domain) throws RemoteException {
        System.out.println("Mailbox server for '" + domain + "' requested by transfer server");
        return mailboxServers.get(domain);
    }
}
