# see partic2/packageManager/pxseedloaderbuilder for detail config

#If you don't install pxseed-cli yet
#npm i -g @partic2/pxseed-cli

#You can also write your own build_config.json.

pxseed-cli "await (await import('partic2/packageManager/pxseedloaderbuilder')).defaultBuild()"
