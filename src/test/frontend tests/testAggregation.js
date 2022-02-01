var dataArray = [765,889,845,897,988,914,954,1362,1218,929,815,675,650,601,628,609,779,1950,866,922,884,975,671,644,691,711,675,477,437,547,610,572,683,706,602,506,629,559,584,577,476,516,502,824,584,521,532,601];


describe("Aggragate data functions with stored data", function() {
  beforeEach(function(){
    var data = 2;
    var everySecond = 0;
  
    for (var i = 0; i < 10; i++) {
      SOILE2.bin.storerow("data", data);
      //console.log(data);
      if (everySecond === 1) {
        SOILE2.bin.storerow("test", "daa");
        everySecond = 0;
      }else {
        everySecond = 1;
      }
      SOILE2.bin.newrow();
      data = data*2;
    }

  });

  afterEach(function() {
    SOILE2.util.resetData();
  });

  it("counts rows", function() {
    var a = SOILE2.bin.count("data");
    var b = SOILE2.bin.count("test", "daa");
    expect(a).toBe(10);
    expect(b).toBe(5);
  });

  it("average of rows", function() {
    var a = SOILE2.bin.average("data");
    expect(a).toBe(204.6);
  });

  it("median of rows", function() {
    var a = SOILE2.bin.median("data");
    expect(a).toBe(48);
  });

  it("standarddeviation of rows", function() {
    var a = SOILE2.bin.standarddeviation("data");
    //expect(a).toBeCloseTo(312.97, 1);
    expect(a).toBeCloseTo(329.8970, 1);

  });

  it("standarddeviation of array", function() {
    var st = SOILE2.bin.standarddeviation(dataArray);

    console.log("STANDARDDEVIATION:\t"+ st);

    console.log("average:\t" + SOILE2.bin.average(dataArray));
    var outlier = SOILE2.bin.outliers(dataArray, 3);
    console.log("outlier av:\t" + SOILE2.bin.average(outlier));
    expect(st).toBeCloseTo(263.453, 1);

    expect(SOILE2.bin.standarddeviation([5,6,8,4,8,2,1,6,8])).toBeCloseTo(2.5980,1);


  });

  it("removes outliers of rows", function() {
    var a = SOILE2.bin.outliers("data", 0.5);
    var b = SOILE2.bin.outliers("data", 1);

    expect(a.length).toBe(3);
    expect(b.length).toBe(9);
  });

  it("testRandommapping", function() {
    var randomList =    [0,1,2,3,4,5,6,7,8,9];
    var randomMapping = [0,1,2,3,4,5,6,7,8,9];
    var randomT =       [1,1,0,2,3,3,2,2,1,1];
    var randomGroups =  [0,0,0,0,0,0,0,0,0,0];


    for (var i = 0; i < randomT.length; i++) {
      if (randomT[i]>0) {
        randomGroups[randomT[i]] = 1;
      }
      randomMapping[i] = i;
    }

    console.log(randomMapping);
    console.log(randomT);
    console.log(randomGroups);

    var shuffle =function shuffle(array) {
      var currentIndex = array.length, temporaryValue, randomIndex ;

      // While there remain elements to shuffle...
      while (0 !== currentIndex) {

        // Pick a remaining element...
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex -= 1;

        // And swap it with the current element.
        temporaryValue = array[currentIndex];
        array[currentIndex] = array[randomIndex];
        array[randomIndex] = temporaryValue;
      }
      return array;
    };

    function randomizeGroup(array, groupMapping, groupNo) {
      var tempArr = [];
      for (var i = 0; i < array.length; i++) {
        if(groupMapping[i]===groupNo) {
          tempArr.push(array[i]);
          array[i] = null;
        }
      }
      console.log(array);
      tempArr = shuffle(tempArr);

      for (var i = 0; i < array.length; i++) {
        if(array[i] === null) {
          array[i] = tempArr.pop();
        }
      }

      return array;

    }

    console.log(randomGroups);
    console.log("-------------------");

    for (var i = 0; i < randomGroups.length; i++) {
      if(randomGroups[i]===1) {
        randomMapping = randomizeGroup(randomMapping, randomT, i);
      }
    }


    console.log(randomMapping);


  });

});

