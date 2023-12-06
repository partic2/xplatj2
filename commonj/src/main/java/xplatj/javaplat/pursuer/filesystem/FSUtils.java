package xplatj.javaplat.pursuer.filesystem;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.io.IDataBlock;
import xplatj.javaplat.pursuer.io.stream.DataBlockInputStream;
import xplatj.javaplat.pursuer.io.stream.DataBlockOutputStream;
import xplatj.javaplat.pursuer.io.stream.StreamTransmit;
import xplatj.javaplat.pursuer.util.IFilter;

public class FSUtils {

	public String executableFileRoot = "xplatj/executable";
	public String configFileRoot = "xplatj/config";
	public String guiResourceRoot = "xplatj/gui";
	public String tempFileRoot = "xplatj/temp";

	public IFile scriptFileRoot() {
		return PlatCoreConfig.get().fs.resolve(executableFileRoot + "/script");
	}

	private class AutoCloseReadStream extends DataBlockInputStream {

		public AutoCloseReadStream(IFile f) throws IOException {
			super(f.open());
		}

		@Override
		public void close() throws IOException {
			getDataBlock().free();
			super.close();
		}
	}

	public DataBlockInputStream readFromIFile(IFile file) throws IOException {
		AutoCloseReadStream in;
		in = new AutoCloseReadStream(file);
		return in;
	}

	private class AutoCloseWriteStream extends DataBlockOutputStream {
		public AutoCloseWriteStream(IFile f) throws IOException {
			super(f.open());
		}

		@Override
		public void close() throws IOException {
			getDataBlock().free();
			super.close();
		}
	}

	public DataBlockOutputStream writeToIFile(IFile file) throws IOException {
		return writeToIFile(file,true);
	}

	public DataBlockOutputStream writeToIFile(IFile file,boolean truncateFile) throws IOException {
		AutoCloseWriteStream out;
		if(truncateFile||!file.exists()){
			file.create();
		}
		out = new AutoCloseWriteStream(file);
		return out;
	}

	public void deleteDirectory(IFile f) throws IOException {
		IFileSystem fs = f.getFileSystem();
		Stack<IFile> delFiles = new Stack<IFile>();
		delFiles.push(f);
		for (int i = 0; delFiles.size() > 0 && i < 0x10000000; i++) {
			IFile t = delFiles.peek();
			Iterable<String> ls = t.list();
			if (ls == null) {
				t.delete();
				delFiles.pop();
			} else {
				boolean hasChild = false;
				for (String child : ls) {
					hasChild = true;
					delFiles.push(t.next(child));
				}
				if (!hasChild) {
					t.delete();
					delFiles.pop();
				}
			}
		}
		if (delFiles.size() > 0) {
			throw new IOException("Too deep recursive.");
		}
	}

	public void deleteFileOrDir(IFile f) throws IOException {
		if(f.list()==null){
			f.delete();
		}else{
			deleteDirectory(f);
		}
	}

	private static class FileCopyRecord {
		IFile s;
		IFile d;

		public FileCopyRecord(IFile s, IFile d) {
			this.s = s;
			this.d = d;
		}
	}

	public void copyFile(IFile src, IFile dest) throws IOException {
		DataBlockInputStream i1 = readFromIFile(src);
		DataBlockOutputStream o1 = writeToIFile(dest);
		//limit 4G
		new StreamTransmit(null, i1, o1, 1024*1024*1024*4, 4096, null);
		i1.close();
		o1.close();
	}

	public void copyFileOrDir(IFile src, IFile dest) throws IOException {
		LinkedList<FileCopyRecord> dirs = new LinkedList<FileCopyRecord>();
		Iterable<String> children = src.list();
		if (children != null) {
			dirs.offer(new FileCopyRecord(src, dest));
		} else {
			copyFile(src, dest);
		}
		for (int rp = 0; dirs.size() > 0 && rp < 0x100000; rp++) {
			FileCopyRecord copyRec = dirs.poll();
			IFile thisDir = copyRec.s;
			children = thisDir.list();
			for (String child : children) {
				IFile f = thisDir.next(child);
				if (f.list() != null) {
					dirs.push(new FileCopyRecord(f, copyRec.d.next(child)));
				} else {
					IFile df = copyRec.d.next(child);
					df.create();
					copyFile(f, df);
				}
			}
		}
	}

	public void moveFileOrDir(IFile src,IFile dest,boolean overwrite) throws IOException{
		LinkedList<FileCopyRecord> dirs = new LinkedList<FileCopyRecord>();
		Iterable<String> children = src.list();
		if (children != null && dest.exists()) {
			dirs.offer(new FileCopyRecord(src, dest));
		} else {
			if(dest.exists()&&overwrite){
				dest.delete();
			}
			if(!dest.exists()){
				src.rename(dest.getPath());
			}
		}
		for (int rp = 0; dirs.size() > 0 && rp < 0x100000; rp++) {
			FileCopyRecord copyRec = dirs.poll();
			IFile thisDir = copyRec.s;
			children = thisDir.list();
			for (String child : children) {
				IFile f = thisDir.next(child);
				IFile df = copyRec.d.next(child);
				if (f.list() != null && df.exists()) {
					dirs.push(new FileCopyRecord(f, copyRec.d.next(child)));
				} else {
					if(df.exists()&&overwrite){
						df.delete();
					}
					if(!df.exists()){
						f.rename(df.getPath());
					}
				}
			}
		}
	}

	public long getFileSize(IFile file) throws IOException {
		IDataBlock db = file.open();
		long size = db.size();
		db.free();
		return size;
	}

	public Iterator<IFile> searchInDir(IFile dir,IFilter<String> nameFilter,boolean excludeDirectory){
		LinkedList<IFile> dirs=new LinkedList<IFile>();
		LinkedList<IFile> found=new LinkedList<IFile>();
		dirs.push(dir);
		for(int rp = 0;dirs.size()>0&&rp<10000000;rp++) {
			IFile cdir = dirs.pop();
			Iterable<String> children = cdir.list();
			for(String child:children) {
				IFile childf = cdir.next(child);
				boolean isDir=childf.list()!=null;
				if(isDir) {
					dirs.add(childf);
				}
				if(!(excludeDirectory&&isDir)) {
					if(nameFilter==null||nameFilter.check(child)) {
						found.add(childf);
					}
				}
			}
		}
		return found.iterator();
	}
}
