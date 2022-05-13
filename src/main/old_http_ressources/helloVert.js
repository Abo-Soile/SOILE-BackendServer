vertx.eventbus().consumer("hello.vertx", function(msg){
		msg.reply("Hello Vertx World");
});