package pursuer.pxprpc;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Serializer2 {
    public ByteBuffer buf;
    public Serializer2 prepareUnserializing(ByteBuffer buf){
        this.buf=buf;
        this.buf.order(ByteOrder.LITTLE_ENDIAN);
        return this;
    }
    public Serializer2 prepareSerializing(int initBufSize){
        this.buf=ByteBuffer.allocate(initBufSize);
        this.buf.order(ByteOrder.LITTLE_ENDIAN);
        return this;
    }
    public Serializer2 putInt(int val){
        buf=Utils.ensureBuffer(buf,4);
        buf.putInt(val);
        return this;
    }
    public Serializer2 putLong(long val){
        buf=Utils.ensureBuffer(buf,8);
        buf.putLong(val);
        return this;
    }
    public Serializer2 putFloat(float val){
        buf=Utils.ensureBuffer(buf,4);
        buf.putFloat(val);
        return this;
    }
    public Serializer2 putDouble(double val){
        buf=Utils.ensureBuffer(buf,8);
        buf.putDouble(val);
        return this;
    }
    public Serializer2 putBytes(byte[] b, int offset, int len){
        if(len>=0xff){
            buf=Utils.ensureBuffer(buf,len+5);
            buf.put((byte)0xff);
            buf.putInt(len);
        }else{
            buf=Utils.ensureBuffer(buf,len+1);
            buf.put((byte)len);
        }
        buf.put(b,offset,len);
        return this;
    }
    public Serializer2 putString(String val){
        byte[] b=val.getBytes(ServerContext.charset);
        putBytes(b,0,b.length);
        return this;
    }

    public ByteBuffer build(){
        Buffer b=buf;
        b.flip();
        return buf;
    }

    public int getInt(){
        return buf.getInt();
    }
    public long getLong(){
        return buf.getLong();
    }
    public float getFloat(){
        return buf.getFloat();
    }
    public double getDouble(){
        return buf.getDouble();
    }
    public byte[] getBytes(){
        int len=buf.get()&0xff;
        if(len==255){
            len=buf.getInt();
        }
        byte[] b = new byte[len];
        buf.get(b);
        return b;
    }
    public String getString(){
        return new String(getBytes(),ServerContext.charset);
    }

}