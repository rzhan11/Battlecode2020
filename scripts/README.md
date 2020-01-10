# Scripts
## runAll.sh
Runs examplefuncsplayer (red) against kryptonite (blue) on all maps

Redirects output to log.txt

python3 runAll.sh

## checkKeywords.py
Checks the log.txt file for keywords listed in keywords.txt

python3 checkKeywords.py

## createUpload.sh
Creates an uploadable .zip file for a given folder

This .zip file is located at uploads/createdFromScript_[PLAYER_DIRECTORY].zip

./createUpload.sh examplefuncsplayer

## printSenseDirections.py
Prints all of the dx/dy/mag directions that are visible based on sensor radius

python3 printSenseDirections.py 24 > dirs.txt
