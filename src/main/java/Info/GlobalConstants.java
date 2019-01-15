package Info;

public class GlobalConstants {

    public static final byte noAuth = 0x00;
    public static final byte validSocksVersion = 0x05;
    public static final byte commandTcpIpConnection = 0x01;
    public static final byte addressTypeIpv4 = 0x01;
    public static final byte addressTypeDomain = 0x03;
    public static final byte getAddressTypeIpv6 = 0x04;
    public static final int someBytesMagic = 0xFF;
    public static final int STATE_READY_FOR_AUTH = 0;
    public static final int STATE_READY_FOR_AUTH_RESPONSE = 1;
    public static final int STATE_READY_FOR_CONNECTION_REQUEST = 2;
    public static final int STATE_READY_FOR_CONNECTION_RESPONSE = 3;
    public static final int STATE_CONNECTED = 4;

}
