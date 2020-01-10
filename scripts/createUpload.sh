#!/bin/sh

cd ..
[ ! -d "uploads" ] && mkdir uploads

if [ "$#" -ne 1 ] || ! [ -d "src/$1" ]; then
	echo "Usage: $0 PLAYER_DIRECTORY" >&2
	exit 1
fi

cd src/$1
zip createdFromScript_$1.zip * -r
mv createdFromScript_$1.zip ../../uploads
echo "Done"
