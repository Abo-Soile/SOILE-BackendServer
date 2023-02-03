package fi.abo.kogni.soile2.http_server.codeProvider;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;

/**
 * A Provider for compiled code.
 * This will request code from via the eventbus.
 * @author Thomas Pfau
 *
 */
public class CompiledCodeProvider implements CodeProvider {
	
	private TimeStampedMap<GitFile, String> codeMap;	
	private CompiledCodeRetriever codeRetriever;
	public CompiledCodeProvider(String targetAddress, EventBus eb)
	{
		codeRetriever = new CompiledCodeRetriever(eb, targetAddress);
		codeMap = new TimeStampedMap<>(codeRetriever,3600*2); 
	}
	
	@Override
	public Future<String> getCode(GitFile file)
	{
		return codeMap.getData(file);
	}
	
	@Override
	public void cleanUp()
	{
		codeMap.cleanUp();
	}
	
	@Override
	public Future<String> compileCode(String code)
	{
		return codeRetriever.compileCode(code);
	}
}
