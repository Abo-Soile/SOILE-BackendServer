describe("Stimuli", function() {

  it("should iterate through all stimuli", function() {
    var stimuli = [1,2,3,4,5,6];
    SOILE2.bin.setstimuli(stimuli);


    expect(SOILE2.rt.stimuli.get()).toEqual(1);
    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(2);
    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(3);
    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(4);
    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(5);

    SOILE2.bin.emptystimuli();
    SOILE2.rt.stimuli.hasmore();
  });

  it("return the same stimuli until hasmore is called", function() {
    var stimuli = [1,2,3,4,5,6];
    SOILE2.bin.setstimuli(stimuli);

    expect(SOILE2.rt.stimuli.get()).toEqual(1);
    expect(SOILE2.rt.stimuli.get()).toEqual(1);
    expect(SOILE2.rt.stimuli.get()).toEqual(1);

    SOILE2.bin.emptystimuli();
    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(null);
  });

  it("add individual stimuli", function() {
    SOILE2.bin.addstimuli(1);
    expect(SOILE2.rt.stimuli.get()).toEqual(1);

    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(null);

    SOILE2.bin.addstimuli(2);
    SOILE2.bin.addstimuli(3);

    expect(SOILE2.rt.stimuli.get()).toEqual(3);
    SOILE2.rt.stimuli.hasmore();
    expect(SOILE2.rt.stimuli.get()).toEqual(2);

    
    SOILE2.bin.emptystimuli();
    SOILE2.rt.stimuli.hasmore();
  });

  it("add stimuli array", function() {
    SOILE2.bin.addstimuli([1,2,3]);
    expect(SOILE2.rt.stimuli.get()).toEqual(3);
    SOILE2.rt.stimuli.hasmore();

    expect(SOILE2.rt.stimuli.get()).toEqual(2);
    SOILE2.rt.stimuli.hasmore();

    expect(SOILE2.rt.stimuli.get()).toEqual(1);
    SOILE2.rt.stimuli.hasmore();

    expect(SOILE2.rt.stimuli.get()).toEqual(null);
  });
});