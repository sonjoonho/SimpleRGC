/*
 * Macro to process lif images, deconstructiing them and saving each series as a .tif
 * 
 */

input = File.openDialog("Choose a Lif file to process.")

processLif(input);

// Function to open and save lif as series of tifs
function processLif(file) {
	requires("1.43d");
	suffix = ".lif";
	dir = File.directory;
	// Ensure no GUI windows pop up.
	setBatchMode(true);
	if (!endsWith(file, suffix)) {	
		exit("Please choose a file in .lif format to run this plugin");
	}
	// Check that bioformats pllugin is installed.
	List.setCommands;
    if (List.get("Bio-Formats") == "") {
       exit("Bioformats plugin not installed. You need to install this plugin to allow for the opening of .lif files. Please install the plugin and try again. \n Details about the plugin can be found here: https://docs.openmicroscopy.org/bio-formats/5.8.0/users/imagej/");
    }
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