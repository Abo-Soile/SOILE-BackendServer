describe("Range", function() {
  var range = "";

  beforeEach(function() {
    range = SOILE2.bin.range;
  });

  it("selects string from beginning", function() {
    expect(range("abcdefg",3)).toBe("abc");
  });

    it("selects array from beginning", function() {
    expect(range([1,2,3,4,5,6,7,8,9],5).toString()).toBe([1,2,3,4,5].toString());
  });

  it("selects string from beginning", function() {
    expect(range("abcdefg",3, 6)).toBe("def");
  });

  it("selects array from middle", function() {
    expect(range([1,2,3,4,5,6,7,8,9],5,8).toString()).toBe([6,7,8].toString());
  });

  it("returns false if start is out of range, ", function() {
    expect(range("abcdefg",10, 40)).toBe(false);
  });

  it("ned can be bigger than the length of the array", function() {
    expect(range("abcdefg",3, 23)).toBe(range("abcdefg",3, 7));
    expect(range("abcdefg",23)).toBe("abcdefg");
  });

  it("returns false if invalid range", function() {
    expect(range("abcdefg",5, 2)).toBe(false);
    expect(range([1,2,3,4,5,6,7,8,9],10, 3)).toBe(false);
  });

  it("start = end is an invalid range", function() {
    expect(range("abcdefg",2, 2)).toBe(false);
    expect(range([1,2,3,4,5,6,7,8,9],3, 3)).toBe(false);
  });

});