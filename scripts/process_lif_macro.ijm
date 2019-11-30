/*
 * Macro to process lif images, deconstructiing them and saving each series as a .tif
 * 
 */

input = File.openDialog("Choose a Lif file to process.")

processLif(input);

// Function to open and save lif as series of tifs
function processLif(file) {
	suffix = ".lif"
	// Ensure no GUI windows pop up.
	setBatchMode(true);
	if (!endsWith(file, suffix)) {	
		exit("Please choose a file in .lif format to run this plugin")
	}
	dir = File.directory;
	
	run("Bio-Formats Importer", "open=[" + file + "] color_mode=Composite rois_import=[ROI manager] open_all_series view=Hyperstack stack_order=XYCZT");
	numOpenImgs = nImages;
    for (i=1; i<= numOpenImgs; i++) {
    	// Select window
        selectImage(i);
        // Save image title in array.
        // Doesn't deal with dupicates.
        saveAs("Tiff", dir + getTitle() + ".tif");
    }
}