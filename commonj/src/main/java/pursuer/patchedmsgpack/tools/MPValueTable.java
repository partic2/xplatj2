package pursuer.patchedmsgpack.tools;

import java.util.ArrayList;

import pursuer.patchedmsgpack.value.ArrayValue;
import pursuer.patchedmsgpack.value.Value;
import pursuer.patchedmsgpack.value.ValueFactory;

public class MPValueTable{
	public Value[] header=new Value[] {};
	public ArrayList<ArrayValue> rows=new ArrayList<ArrayValue>();
	public MPValueTable header(Value[] header) {
		this.header=header;
		return this;
	}
	public MPValueTable header(String[] strHeader){
		this.header=new Value[strHeader.length];
		for(int i=0;i<strHeader.length;i++){
			header[i]=ValueFactory.newString(strHeader[i]);
		}
		return this;
	}
	public MPValueTable addRow(ArrayValue row){
		this.rows.add(row);
		return this;
	}
	public Value toValue(){
		return new MapBuilder2()
				.put("header",ValueFactory.newArray(this.header))
				.put("rows",ValueFactory.newArray(rows)).build();
	}
}
