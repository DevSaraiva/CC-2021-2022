import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranferState {

    private String fileName;
    private int totalBlocks;
    private int actualBlocks;
    private byte[] bytes;


    public  TranferState(String fileName, int totalBlocks){
        this.fileName = fileName;
        this.totalBlocks = totalBlocks;
        this.bytes = new byte[0];
    }

    public String getFileName() {
        return fileName;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getActualBlocks() {
        return actualBlocks;
    }

    public  void increaseBlocks(){
        this.actualBlocks++;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void addBytes(byte[] bytes){
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(this.bytes);
            outputStream.write(bytes);
            byte res[] = outputStream.toByteArray();
            this.bytes = res;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean  isFinished(int actualBlocks){


        System.out.println("actual =" + actualBlocks);
        System.out.println("total =" + this.totalBlocks);

        return this.actualBlocks == this.totalBlocks + 1;
    }

    public String toString(){
        return this.fileName + ' ' + this.actualBlocks;
    }


}



