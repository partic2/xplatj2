package pursuer.patchedmsgpack.tools;

import pursuer.patchedmsgpack.value.ArrayValue;
import pursuer.patchedmsgpack.value.Value;
import pursuer.patchedmsgpack.value.ValueFactory;

import java.util.ArrayList;

public class ArrayBuilder2 {
    public ArrayList<Value> arr=new ArrayList<Value>();
    public ArrayBuilder2 add(Value val) {
        arr.add(val);
        return this;
    }
    public ArrayBuilder2 add(String val){
        add(ValueFactory.newString(val));
        return this;
    }
    public ArrayBuilder2 add(int val){
        add(ValueFactory.newInteger(val));
        return this;
    }
    public ArrayBuilder2 add(long val){
        add(ValueFactory.newInteger(val));
        return this;
    }
    public ArrayBuilder2 add(float val){
        add(ValueFactory.newFloat(val));
        return this;
    }
    public ArrayBuilder2 add(double val){
        add(ValueFactory.newFloat(val));
        return this;
    }
    public ArrayBuilder2 add(byte[] val){
        add(ValueFactory.newBinary(val));
        return this;
    }
    public ArrayBuilder2 add(boolean val){
        add(ValueFactory.newBoolean(val));
        return this;
    }
    public ArrayValue build(){
        return ValueFactory.newArray(arr);
    }
}
