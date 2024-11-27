If you want to compile from the source then you need to install gradle 2.14-rc-1 (thats what was used on the lastest official ae2stuff build)

To install gradle
- you download the release online
- extract
- edit system environment variables (windows search brings it up)
- click "environment variables..."
- under system variables click Path and edit
- Browse and click the bin folder of the extracted gradle release

To compile it:
- Open a command prompt
- cd into the folder of the extracted source code for the mod
- type gradle build
- it should output the jar to build -> libs
