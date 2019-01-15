package v2;

import org.xbill.DNS.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

public class DnsEntity implements EntityReadable{

    ProxyEntity proxyEntity;

    public DnsEntity(ProxyEntity proxyEntity){
        this.proxyEntity = proxyEntity;
    }

    public void read(SelectionKey key) throws IOException {
        System.out.println("[DNS] Reading answer from dns server");
        DatagramChannel dns = (DatagramChannel) key.channel();
        dns.read(proxyEntity.buffer);
        Message response = new Message(proxyEntity.buffer.array());
        proxyEntity.buffer.clear();
        proxyEntity.address = response.getSectionArray(1)[0].rdataToString();
        proxyEntity.establishConnection();
    }

}
