package v2;

import Helpers.Utills;
import Info.AuthInfo;
import Info.ConnectInfo;
import Info.GlobalConstants;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class ProxyEntity implements EntityReadable, EntityClosable{

    ByteBuffer buffer;
    Selector selector;
    SocketChannel channel;
    DatagramChannel dns;
    ProxyEntity pair;
    boolean client;

    int interestOps;

    int state;
    AuthInfo authInfo;
    ConnectInfo connectInfo;
    String address;
    int port;

    public ProxyEntity(Selector selector, SocketChannel channel){
        this.selector = selector;
        this.channel = channel;
        buffer = ByteBuffer.allocate(65536);
        state = GlobalConstants.STATE_READY_FOR_AUTH;
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        if (state == GlobalConstants.STATE_READY_FOR_AUTH) {
            System.out.println("[0][0] Initial greeting from client");
            channel.read(buffer);
            authInfo = new AuthInfo();
            authInfo.parse(buffer);
            buffer.clear();

            unregisterOpRead();
            Utills.putResponseForAuth(buffer, authInfo);
            state = GlobalConstants.STATE_READY_FOR_AUTH_RESPONSE;
            registerOpWrite();
            return;
        }

        if (state == GlobalConstants.STATE_READY_FOR_CONNECTION_REQUEST) {
            System.out.println("[1][0] Connection request from client");
            channel.read(buffer);
            connectInfo = new ConnectInfo();
            connectInfo.parse(buffer);
            buffer.clear();

            port = connectInfo.getPortInt();
            if (connectInfo.getAddressType() == GlobalConstants.addressTypeIpv4) {
                System.out.println("[INFO] Address type: IPv4");
                address = connectInfo.getIPv4AddressString();
                establishConnection();
            } else if (connectInfo.getAddressType() == GlobalConstants.addressTypeDomain) {
                System.out.println("[INFO] Address type: domain name");
                System.out.println("[DNS] Creating request to dns server");
                Message message = new Message();
                Header header = message.getHeader();
                header.setOpcode(Opcode.QUERY);
                header.setID(1);
                header.setRcode(Rcode.NOERROR);
                header.setFlag(Flags.RD);
                message.addRecord(Record.newRecord(new Name(connectInfo.getDomainNameString() + "."), Type.A, DClass.IN), Section.QUESTION);
                byte[] data = message.toWire();
                ByteBuffer dnsMessage = ByteBuffer.wrap(data);
                System.out.println("[DNS] Wryting request to dns server");
                dns.write(dnsMessage);
                unregisterOpRead();
            }
        }

        if(state == GlobalConstants.STATE_CONNECTED) {
                System.out.println("Reading data from: " + channel.getRemoteAddress());
                int numBytes = channel.read(pair.buffer);
                if (numBytes == -1) {
                    System.out.println("[INFO] Reading returned -1");
                    unregisterOpRead();
                    key.cancel();
                    channel.close();
                    if(client)dns.close();
                    pair.channel.close();
                    return;
                }
                if (!pair.buffer.hasRemaining()) unregisterOpRead();
                pair.registerOpWrite();
        }


    }

    public void write(SelectionKey key) throws IOException {
        buffer.flip();
        SocketChannel channel = (SocketChannel) key.channel();

        if(state == GlobalConstants.STATE_READY_FOR_AUTH_RESPONSE){
            System.out.println("[0][1]   Wryting response for the client's greeting");
            System.out.println(channel.getRemoteAddress());
            channel.write(buffer);
            unregisterOpWrite();
            regiserOpRead();
            buffer.compact();

            state = GlobalConstants.STATE_READY_FOR_CONNECTION_REQUEST;
            return;
        }


        if(state == GlobalConstants.STATE_READY_FOR_CONNECTION_RESPONSE){
            System.out.println("[1][1] Writing response for the client's connection request");
            channel.write(buffer);
            unregisterOpWrite();
            regiserOpRead();
            state = GlobalConstants.STATE_CONNECTED;
            buffer.compact();
            return;
        }

        if(state == GlobalConstants.STATE_CONNECTED) {
            System.out.println("Wryting data to: " + channel.getRemoteAddress());
            int numbytes = channel.write(buffer);
            if(numbytes==-1){
                if(!client){
                    System.out.println("[INFO] Writing to proxy returned -1");
                    pair.state = GlobalConstants.STATE_READY_FOR_CONNECTION_REQUEST;
                    key.cancel();
                    channel.close();
                } else {
                    System.out.println("[INFO] Writing to client returned -1");
                }
            }
            if(!buffer.hasRemaining())unregisterOpWrite();
            regiserOpRead();
        }
        buffer.compact();


    }

    public void close(SelectionKey key) throws IOException {
        key.channel().close();
        key.cancel();
        dns.close();
    }

    public void establishConnection() throws IOException {
        SocketChannel rchannel = SocketChannel.open();
        rchannel.configureBlocking(false);
        rchannel.connect(new InetSocketAddress(address, port));
        ProxyEntity proxyEntity = new ProxyEntity(selector, rchannel);
        proxyEntity.interestOps = SelectionKey.OP_CONNECT;
        proxyEntity.pair = this;
        proxyEntity.client = false;
        this.pair = proxyEntity;
        rchannel.register(selector, SelectionKey.OP_CONNECT, proxyEntity);
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


}
