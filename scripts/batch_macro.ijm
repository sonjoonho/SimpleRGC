/*
 * Macro template to process multiple images in a folder
 */

// TODO
// create dialogue for input

input = getDirectory("Choose an Input Directory")
recurse = getBoolean("Apply batch process in nested folders?")
suffix = getString("File suffix to run plugin on:", ".tif")

processFolder(input);

// function to scan folders/subfolders/files to find files with correct suffix
function processFolder(input) {
	list = getFileList(input);
	list = Array.sort(list);
	for (i = 0; i < list.length; i++) {
		if(recurse && File.isDirectory(input + File.separator + list[i])) {
		    processFolder(input + File.separator + list[i]);
		} else {
		    processFile(input, list[i]);
		}
	}
}

// close("*")

function processFile(input, file) {
	print(file);
	open(file);
	// TODO: Run headless somehow.
	// run("Invert");
}