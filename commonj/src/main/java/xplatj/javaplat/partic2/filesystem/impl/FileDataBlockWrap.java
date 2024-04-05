package xplatj.javaplat.partic2.filesystem.impl;

import java.io.IOException;
import java.io.RandomAccessFile;

import xplatj.javaplat.partic2.io.datablock.FileDataBlock;

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
