package xplatj.javaplat.pursuer.filesystem.impl;

import java.io.IOException;
import java.io.RandomAccessFile;

import xplatj.javaplat.pursuer.filesystem.IFile;
import xplatj.javaplat.pursuer.io.datablock.FileDataBlock;

public class FileDataBlockWrap extends FileDataBlock {
	private FileWrap f;

	public FileDataBlockWrap(FileWrap file, RandomAccessFile raf) throws IOException {
		f = file;
		init(raf);
	}
	@Override
	public void free() {
		try {
			f.close();
		} catch (IOException e) {
		}
	}

}
