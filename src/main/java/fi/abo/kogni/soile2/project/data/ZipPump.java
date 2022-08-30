package fi.abo.kogni.soile2.project.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.netty.util.concurrent.FailedFuture;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;


public class ZipPump implements ReadStream<Buffer>,Handler<Buffer>{

	Iterator<FileDescriptor> filesToPump;
	PipedOutputStream pos;
	ZipOutputStream zos;
	InputStream pis;
	ReadStream<Buffer> fileInputStream;
	Vertx vertx;
	
	private int state;
	
	
	public static int READING = 0;
	public static int PAUSED = 1;
	public static int CLOSED = 2;
	
	private Handler<Void> endHandler;
	private Handler<Throwable> exceptionHandler;
	private Handler<Buffer> dataHandler;
	
	public ZipPump(Iterator<FileDescriptor> filesToPump, Vertx vertx) throws IOException
	{
		this.vertx = vertx;
		pos = new PipedOutputStream();
		zos = new ZipOutputStream(pos);
		pis = new PipedInputStream(pos, 8192);
		this.filesToPump = filesToPump;
	}	


	public void test(Vertx vertx)
	{
		
	}
	
	
	public void read()
	{
		if(state == READING)
		{
			vertx.executeBlocking(promise -> {
				// Flush regularily to not block the pipe.
                next(this::doFlush);

                // We start by reading the first file
                next(this::readFile);
                promise.complete();
			});
		}
	}
	
	
	private void readFile()
	{		
		if(filesToPump.hasNext())
		{
		
		}
	}
	
	private void readFile(FileDescriptor entry, Handler<AsyncResult<Void>> handler)
	{
		
	}
	/**
	 * Open a File stream for an async file. This sets the fileInputStream to the file indicated by the fileDescriptor
	 * @param file
	 * @param handler
	 * @throws IOException
	 */
	private void openFileStream(FileDescriptor file, Handler<AsyncResult<Void>> handler) throws IOException
	{
		ZipEntry e = new ZipEntry(file.getFileName());
		zos.putNextEntry(e);
		vertx.fileSystem().open(file.getPath(), new OpenOptions().setRead(true)).onSuccess(asyncFile ->{
			fileInputStream = asyncFile;
			vertx.runOnContext(v -> readFile(file, handler));
		}).onFailure(fail -> {
			exceptionHandler.handle(fail);	
		});
	}
	
	public void doFlush()
	{
		try
		{
		if(state == READING)
		{
			if(pis.available() > 0)
			{
				byte[] tmp = new byte[pis.available()];
                int readBytes = pis.read(tmp);
                if (readBytes > 0) {
                    byte[] buffer = new byte[readBytes];
                    System.arraycopy(tmp, 0, buffer, 0, readBytes);
                    dataHandler.handle(Buffer.buffer(buffer));
                }
                next(this::doFlush);
			}
			else
			{
				next(this::doFlush);
			}
		}
		}
		catch(IOException e)
		{
			exceptionHandler.handle(e);
		}
	}
	public void next(Runnable f)
	{
		vertx.getOrCreateContext().runOnContext( v -> f.run());
	}
	
	@Override
	public ReadStream<Buffer> exceptionHandler( Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public ReadStream<Buffer> handler( Handler<Buffer> handler) {
		this.dataHandler = handler;
		read();
		return this;
	}

	@Override
	public ReadStream<Buffer> pause() {
		state = PAUSED;
		return this;
	}

	@Override
	public ReadStream<Buffer> resume() {
		state = READING;
		return this;
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		
		return this;
	}

	@Override
	public ReadStream<Buffer> endHandler( Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}


	@Override
	public void handle(Buffer event) {
		// TODO Auto-generated method stub
		
	}
}
