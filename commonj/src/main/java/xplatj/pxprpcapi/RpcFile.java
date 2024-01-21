package xplatj.pxprpcapi;

import xplatj.javaplat.pursuer.filesystem.IFile;
import xplatj.javaplat.pursuer.io.IDataBlock;

import java.io.Closeable;
import java.io.IOException;

public class RpcFile implements Closeable {
    public IDataBlock db;
    public IFile fi;
    Process proc;
    @Override
    public void close() throws IOException {
        if(db!=null)db.free();
    }
}
