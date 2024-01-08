package dslab.nameserver;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class NameserverRemote implements INameserverRemote {

    private ConcurrentHashMap<String, INameserverRemote> childNameServers;
    private ConcurrentHashMap<String, String> mailboxServers;

    public NameserverRemote(ConcurrentHashMap<String, INameserverRemote> childNameServers, ConcurrentHashMap<String, String> mailboxServers) {
        this.childNameServers = childNameServers;
        this.mailboxServers = mailboxServers;
    }


    /**
     * Registers a mailbox server with the given address for the given domain. For example, when registering a
     * nameserver for the domain 'earth.planet', the new nameserver first calls the root nameserver with the argument
     * 'earth.planet'. The root nameserver locates the nameserver for 'planet' via its child-nameserver references, and
     * invokes this method with the remainder of the domain (i.e., 'earth'). Because 'earth' is then the leaf zone, the
     * current nameserver ('planet') stores the reference in its child-nameserver references.
     *
     * @param domain     the domain
     * @param nameserver the nameserver's remote object
     * @throws RemoteException            RMI exception (declaration required by RMI)
     * @throws AlreadyRegisteredException if the given domain is already registered
     * @throws InvalidDomainException     if the domain is invalid (e.g., due to a syntax error, or a required intermediary
     *                                    nameserver was not found)
     */
    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        validateDomain(domain);

        int lastDotIndex = domain.lastIndexOf(".");
        if (lastDotIndex != -1) {
            String nextZone = domain.substring(lastDotIndex + 1);
            String nextDomain = domain.substring(0, lastDotIndex);

            validateDomainParts(nextZone, nextDomain);
            childNameServers.get(nextZone).registerNameserver(nextDomain, nameserver);
        } else {
            if (childNameServers.containsKey(domain)) {
                throw new AlreadyRegisteredException("Domain is already registered");
            }
            childNameServers.put(domain, nameserver);
        }
    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        validateDomain(domain);

        int lastDotIndex = domain.lastIndexOf(".");
        if (lastDotIndex != -1) {
            String nextZone = domain.substring(lastDotIndex + 1);
            String nextDomain = domain.substring(0, lastDotIndex);

            validateDomainParts(nextZone, nextDomain);
            childNameServers.get(nextZone).registerMailboxServer(nextDomain, address);
        } else {
            if (mailboxServers.containsKey(domain)) {
                throw new AlreadyRegisteredException("Mailbox is already registered");
            }
            mailboxServers.put(domain, address);
        }
    }

    private void validateDomain(String domain) throws InvalidDomainException {
        if (domain.matches(".*\\s+.*")) {
            throw new InvalidDomainException("Domain can not contain whitespace"); // TODO maybe add more
        }
    }

    private void validateDomainParts(String nextZone, String nextDomain) throws InvalidDomainException {
        if (nextZone.isBlank() || nextDomain.isBlank()) {
            throw new InvalidDomainException("Domain must contain characters between dots");
        }

        if (!childNameServers.containsKey(nextZone)) {
            throw new InvalidDomainException("Intermediary zone " + nextZone + " does not exist");
        }
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        return childNameServers.get(zone);
    }

    @Override
    public String lookup(String domain) throws RemoteException {
        return mailboxServers.get(domain);
    }
}
