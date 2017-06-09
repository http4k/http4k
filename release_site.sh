#!/usr/bin/env bash

pip install mkdocs
pip install mkdocs-material

TMP=/tmp/http4k.github.io/
rm -rf ${TMP}
git clone git@github.com:http4k/http4k.github.io.git ${TMP}
cp -R docs ${TMP}/
cp -R mkdocs.yml ${TMP}

cd ${TMP}
mkdocs gh-deploy
echo "www.http4k.org" > CNAME
git add CNAME
git commit -am"Adding CNAME"
git push
cd -


<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/http4k-client-okhttp/src/main/kotlin/org/http4k/client/OkHttp.kt"></script>
