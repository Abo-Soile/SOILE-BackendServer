describe("Utils", function() {
  var split = "";
  var join = "";

  var testString = "this is a test string";
  var testArray = testString.split(" ");

  beforeEach(function() {
    split = SOILE2.bin.split;
    join =  SOILE2.bin.join;
  });

  it("splits string", function() {
    var test = split(testString, " ");
    var real = testString.split(" ");

    expect(test[0]).toBe(real[0]);
    expect(test[1]).toBe(real[1]);
    expect(test[2]).toBe(real[2]);
    expect(test[3]).toBe(real[3]);
  });


  it("joins array", function() {

    expect(join(testArray)).toBe(testArray.join());
  });

  it("split to join", function() {
    var first = split(testString);
    var second = split(join(split(testString), " "));
    
    expect(first[0]).toBe(second[0]);
    expect(first[1]).toBe(second[1]);
    expect(first[2]).toBe(second[2]);
    expect(first[3]).toBe(second[3]);
  });
});