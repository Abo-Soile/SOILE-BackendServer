describe("Comparisons,", function() {

    if (!Object.assign) {
  Object.defineProperty(Object, 'assign', {
    enumerable: false,
    configurable: true,
    writable: true,
    value: function(target, firstSource) {
      'use strict';
      if (target === undefined || target === null) {
        throw new TypeError('Cannot convert first argument to object');
      }

      var to = Object(target);
      for (var i = 1; i < arguments.length; i++) {
        var nextSource = arguments[i];
        if (nextSource === undefined || nextSource === null) {
          continue;
        }

        var keysArray = Object.keys(Object(nextSource));
        for (var nextIndex = 0, len = keysArray.length; nextIndex < len; nextIndex++) {
          var nextKey = keysArray[nextIndex];
          var desc = Object.getOwnPropertyDescriptor(nextSource, nextKey);
          if (desc !== undefined && desc.enumerable) {
            to[nextKey] = nextSource[nextKey];
          }
        }
      }
      return to;
    }
  });
}


function replacer(key, value) {
    if (key == "sex") {
        return undefined;
    }

    return value;
}

  var Parent = function(p) {
    this.p = p + " trolol";
    var gaga = "gaga"
  }

  Parent.prototype.pa = function() {
    console.log(this.p);
  }

  Parent.prototype.echo = function() {
    console.log("Echo " + this.name);
  }

  Parent.prototype.te = function() {
    console.log(this.test);
    console.log(Taa.test)
  };


  var Person = function(name) {
    this.name = name;
    this.address = "address"
    this.country = "country"
    this.place = "place"
    this.sex = "sexs"
    Parent.call(this, name);
  }
  Person.prototype = new Parent()
  Person.prototype.constructor = Person;

  Person.prototype.intro = function() {
    console.log("Hello, I am " + this.name)
  }

  /*Person.prototype.rset = function() {
    delete this.sexs
  };*/

  function Bird(name) {
    Parent.call(this, name);
    this.name = name

  }
  Bird.prototype = new Parent();
  Bird.prototype.constructor = new Bird;

  Bird.prototype.intro = function() {
    console.log(this.name + " says chirp chirp");
  }

  Bird.prototype.fly = function() {
    console.log("I'm flyyyying");
  }

  Bird.aa= "test"
  Bird.prototype.a = function() {
    console.log("sdsss " + Bird.aa)
  }

  Taa.test = "test"
  function Taa(a) {
    Parent.call(this, a)
    this.t = a;
    this.name = "nothing";
  }

  Taa.prototype = new Parent();
  Taa.prototype.constructor = new Taa

  Taa.prototype.a = function() {
    console.log(this.stat);
  };

  Taa.prototype.setst = function(first_argument) {
    this.stat = first_argument
  };

  Taa.stat = "aaaa"



  it("Hello i am", function() {
    var p = new Person("Joe");
    var b = new Bird("njööpnjööp");

  /*  p.intro();
    b.intro();

    p.pa();
    b.pa();
    
    p.echo();
    b.echo();

    console.log(p instanceof Person);
    console.log(p instanceof Parent);
    console.log(p instanceof Bird);
    console.log(b instanceof Bird);
    console.log("-------------------")

    b.fly();
    b.a();

    taa = new Taa("aaa");

    taa.pa();
    taa.a();

    taa.a();
    console.log("adas" + Taa.stat)

    taa.setst("öööööööööööööööööö")
    taa.a();

    console.log("asda  " + Taa.stat)

    console.log(Taa.test)

    console.log(JSON.stringify(p, replacer));

    var a = {n:"N",m:"M"}
    var b = {a:"A", b:"b"}

    var c = Object.assign(a, b)
    console.log(JSON.stringify(c))

    taa.te();*/

  })

  
})
