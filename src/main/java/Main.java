public class Main {

    public static void main(String[] args){
        String str = "192.168.10.12";
        byte[] arr = Utills.getBytesFromAddress(str);
        for(byte b: arr){
            System.out.println(b & 0xFF);
        }
    }

}
