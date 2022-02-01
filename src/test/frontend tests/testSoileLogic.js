
describe("Builtin or", function() {

  it("should return a correct result", function() {
    var t1 = SOILE2.bin.or(true, false); 
    var t2 = SOILE2.bin.or(false, true); 
    var t3 = SOILE2.bin.or(false, false);
    var t4 = SOILE2.bin.or(true, true); 
    
    expect(t1).toBeTruthy();
    expect(t2).toBeTruthy();
    expect(t3).not.toBeTruthy();
    expect(t4).toBeTruthy();
  });

  it("handles multiple arugments", function() {
    var t5 = SOILE2.bin.or(false, false, false, true)
    var t6 = SOILE2.bin.or(false,false,false,false)

    expect(t5).toBeTruthy()
    expect(t6).not.toBeTruthy()
  })
})

describe("Builtin and", function() {

  it("should return a correct result", function() {
    var t1 = SOILE2.bin.and(true, false); 
    var t2 = SOILE2.bin.and(false, true); 
    var t3 = SOILE2.bin.and(false, false);
    var t4 = SOILE2.bin.and(true, true); 

    expect(t1).not.toBeTruthy();
    expect(t2).not.toBeTruthy();
    expect(t3).not.toBeTruthy();
    expect(t4).toBeTruthy();
  });

  it("handles multiple arugments", function() {
    var t5 = SOILE2.bin.and(true, true, true, false)
    var t6 = SOILE2.bin.and(true,true,true,true)

    expect(t5).not.toBeTruthy()
    expect(t6).toBeTruthy()
  })
})

describe("Builtin not", function() {

  it("should return a correct result", function() {
    var t1 = SOILE2.bin.not(true); 
    var t2 = SOILE2.bin.not(false); 

    expect(t1).not.toBeTruthy();
    expect(t2).toBeTruthy();
  });
})