# Android_ApkStaticAnalyzer
CS 7990 - Wireless App Security class project

This is a Java based Android apk static analyzer which will decompile a given apk and return some information about it

The demo video can be seen at https://youtu.be/qA4Xe2aC23A

IMPORTANT NOTE: The analyzer will filter the analysis to one parent deep. If you want a broader package decompilation, look for StaticAnalyzer#getClassPath and repeat the step "packageName = packageName.substring(0, packageName.lastIndexOf("."));"

Useful setup information:
- Import project as "Maven Project" in IntelliJ
 
![alt text](http://www.justfabcodes.com/projects/apk_analyzer/1_Import.png "Import")

- Set the source package as pictured below
 
![alt text](http://www.justfabcodes.com/projects/apk_analyzer/2_SetSources.png "Set Source")

- "StaticAnalyzer" is the program's entry point. 
- If errors are found after importing the maven dependencies make sure to set up the java project and compilers as Java 8 as seen on the following screenshots
 
![alt text](http://www.justfabcodes.com/projects/apk_analyzer/setup1.png "Setup 1")

![alt text](http://www.justfabcodes.com/projects/apk_analyzer/setup2.png "Setup 2")
