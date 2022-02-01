describe("persistant data", function() {

  it("returns null if no variables are set", function() {
    var variable = SOILE2.rt.persistantDataHandler.get();

    expect(variable).toBeNull();

  })

  it("saves variable", function() {
    SOILE2.bin.savevariable("test", 100);

    var variables = SOILE2.rt.persistantDataHandler.get();
    expect(variables.test).toBe(100);
  });

  it("saves and load variable", function() {
    SOILE2.bin.savevariable("test", 10);

    var variable = SOILE2.bin.loadvariable("test");
    expect(variable).toBe(10);
  });

  it("load default value", function() {
    var variable = SOILE2.bin.loadvariable("test", 10);

    expect(variable).toBe(10);
  });

  it("load values from file", function() {
    var variables = {"var1":1, "var2":2};
    SOILE2.rt.persistantDataHandler.set(variables);

    var var1 = SOILE2.bin.loadvariable("var1", 99);
    var var2 = SOILE2.bin.loadvariable("var2", 99);
    var var3 = SOILE2.bin.loadvariable("var3", 99);

    expect(var1).toBe(1);
    expect(var2).toBe(2);
    expect(var3).toBe(99);
  });
});
