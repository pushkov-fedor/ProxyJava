import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Proxy {

    public static void main(String[] args) throws UnknownHostException {



        int lport = 1080;//Integer.parseInt(args[0]);
        String rhost = "fit.ippolitov.me"; //args[1];
        InetAddress address = InetAddress.getByName(rhost);
        rhost = address.getHostAddress();
        int rport = 80; //Integer.parseInt(args[2]);

        try{
            Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel.socket().bind(new InetSocketAddress("localhost", lport));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(true){
                int num = selector.select();

                if (num == 0) continue;

                Set keysSet = selector.selectedKeys();
                Iterator it = keysSet.iterator();

                while (it.hasNext()){
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    if(key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel client = serverSocketChannel.accept();
                        client.configureBlocking(false);

                        ProxyEntity clientEntity = new ProxyEntity(selector, client, false, false);
                        clientEntity.client = true;
                        client.register(selector, SelectionKey.OP_READ, clientEntity);
                        clientEntity.setInterestOps(client.validOps());
                        System.out.println("Connection is established. Client address: " + client.getRemoteAddress() + new Date());
//region
//                        SocketChannel rchannel = SocketChannel.open(new InetSocketAddress(rhost, rport));
//                        rchannel.configureBlocking(false);
//                        ProxyEntity rchannelEntity = new ProxyEntity(selector, rchannel, false, false);
//                        rchannel.register(selector, SelectionKey.OP_CONNECT, rchannelEntity);
//
//                        clientEntity.setPair(rchannelEntity);
//                        clientEntity.client = true;
//                        rchannelEntity.setPair(clientEntity);
//                        rchannelEntity.client = false;
//                        clientEntity.setInterestOps(client.validOps());
//                        rchannelEntity.setInterestOps(rchannel.validOps());
                        //endregion
                    } else if(key.isConnectable()){
                        System.out.println("[1][0][1]   Connecting............");
                        SocketChannel rchannel = (SocketChannel) key.channel();
                        ProxyEntity entity = (ProxyEntity) key.attachment();
                        if(!rchannel.finishConnect()){
                            System.out.println("Cant make connection with " + rchannel.getRemoteAddress());
                            System.out.println("Connection with " + entity.pair.channel.getRemoteAddress() +
                                    "  and  " + rchannel.getRemoteAddress() + " is closed");
                            key.cancel();
                            rchannel.close();

                            Utills.putResponseForImposRequest(entity.pair.getBuffer(), (byte) 0x06);
                            entity.pair.registerOpWrite();
                        }
                        entity.unregisterOpConnect();
                        System.out.println("Connection to " + rchannel.getRemoteAddress() + " has been established successfully");
                        Utills.putResponseForRequest(entity.pair.buffer, entity.pair.connectInfo,
                                rchannel.socket().getInetAddress().getHostAddress(), rchannel.socket().getPort());
                        entity.pair.registerOpWrite();
                    } else if(key.isReadable()){
                        try {
                            ProxyEntity fe = (ProxyEntity) key.attachment();
                            fe.read(key);
                        }catch(IOException e){
                            System.out.println("Disconnection from: " + ((SocketChannel)key.channel()).getRemoteAddress() );
                            key.cancel();
                            key.channel().close();
                        }
                    } else if(key.isWritable()){
                        try {
                            ProxyEntity fe = (ProxyEntity) key.attachment();
                            fe.write(key);
                        }catch (IOException e){
                            System.out.println("Disconnection from: " + ((SocketChannel)key.channel()).getRemoteAddress());
                            key.cancel();
                            key.channel().close();
                        }
                    }
                }
                keysSet.clear();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
