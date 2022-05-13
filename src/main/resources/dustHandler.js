
var dust = require('dustjs-linkedin');

var eb = vertx.eventBus();

eb.consumer('dust.compile').handler(compileTemplate);
eb.consumer('dust.render').handler(renderTemplate);

var compileTemplate = function(msg) { 
	try {
		m = msg.body();
		m.compiled = dust.compile(m.source, m.name);
		dust.loadSource(m.compiled);
		msg.reply(m);
	} catch (e) {
		msg.fail(e)
	}
}

var renderTemplate = function(msg) {	
	try {		
		m = msg.body();
		dust.render(m.name, m.context, function(err, out) {
		
		if (err) {
			msg.reply({
					error : err
				});
			} else
				msg.reply({
					output : out
				});
		});
	} catch (e) {
		msg.fail(e)
	}
}
