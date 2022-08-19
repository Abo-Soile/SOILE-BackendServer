import { ConfigRetriever } from '@vertx/config';
var retriever = ConfigRetriever.create(vertx);
var config = undefined;
try {
  let cload = retriever.getConfig();
  config = await cload;
  console.log('Config Ready!');
} catch (err) {
  console.log('Loading Config failed!')
}

console.log(config)


var port = config.port;
var host = config.host;
var externalPort = config.http_server.externalport;

function swapUrlPort(url, newPort) {
  var uriRegex = /([a-zA-Z+.\-]+):\/\/([^\/]+):([0-9]+)\//;

  return url.replace(uriRegex, "$1://$2:"+externalPort+"/");
}

var templateManager = (function() {
  var templates = [];
  var isLoaded = false;
  var i, sp;

  var debug = config.http_server.debug;
  var folder = config.http_server.template_folder;

  console.log("TEMPLATEFOLDER: " + folder + "DEBUG MODE: " + debug);
  var files = undefined
  try {

    let cfiles = vertx.fileSystem.readDir(folder)
    files = await cfiles
    console.log('Successfully read files!');
  } catch (err) {
    console.log('Failed reading Templates!')
  }
  
  
  for (i = 0; i < files.length; i++) {
    sp = res[i].lastIndexOf("/") + 1;
    if(err) {
      console.log("Error in templatemanager: " + err);
    }

    templates.push(res[i].slice(sp).replace(".html", ""));
  }
  console.log(JSON.stringify(templates));



  return {
    'load_template': function(msg) {
      let templateName = msg.body().templateName
      console.log("Loading template " + templateName);
      vertx.fileSystem.readFile(folder + templateName + ".html", function(err, res) {
        if (!err) {
          var templateContent = res.getString(0, res.length());
          eb.send("dust.compile", {'name': templateName, "source": templateContent}, function(reply) {
            console.log("Loading template " + JSON.stringify(reply));
          });
        } else {
          console.log(err);
        }
      });
    },
    'render_template': function(msg) {
      let templateName = msg.body().templateName;
      let data = msg.body().data;      
      data.URI = String(msg.body().ExternalURI);
      data.URI = data.URI.split("?")[0]
      data.URI = swapUrlPort(data.URI, externalPort);

      data.URLQUERY = request.query()

      //console.log(JSON.stringify(data));
      // Reloading templates adds about 500ms to pageload and should be avoided in production
      if (!isLoaded || debug) {
        this.load_template(templateName);
        vertx.setTimer(500, function() {
          eb.send("dust.render", {"name": templateName, "context": data}, function(reply) {
            request.response.end(reply.output);
          });
        });
      }else{
        eb.send("dust.render", {"name":templateName, "context":data}, function(reply) {
            request.response.end(reply.output);
            });
      }

    },
    'loadAll':function(){
      var i;
      if(!isLoaded||DEBUG){
        for(i=0; i<templates.length;i++){
          this.load_template(templates[i]);
        }
        isLoaded = true;
      }
    }
  };

});

var manager = templateManager();

eb = vertx.eventBus;

eb.consumer('template.loadAll').handler(manager.loadAll);
eb.consumer('template.render_template').handler(manager.render_template);
eb.consumer('template.load_template').handler(manager.load_template);


