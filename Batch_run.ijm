/*
 * Macro template to process multiple images in a folder
 */

// #@ File (label = "Input directory", style = "directory") input
// #@ File (label = "Output directory", style = "directory") output
// #@ bool (label = "Recurse through nested folders") recurse
// #@ String (label = "File suffix", value = ".tif") suffix

// See also Process_Folder.py for a version of this code
// in the Python scripting language.

// create dialogue for input

input = getDirectory("Choose an Input Directory")
output = getDirectory("Choose an Output Directory")
recurse = getBoolean("Apply batch process in nested folders?")
suffix = getString("File suffix to run plugin on:", ".tif")

processFolder(input);

// function to scan folders/subfolders/files to find files with correct suffix
function processFolder(input) {
	list = getFileList(input);
	list = Array.sort(list);
	for (i = 0; i < list.length; i++) {
		if(recurse && File.isDirectory(input + File.separator + list[i]))
			processFolder(input + File.separator + list[i]);
		if(endsWith(list[i], suffix))
			processFile(input, output, list[i]);
	}
}

function processFile(input, output, file) {
	run("Simple Colocalization")
	print("Processing: " + input + File.separator + file);
	print("Saving to: " + output);
}
