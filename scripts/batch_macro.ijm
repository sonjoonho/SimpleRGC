/*
 * Macro template to process multiple images in a folder
 */

// TODO
// create dialogue for input

input = getDirectory("Choose an Input Directory")
recurse = getBoolean("Apply batch process in nested folders?")
suffix = getString("File suffix to run plugin on:", ".tif")

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
		    processFile(file);
		}
	}
}

function processFile(file) {
	open(file);
	// TODO: Run headless somehow.
	run("Simple Cell Counter");
}