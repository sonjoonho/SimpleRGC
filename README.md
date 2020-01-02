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

## Details

We're still working on this bit.

## Status

SimpleColocalization is still under development, and many aspects of it are subject to change. Pull Requests and Issues are welcomed. 
