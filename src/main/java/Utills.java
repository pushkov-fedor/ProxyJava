import Info.AuthInfo;
import Info.ConnectInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utills {

    public static byte[] getBytesFromAddress(String IPv4Address){
        String[] addressSplited = IPv4Address.split("\\.");
        byte[] result = new byte[4];
        for(int i = 0; i < 4; i++){
            int num = Integer.parseInt(addressSplited[i]);
            result[i] = (byte) num;
        }
        return result;
    }

    public static void putResponseForImposRequest(ByteBuffer buffer, byte status){
        //socks version
        buffer.put((byte) 0x05);
        //status
        buffer.put(status);
        //reserved
        buffer.put((byte) 0x00);
        //address type
        buffer.put((byte) 0x01);
        //IPv4 server bound address
        //4 bytes
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x01);
        //server bound port number
        //2 bytes
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x01);
    }

    public static void putResponseForRequest(ByteBuffer buffer, ConnectInfo connectInfo,
                                             String serverBoundAddress, int port){
        //socks version
        buffer.put((byte) 0x05);
        //status
        //0x00 - request granted
        buffer.put((byte) 0x00);
        //reserved
        buffer.put((byte) 0x00);
        //address type
        //0x01 - IPv4
        buffer.put((byte) 0x01);
        //server bound address
        byte[] serverBoundAddressBytes = getBytesFromAddress(serverBoundAddress);
        for(byte b: serverBoundAddressBytes)buffer.put(b);
        //server port;
        ByteBuffer portBuffer = ByteBuffer.allocate(2);
        portBuffer.order(ByteOrder.BIG_ENDIAN);
        portBuffer.putShort((short)port);
        byte[] portBytes = portBuffer.array();
        for(byte b: portBytes) buffer.put(b);
    }

    public static void putResponseForAuth(ByteBuffer buffer, AuthInfo authInfo){
        boolean isValidMethodExist = false;
        byte numberAuthMethods = authInfo.getNumbOfAuthMethods();
        byte[] authMethods = authInfo.getAuthMethods();
        for(int i = 0; i < numberAuthMethods; i++){
            if(authMethods[i]==AuthInfo.myValideAuthMethod){
                isValidMethodExist = true;
                break;
            }
        }
        byte chosenAuthMethod = 0;
        if(isValidMethodExist) chosenAuthMethod = AuthInfo.myValideAuthMethod; else chosenAuthMethod = (byte) 0xFF;
        buffer.put((byte) 0x05);
        buffer.put(chosenAuthMethod);
    }
}
