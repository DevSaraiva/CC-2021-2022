import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TranferState {

    private Lock l = new ReentrantLock();
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
        this.l.lock();
        try{
            return fileName;

        }
        finally{
            this.l.unlock();
        }
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getActualBlocks() {
        return actualBlocks;
    }

    public  void increaseBlocks(){
        this.l.lock();
        try{
            this.actualBlocks++;
        }finally {
            this.l.unlock();
        }
    }

    public byte[] getBytes() {
        this.l.lock();
        try{
            return bytes;
        }
        finally{
            this.l.unlock();
        }
    }

    public void addBytes(byte[] bytes){

        this.l.lock();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(this.bytes);
            outputStream.write(bytes);
            byte res[] = outputStream.toByteArray();
            this.bytes = res;
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            this.l.unlock();
        }
    }

    public boolean  isFinished(int actualBlocks){

        Boolean res;
        this.l.lock();

        try{
              res = this.actualBlocks == this.totalBlocks + 1;
        }
        finally {

            this.l.unlock();
        }
        return res;
    }

    public String toString(){
        return this.fileName + ' ' + this.actualBlocks;
    }


}



