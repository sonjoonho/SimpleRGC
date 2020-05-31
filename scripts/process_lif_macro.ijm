/*
 * Macro to process LIF images, deconstructing them and saving each series as a .tif
 * Assumes the Bio-Formats plugin is already installed.
 * Takes as argument pipe separated string: LifPath|OutputDirPath
 */

args = getArgument()
argList = split(args, "|")

print(argList[0])
print(argList[1])

processLIF(argList[0], argList[1]);

// Function to open and save LIF as series of TIFFs
function processLIF(inputfile, outputdir) {
	requires("1.43d");
	suffix = ".lif";

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