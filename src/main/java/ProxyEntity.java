import Info.AuthInfo;
import Info.ConnectInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ProxyEntity {

    ByteBuffer buffer;
    Selector selector;
    SocketChannel channel;
    ProxyEntity pair;
    boolean client;
    int interestOps;

    boolean isAuthorized;
    boolean isConnectionEstablished;
    AuthInfo authInfo;
    ConnectInfo connectInfo;

    public ProxyEntity(Selector selector, SocketChannel channel, boolean isAuthorized, boolean isConnectionEstablished) {
        buffer = ByteBuffer.allocate(4096);
        this.selector = selector;
        this.channel = channel;
        this.isAuthorized = isAuthorized;
        this.isConnectionEstablished = isConnectionEstablished;
        interestOps = 0;
    }

    public void setPair(ProxyEntity pair) {
        this.pair = pair;
    }

    public void regiserOpRead() throws ClosedChannelException {
        interestOps |= SelectionKey.OP_READ;
        channel.register(selector, interestOps, this);
    }

    public void unregisterOpRead() throws ClosedChannelException {
        interestOps &= ~SelectionKey.OP_READ;
        channel.register(selector, interestOps, this);
    }

    public void registerOpWrite() throws ClosedChannelException {
        interestOps |= SelectionKey.OP_WRITE;
        channel.register(selector, interestOps, this);
    }

    public void unregisterOpWrite() throws ClosedChannelException {
        interestOps &= ~SelectionKey.OP_WRITE;
        channel.register(selector, interestOps, this);
    }

    public void unregisterOpConnect() throws ClosedChannelException {
        interestOps &= ~SelectionKey.OP_CONNECT;
        channel.register(selector, interestOps, this);
    }

    public void setInterestOps(int interestOps) {
        this.interestOps = interestOps;
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        if(!isAuthorized && client){
            //
            System.out.println("[0][0]  Initial greeting from client");
            System.out.println(channel.getRemoteAddress());
            channel.read(buffer);

            authInfo = new AuthInfo();
            authInfo.parse(buffer);
            buffer.clear();

//            System.out.println(authInfo);

            unregisterOpRead();
            registerOpWrite();
            Utills.putResponseForAuth(buffer, authInfo);
        } else if(!isConnectionEstablished && client){
            System.out.println("[1][0][0]   Connection request from client");
            System.out.println(channel.getRemoteAddress());
            channel.read(buffer);
            connectInfo = new ConnectInfo();
            connectInfo.parse(buffer);
            System.out.println(connectInfo);
            buffer.clear();

            byte commandCode = connectInfo.getCommandCode();
            if(commandCode!=0x01)connectInfo.impossibleRequest = true;
            byte addressType = connectInfo.getAddressType();
            if(addressType==0x04)connectInfo.impossibleRequest = true;
            int port = connectInfo.getPortInt();

            String host;
            if(addressType==0x01) {
                host = connectInfo.getIPv4AddressString();
            }
            else {
                host = InetAddress.getByName(connectInfo.getDomainNameString()).getHostAddress();
            }

            if(connectInfo.impossibleRequest){
                System.out.println("Client's request is impossible");
                Utills.putResponseForImposRequest(buffer, (byte) 0x01);
                unregisterOpRead();
                registerOpWrite();
                return;
            }

            SocketChannel rchannel = SocketChannel.open();
            rchannel.configureBlocking(false);
            rchannel.connect(new InetSocketAddress(host,port));
            ProxyEntity rchannelEntity = new ProxyEntity(selector, rchannel, false, false);
            rchannel.register(selector, SelectionKey.OP_CONNECT, rchannelEntity);

            /////////////Чекнуть норм ли тут написано
            this.setPair(rchannelEntity);
//            this.client = true;
            rchannelEntity.setPair(this);
            rchannelEntity.client = false;
//            this.setInterestOps(channel.validOps());
            rchannelEntity.setInterestOps(rchannel.validOps());

            unregisterOpRead();
        } else {
            System.out.println("Reading data from: " + channel.getRemoteAddress());
            int numBytes = channel.read(pair.buffer);
            System.out.println(new String(pair.buffer.array()).substring(0,200));
            if (client) {
                if (numBytes == -1) {
                    System.out.println("End of input stream had been reached");
                    unregisterOpRead();
                    return;
                }
                pair.registerOpWrite();
                if (!pair.buffer.hasRemaining()) unregisterOpRead();
            } else {
                pair.registerOpWrite();
                if (!pair.buffer.hasRemaining()) unregisterOpRead();
            }
        }
    }

    public void write(SelectionKey key) throws IOException {
        buffer.flip();
        SocketChannel channel = (SocketChannel) key.channel();

        if(!isAuthorized && client){
            System.out.println("[0][1]   Wryting response for the client's greeting");
            System.out.println(channel.getRemoteAddress());
            channel.write(buffer);
            unregisterOpWrite();
            regiserOpRead();
            isAuthorized = true;
        } else if(!isConnectionEstablished && client){
            System.out.println("[1][1][1]   Wreting response for the client's request");
            System.out.println(channel.getRemoteAddress());
            channel.write(buffer);
            unregisterOpWrite();
            regiserOpRead();
            isConnectionEstablished = true;
        } else {
            System.out.println("Wryting data to: " + channel.getRemoteAddress());
            channel.write(buffer);
            if (client) {
                pair.regiserOpRead();
                if (!buffer.hasRemaining()) unregisterOpWrite();
            } else {
                regiserOpRead();
                if (!buffer.hasRemaining()) {
                    unregisterOpWrite();
                }
            }
        }
        buffer.compact();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
