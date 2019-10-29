// Macro described and explained in Nieuwenhuis, B. (2019). Gene therapy for axon regeneration

// Preamble - modify the image as appropriate\
run("Set Scale...", "distance=0 known=0 pixel=1 unit=pixel");
waitForUser("Draw your area of interest to analyse particles on and press OK. The area of interest must be rectangular.");
// run("Measure");

run("Clear Outside");
run("Duplicate...", "");
setRGBWeights(1, 0, 0);
run("8-bit");

// Here goes the code of what you want to get from the red channel.
setAutoThreshold("Moments dark");
setOption("BlackBackground", false);
run("Convert to Mask");
run("Watershed");
run("Size..."); // Remember to change the size of your image here to match the size of the one that you already have.
run("Make Binary");
run("Analyze Particles...", "size=100-3500 circularity=0-1.00 show=Outlines exclude summarize add");
close();

close();
setRGBWeights(0, 1, 0);
run("8-bit");
run("Size...");
roiManager("show all");
roiManager("select all");
waitForUser("Press OK to continue this macro if the selection displays your desired xyz");
roiManager("measure");

// Here goes the code of what you want to get from the green channel.
// Make sure that at the end the measurement tab is selected.
saveAs("Results");
roiManager("delete");
run("Clear Results");
close();
