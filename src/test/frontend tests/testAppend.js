describe("Builtin append", function() {
  var app = "";

  beforeEach(function() {
    app = SOILE2.bin.append;
  });


  it("appends 2 strings", function() {

    console.log(app("rt-",0));

    var twoStrings1 = app("s1", "s2");
    var twoStrings2 = app("s2", "s1");

    expect(twoStrings1).toBe("s1s2");
    expect(twoStrings2).toBe("s2s1");
  });

  it("appends multiple strings", function() {
    console.log("MULTIPLE STRINGS\n\n");

    var fiveStrings = app("s1", "s2", "s3", "s4", "s5");
    var arrayFirst = app([1], 2,5,6,8,3,55);

    var mixedAppend = app(1, "asda", [1,2,3], {a:"a",b:"b"});

    expect(fiveStrings).toBe("s1s2s3s4s5");
    expect(arrayFirst).toEqual([1,2,5,6,8,3,55]);
    expect(mixedAppend).toEqual("1asda1,2,3[object Object]");
  });


  it("appends arrays", function() {
    var arrEmpty = [];
    var arr = ["a", "b", "c"];
    var value = "d";

    app(arrEmpty, arr);
    expect(arr).toContain("a");
    expect(arr).toContain("b");
    expect(arr).toContain("c");
    
    arrEmpty = [];
    app(arrEmpty, value);
    expect(arrEmpty).toContain("d");
  
  });

  it("concateneates two array", function() {
    var arr1 = [1,2,3];
    var arr2 = ["a","b","c"];

    var c = app(arr1, arr2);
    
    expect(arr1).toContain("a");
    expect(arr1).toContain("b");
    expect(arr1).toContain("c");
  });
});