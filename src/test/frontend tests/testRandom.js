describe("Random number generation", function() {
  var split = "";
  var join = "";


  beforeEach(function() {
    randomnumber = SOILE2.bin.randomnumber;
    randominteger =  SOILE2.bin.randominteger;
  });

  it("generated ints", function() {
    var ran = randominteger(5, 15);

    expect(ran).toBeGreaterThan(4);
    expect(ran).toBeLessThan(16);
  });


  it("generated floats", function() {
    var ran = randomnumber(5, 15);

    expect(ran).toBeGreaterThan(4);
    expect(ran).toBeLessThan(16);
  });

  it("exludes some numbers", function() {
    var ran = 0;

    for (var i = 0; i < 20; i++) {
      ran = randominteger(1,4,[2]);
      ran2 = randominteger(1,4,3);

      expect(ran).not.toBe(2);
      expect(ran2).not.toBe(3);
    }
  });
});