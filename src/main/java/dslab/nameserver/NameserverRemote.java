package dslab.nameserver;

import java.rmi.RemoteException;

public class NameserverRemote implements INameserverRemote{

    public NameserverRemote() {
    }

    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

    }

    @Override
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        return null;
    }

    @Override
    public String lookup(String domain) throws RemoteException {
        return null;
    }
}
