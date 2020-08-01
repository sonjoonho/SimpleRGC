#!/usr/bin/env sh
set -e

# Define some variables.
export USER="Sonjoonho"
export UPDATE_SITE="Sonjoonho"

export IJ_PATH="$HOME/Fiji.app"
export URL="https://sites.imagej.net/$UPDATE_SITE/"
export IJ_LAUNCHER="$IJ_PATH/ImageJ-linux64"
export PATH="$IJ_PATH:$PATH"

# Install ImageJ.
printf "Installing ImageJ\n"
mkdir -p "$IJ_PATH"/
cd "$HOME"/
wget -q https://downloads.imagej.net/fiji/latest/fiji-linux64.zip
unzip -q fiji-linux64.zip

# Install the package.
printf "Installing the package\n"
cd "$TRAVIS_BUILD_DIR"/
# TODO(#194): Temporary workaround.
mvn deploy:deploy-file -DgroupId=org.renjin -DartifactId=renjin-script-engine -Dversion=0.8.1906 -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=./local-maven-repo/jars/renjin-script-engine-0.8.1906.jar
mvn -q clean install -Dscijava.app.directory="$IJ_PATH" -Dscijava.deleteOtherVersions=true

# Deploy the package
printf "Deploying the plugin\n"
# Add the update site to ImageJ.
$IJ_LAUNCHER --update edit-update-site $UPDATE_SITE $URL "webdav:$USER:$WIKI_UPLOAD_PASS" .
$IJ_LAUNCHER --update update
# Upload to the update site.
yes 1 | $IJ_LAUNCHER --update upload --update-site $UPDATE_SITE --force-shadow --forget-missing-dependencies jars/SimpleRGC.jar
