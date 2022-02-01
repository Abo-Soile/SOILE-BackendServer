describe("Comparisons,", function() {
  var bigNumber = 11111111;
  var smallNumber = 11;

  var bigString = "11111";
  var smallString = "11";

  it("greater than works", function() {
    expect(SOILE2.bin.gt(bigNumber, smallNumber)).toBeTruthy();
    expect(SOILE2.bin.gt(smallNumber, bigNumber)).not.toBeTruthy(); 
  });

  it("greter than with strings", function() {
    expect(SOILE2.bin.gt(bigString,smallString)).toBeTruthy();
    expect(SOILE2.bin.gt(smallString,bigString)).not.toBeTruthy();
  });

  it("less than works", function() {
    expect(SOILE2.bin.lt(smallNumber, bigNumber)).toBeTruthy(); 
    expect(SOILE2.bin.lt(bigNumber, smallNumber)).not.toBeTruthy();
  });

  it("less than with strings", function() {
    expect(SOILE2.bin.lt(smallString,bigString)).toBeTruthy();
    expect(SOILE2.bin.lt(bigString,smallString)).not.toBeTruthy();
  });

  it("equals works", function() {
    expect(SOILE2.bin.eq(smallNumber, smallNumber)).toBeTruthy();
    expect(SOILE2.bin.eq(smallNumber,bigNumber)).not.toBeTruthy();
    expect(SOILE2.bin.eq(bigNumber, smallNumber)).not.toBeTruthy();
  });

  it("equality with different types work", function() {
    var intArray = [3,4,5,6,7,8];
    var stringArray = ["3","4","5","6","7","8"];
    var string = "345678";

    var string22 = "22";
    var integer = 22;

    expect(SOILE2.bin.eq(intArray, stringArray)).toBeTruthy();

    expect(SOILE2.bin.eq(string, intArray)).toBeTruthy();
    expect(SOILE2.bin.eq(intArray, string)).toBeTruthy();

    expect(SOILE2.bin.eq(stringArray, string)).toBeTruthy();
    expect(SOILE2.bin.eq(string, stringArray)).toBeTruthy();

    expect(SOILE2.bin.eq(integer,string22)).toBeTruthy();
  });
});