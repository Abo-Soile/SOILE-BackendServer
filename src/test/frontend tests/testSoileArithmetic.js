describe("Builtin plus +", function() {

  it("Adds numbers", function() {
    var intInt = SOILE2.bin.plus(4,4);
    expect(intInt).toBe(4+4);
  });

  it("Adds strings with numbers", function() {
    var stringString = SOILE2.bin.plus("4","4");
    var intString = SOILE2.bin.plus(4,"4");
    var stringInt = SOILE2.bin.plus("4",4);

    expect(stringString).toBe(8);
    expect(intString).toBe(8);
    expect(stringInt).toBe(8);
  });

  it("Doesn't fail on invalid input", function() {
    var na = SOILE2.bin.plus("fdsgds", "3242");
    expect(na).toEqual(NaN);
  });

  it("works with many arguments", function(){
    var num = SOILE2.bin.plus(1,2,3,4,5,6,7,8);
    var num2 = SOILE2.bin.plus(1,2,"3",4,"5",6,7,"8");
    expect(num).toBe(1+2+3+4+5+6+7+8);
    expect(num2).toBe(1+2+3+4+5+6+7+8);
  });
});

describe("Builtin minus -", function() {

  it("subtracts numbers", function() {
    var num = SOILE2.bin.minus(4,3);
    var num2 = SOILE2.bin.minus(3,4);

    expect(num).toBe(1);
    expect(num2).toBe(-1);
  });

  it("subtracts strings with numbers", function() {
    var stringString = SOILE2.bin.minus("4","3");
    var intString = SOILE2.bin.minus(4,"3");
    var stringInt = SOILE2.bin.minus("4",3);

    expect(stringString).toBe(1);
    expect(intString).toBe(1);
    expect(stringInt).toBe(1);
  });

  it("Doesn't fail on invalid input", function() {
    var na = SOILE2.bin.minus("fdsgds", "3242");
    expect(na).toEqual(NaN);
  });

  it("works with many arguments", function(){
    var num = SOILE2.bin.minus(50,2,3,4,5,6,7,8);
    var num2 = SOILE2.bin.minus(50,2,"3",4,"5",6,7,"8");
    expect(num).toBe(50-2-3-4-5-6-7-8);
    expect(num2).toBe(50-2-3-4-5-6-7-8);
  });
});

describe("Builtin multiply *", function() {

  it("multiplies numbers", function() {
    var intInt = SOILE2.bin.multiply(4,4);
    expect(intInt).toBe(4*4);
  });

  it("multiplies strings with numbers", function() {
    var stringString = SOILE2.bin.multiply("4","4");
    var intString = SOILE2.bin.multiply(4,"4");
    var stringInt = SOILE2.bin.multiply("4",4);

    expect(stringString).toBe(16);
    expect(intString).toBe(16);
    expect(stringInt).toBe(16);
  });

  it("Doesn't fail on invalid input", function() {
    var na = SOILE2.bin.multiply("fdsgds", "3242");
    expect(na).toEqual(NaN);
  });

  it("works with many arguments", function(){
    var num = SOILE2.bin.multiply(1,2,3,4,5,6,7,8);
    var num2 = SOILE2.bin.multiply(1,2,"3",4,"5",6,7,"8");
    expect(num).toBe(1*2*3*4*5*6*7*8);
    expect(num2).toBe(1*2*3*4*5*6*7*8);
  });
});

describe("Builtin division /", function() {

  it("divides numbers", function() {
    var intInt = SOILE2.bin.divide(4,2);
    expect(intInt).toBe(4/2);
  });

  it("divides strings with numbers", function() {
    var stringString = SOILE2.bin.divide("8","4");
    var intString = SOILE2.bin.divide(8,"4");
    var stringInt = SOILE2.bin.divide("8",4);

    expect(stringString).toBe(2);
    expect(intString).toBe(2);
    expect(stringInt).toBe(2);
  });

  it("Doesn't fail on invalid input", function() {
    var na = SOILE2.bin.multiply("fdsgds", "3242");
    expect(na).toEqual(NaN);
  });
});

describe("Builtin modulo %", function() {

  it("computes modulo", function() {
    var mod1 = SOILE2.bin.modulo(3,8);
    var mod2 = SOILE2.bin.modulo(4,8);

    expect(mod1).toBe(3%8);
    expect(mod2).toBe(4%8);
  });

});
