
/*
 * Macro to process LIF images, deconstructing them and saving each series as a .tif
 * Assumes the Bio-Formats plugin is already installed.
 */

processLIF();

// Function to open and save LIF as series of TIFFs
function processLIF() {
	requires("1.43d");
	suffix = ".lif";
	inputfile = File.openDialog("Choose a .lif file to process.");
	outputdir = getDirectory();

	// Ensure no GUI windows pop up.
	setBatchMode(true);

	// Check input.
	if (!endsWith(inputfile, suffix)) {
		exit("Please choose a file in .lif format to run this plugin");
	}

	run("Bio-Formats Importer", "open=[" + inputfile + "] color_mode=Composite rois_import=[ROI manager] open_all_series view=Hyperstack stack_order=XYCZT");
	numOpenImgs = nImages;

	for (i=1; i<= numOpenImgs; i++) {
    		// Select window
		selectImage(i);
		// Save image title in array.
		// Doesn't deal with duplicate series.
		saveAs("Tiff", outputdir + getTitle() + ".tif");
	}
}