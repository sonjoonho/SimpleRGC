# Simple Colocalization

[![Build Status](https://travis-ci.com/sonjoonho/simple-colocalization.svg?token=qFf5VdpqfSMd2gygFDZQ&branch=master)](https://travis-ci.com/sonjoonho/simple-colocalization)

## Overview

This repository contains the source code for **SimpleColocalization** - an ImageJ plugin that provides tools for cell counting, segmentation, and percentage colocalization calculation. 

It was originally developed in collaboration with scientists at [Cambridge Neuroscience](https://www.neuroscience.cam.ac.uk/) for the third year software engineering project at Imperial College London.

## Installation and Usage

The plugin can be added via the [update site](https://imagej.net/Update_Sites): 
```
https://sites.imagej.net/Sonjoonho/
```
This will install the plugin itself, along with the Kotlin standard library which it depends upon. You must have the **Fiji** and **Java 8** update sites enabled in order for this plugin to work. If you are using Fiji, these should be enabled by default, so don't worry.

Alternatively, the `.jar` can be compiled using Maven and installed in the `jars/`
 directory under your ImageJ/Fiji installation. However, this requires manual installation of dependencies.
 
After installing, the plugin can be found in the ImageJ menu under `Plugins > Simple Cells`.

At this point, you might ask "why is it under 'Simple Cells' if the plugin is called 'SimpleColocalization'?". I'm not sure.

## Caveats
- Make sure your project is configured to JDK 1.8.
- If you get an error like `attempted duplicate class definition: "ij/ImagePlus"`, a workaround is to add the line

  ```-javaagent:/home/joon/.m2/repository/net/imagej/ij1-patcher/0.12.9/ij1-patcher-0.12.9.jar=init```

  to your JVM arguments - the exact path will be given in the error message.
- If you get a error to do with the `CellSegmentationService` not being in the `Context`, this can often be solved by running `mvn clean` before re-building.

## Details

We're still working on this bit.

## Status

SimpleColocalization is still under development, and many aspects of it are subject to change. Pull Requests and Issues are welcomed. 
