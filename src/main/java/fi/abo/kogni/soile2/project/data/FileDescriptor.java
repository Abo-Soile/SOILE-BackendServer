package fi.abo.kogni.soile2.project.data;

public class FileDescriptor {

	private String path;
	private String FileName;

	public FileDescriptor(String path, String fileName) {
		super();
		this.path = path;
		FileName = fileName;
	}
	
	public String getPath() {
		return path;
	}
	public String getFileName() {
		return FileName;
	}

	
}
