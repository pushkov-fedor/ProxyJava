import org.xbill.DNS.*;

import java.io.IOException;
import java.net.*;

public class Main {

    public static void main(String[] args) throws IOException {

        String dnsServers[] = ResolverConfig.getCurrentConfig().servers();

        try (DatagramSocket socket = new DatagramSocket()) {
            Message message = new Message();
            Header header = message.getHeader();
            header.setOpcode(Opcode.QUERY);
            header.setID(1);
            header.setRcode(Rcode.NOERROR);
            header.setFlag(Flags.RD);
            message.addRecord(Record.newRecord(new Name("www.vk.com."), Type.A, DClass.IN), Section.QUESTION);
            byte[] data = message.toWire();
            DatagramPacket packet = new DatagramPacket(data, data.length, new InetSocketAddress(dnsServers[0], 53));
            socket.send(packet);
            data = new byte[65536];
            packet = new DatagramPacket(data, data.length);
            socket.setSoTimeout(2000);
            socket.receive(packet);
            Message response = new Message(data);
            System.out.println(response.getSectionArray(1)[0]);
            System.out.println(response.getSectionArray(1)[0].rdataToString());
        }
    }

}
