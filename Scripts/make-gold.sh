#!/usr/bin/env bash

lang=$1
if [ -z "${lang}" ]; then
    echo "Usage: $0 [Java | C++]"
    exit 1
elif [ ${lang} != "Java" -a ${lang} != "C++" ]; then
    echo "Usage: $0 [Java | C++]"
    exit 1
fi

if [ ! -d ${lang}/mold ]; then
    echo "First, you need to make mold"
    exit 1
fi

version=`../Java/DAAEcho --version`
release=${version/DAIDALUSj-/}
goldname=${release/-FormalATM*/}

echo "Making $goldname..."
read -p "Are you sure you want to continue? <y/N> " prompt

if [[ $prompt == "y" || $prompt == "Y" || $prompt == "yes" || $prompt == "Yes" ]]; then
    if [ ! -d gold ]; then
	mkdir gold
    fi
    if [ ! -d gold/${goldname} ]; then
	mkdir gold/${goldname}
    fi
    rsync -avz $lang/mold/ gold/$goldname/
    cd gold; rm -f current; ln -s $goldname current
else
    exit 1
fi
