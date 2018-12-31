package Info;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConnectInfo {

    byte socksVersion;
    byte commandCode;
    byte reserved;
    byte addressType;
    byte[] IPv4Address;
    byte nameLen;
    byte[] domainName;
    byte[] IPv6Address;
    byte[] portNumber;

    public boolean impossibleRequest = false;

    public String toString(){
        String str = "";
        if(addressType==0x01) for(byte b: IPv4Address) str+= " | " + (b & 0xFF);
        else str += " | " + new String(domainName);
        ByteBuffer buffer = ByteBuffer.wrap(portNumber);
        return "version: " + (socksVersion) + "\ncommand code: " + commandCode + "\naddressType: " + addressType + "\naddress: "
                + str + "\nportNumber: " + buffer.getShort();
    }

    public void parse(ByteBuffer buffer){
        byte[] bytes = buffer.array();
        int cursor = 0;
        socksVersion = (byte) (bytes[cursor]);
        cursor++;
        commandCode = (byte) (bytes[cursor]);
        cursor++;
        reserved = (byte) (bytes[cursor]);
        cursor++;
        addressType = (byte) (bytes[cursor]);
        cursor++;
        IPv4Address = new byte[4];
        IPv6Address = new byte[16];
        switch (addressType){
            case 0x01:
                for(int i = 0; i < 4; i++){
                    IPv4Address[i] = (byte) (bytes[cursor]);
                    cursor++;
                }
                break;
            case 0x03:
                nameLen = bytes[cursor];
                cursor++;
                domainName = new byte[nameLen];
                for(int i = 0; i < nameLen; i++){
                    domainName[i] = (byte) (bytes[cursor]);
                    cursor++;
                }
                break;
            case 0x04:
                for(int i = 0; i < 16; i++){
                    IPv6Address[i] = (byte) (bytes[cursor]);
                    cursor++;
                }
                break;
        }
        portNumber = new byte[2];
        portNumber[0] = (byte) (bytes[cursor]);
        cursor++;
        portNumber[1] = (byte) (bytes[cursor]);
    }

    //region Getters Modified
    public String getIPv4AddressString(){
        int byte0, byte1, byte2, byte3;
        byte0 = IPv4Address[0] & 0xFF;
        byte1 = IPv4Address[1] & 0xFF;
        byte2 = IPv4Address[2] & 0xFF;
        byte3 = IPv4Address[3] & 0xFF;

        String res = byte0+"."+byte1+"."+byte2+"."+byte3;
        return res;
    }

    public String getDomainNameString(){
        String domainName = new String(this.domainName);
        return domainName;
    }

    public int getPortInt(){
        ByteBuffer buffer = ByteBuffer.wrap(portNumber);
        return buffer.getShort();
    }
    //endregion

    //region Getters Pure
    public byte getSocksVersion() {
        return socksVersion;
    }

    public byte getCommandCode() {
        return commandCode;
    }

    public byte getAddressType() {
        return addressType;
    }

    public byte getNameLen() {
        return nameLen;
    }

    public byte[] getDomainName() {
        return domainName;
    }

    public byte[] getIPv4Address() {
        return IPv4Address;
    }

    public byte[] getIPv6Address() {
        return IPv6Address;
    }

    public byte[] getPortNumber() {
        return portNumber;
    }
    //endregion

    //region Setters
    public void setSocksVersion(byte socksVersion) {
        this.socksVersion = socksVersion;
    }

    public void setCommandCode(byte commandCode) {
        this.commandCode = commandCode;
    }

    public void setAddressType(byte addressType) {
        this.addressType = addressType;
    }

    public void setDomainName(byte[] domainName) {
        this.domainName = domainName;
    }

    public void setIPv4Address(byte[] IPv4Address) {
        this.IPv4Address = IPv4Address;
    }

    public void setIPv6Address(byte[] IPv6Address) {
        this.IPv6Address = IPv6Address;
    }

    public void setPortNumber(byte[] portNumber) {
        this.portNumber = portNumber;
    }

    public void setNameLen(byte nameLen) {
        this.nameLen = nameLen;
    }
    //endregion
}
