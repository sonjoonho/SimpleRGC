/*
 * Macro template to process multiple images in a folder
 */

input = getDirectory("Choose an Input Directory")
recurse = true //getBoolean("Apply batch process in nested folders?")
suffix = ".lif"

setBatchMode(true);
processFolder(input);

// function to scan folders/subfolders/files to find files with correct suffix
function processFolder(input) {

	list = getFileList(input);

	for (i = 0; i < list.length; i++) {

		file = input + list[i];

		if (recurse && endsWith(file, "/")) {
		    processFolder(file);
		} else if (endsWith(list[i], suffix)) {
			fixSpaces(file);
		    processFile(file);
		}
	}
}

function processFile(file) {
	// Following fails if filepath has spaces in it:
	run("Bio-Formats Importer", "open=" + file + " color_mode=Composite open_all_series view=Hyperstack stack_order=XYCZT");
    // run("Bio-Formats Exporter", "save=” + file + “.tif export compression=Uncompressed");
}