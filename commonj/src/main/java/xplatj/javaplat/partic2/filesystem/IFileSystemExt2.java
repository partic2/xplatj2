package xplatj.javaplat.partic2.filesystem;

public interface IFileSystemExt2 extends IFileSystem {
	long lastModified(IFile f);
	boolean setLastModified(IFile f,long time);
}
