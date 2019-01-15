package Info;

import java.nio.ByteBuffer;
public class AuthInfo {
    byte socksVersion;
    byte numbOfAuthMethods;
    byte[] authMethods;

    public static final byte myValideAuthMethod = GlobalConstants.noAuth;

    public String toString(){
        String str = "";
        for(byte b: authMethods)str+=" | " + b;
        return socksVersion + " | " + numbOfAuthMethods + str;
    }

    public AuthInfo(){

    }

    public AuthInfo(byte socksVersion, byte numbOfAuthMethods, byte[] authMethods){
        this.socksVersion = socksVersion;
        this.numbOfAuthMethods = numbOfAuthMethods;
        this.authMethods = authMethods;
    }

    public void parse(ByteBuffer buffer){
        byte[] bytes = buffer.array();
        socksVersion = bytes[0];
        numbOfAuthMethods = bytes[1];
        authMethods = new byte[numbOfAuthMethods];
        for(int i = 0; i < numbOfAuthMethods; i++){
            authMethods[i] = bytes[2 + i];
        }
    }

    //region Getters
    public byte getSocksVersion() {
        return socksVersion;
    }

    public byte getNumbOfAuthMethods() {
        return numbOfAuthMethods;
    }

    public byte[] getAuthMethods() {
        return authMethods;
    }
    //endregion

    //region Setters
    public void setSocksVersion(byte socksVersion) {
        this.socksVersion = socksVersion;
    }

    public void setNumbOfAuthMethods(byte numbOfAuthMethods) {
        this.numbOfAuthMethods = numbOfAuthMethods;
    }

    public void setAuthMethods(byte[] authMethods) {
        this.authMethods = authMethods;
    }
    //endregion
}
