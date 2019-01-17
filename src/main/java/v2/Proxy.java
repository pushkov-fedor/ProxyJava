package v2;

import Helpers.Utills;
import Info.GlobalConstants;
import org.xbill.DNS.ResolverConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class Proxy {

    static final int PROXY_PORT = 1080;

    public static void main(String[] args) throws IOException {
        Selector selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress("localhost", PROXY_PORT));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while(selector.select() > -1){
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();
                if(key.isValid()){
                    try{
                        if(key.isAcceptable()){

                            SocketChannel client = ((ServerSocketChannel)key.channel()).accept();
                            client.configureBlocking(false);
                            ProxyEntity proxyEntity = new ProxyEntity(selector, client);
                            proxyEntity.interestOps = SelectionKey.OP_READ;
                            proxyEntity.client = true;
                            client.register(key.selector(), SelectionKey.OP_READ, proxyEntity);

                            String dnsServers[] = ResolverConfig.getCurrentConfig().servers();
                            //somewhere here we have to create DnsEntity
                            DatagramChannel dns = DatagramChannel.open();
                            DnsEntity dnsEntity = new DnsEntity(proxyEntity);
                            dns.configureBlocking(false);
                            dns.connect(new InetSocketAddress(dnsServers[0],53));
                            dns.register(selector, SelectionKey.OP_READ, dnsEntity);

                            proxyEntity.dns = dns;

                        } else if(key.isConnectable()){
                            SocketChannel rchannel = (SocketChannel) key.channel();
                            ProxyEntity proxyEntity = (ProxyEntity) key.attachment();
                            if(rchannel.finishConnect()) {
                                proxyEntity.unregisterOpConnect();
                                System.out.println("Connection to " + rchannel.getRemoteAddress() + " has been established successfully");
                                Utills.putResponseForRequest(proxyEntity.pair.buffer, proxyEntity.pair.connectInfo,
                                        rchannel.socket().getInetAddress().getHostAddress(), rchannel.socket().getPort());
                                proxyEntity.pair.state = GlobalConstants.STATE_READY_FOR_CONNECTION_RESPONSE;
                                proxyEntity.state = GlobalConstants.STATE_CONNECTED;
                                proxyEntity.pair.registerOpWrite();
                            } else {
                                throw new Exception("Couldn't connect to the client's interest");
                            }
                        } else if(key.isReadable()){
                            EntityReadable entity = (EntityReadable) key.attachment();
                            entity.read(key);

                        } else if(key.isWritable()){
                            ProxyEntity pe = (ProxyEntity) key.attachment();
                            pe.write(key);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        EntityClosable pe = (EntityClosable) key.attachment();
                        pe.close(key);
                        System.out.println("[INFO} Socket is closed.");
                    }
                }
            }
        }

        serverChannel.close();
    }


}
