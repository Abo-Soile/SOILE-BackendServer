describe("Fuzzy comparison", function() {
  var fuzzy = ""

  beforeEach(function() {
    fuzzy = SOILE2.bin.fuzzyequal;
  });

  it("distnace between identical strings should be 0", function() {
    var string1 = "aaa";

    var distance = fuzzy(string1, string1);
    expect(distance).toBe(0);
  });

  it("is non case sensitive", function() {
    expect(fuzzy("aaa", "AAA")).toBe(0);
  });

  it("is case sensitive", function() {
    expect(fuzzy("aaa", "AAA", false)).not.toBe(0);
  });

  it("handles undefined stuff", function() {
    expect(fuzzy(0, undefined)).toBe(-1);
  });

});