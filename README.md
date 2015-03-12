# What's DummyOptNode
This software works instead of optical network equipment which has OTN/WDM functions without data plane.
DummyOptNode connects to RYU-OE, receive Flowmod, show new cross connection assigned by Flowmod.

Validated Environment
--------------------------
OS: Windows7 Professional SP1(x64), Ubuntu desktop 12.04(x64)

(Notes) This software works with [OCNRM](https://github.com/o3project), [RYU-OE](https://github.com/o3project), [PseudoMF](https://github.com/o3project)

Software Installation
--------------------------
* [Install jdk1.7, maven, and set proxy for maven.]
* [Build OpenflowJ-OE](https://github.com/o3project)

Build & Configuration
--------------------------

    $ mvn install
    $ cd target
    $ tar xfvz dummyoptnode-1.0.0-bin.tar.gz
    $ cd dummyoptnode-1.0.0
    $ vi config.properties

  set IP address and port number of RYU-OE in your environment  as "ofcHostname" and "ofcPortNumber".
ex.

    ofcHostname=127.0.0.1
    ofcPortNumber=6633


You can also modify GUI color and so on by editing "config.properties".
You can change main GUI topology image(img/topology.png) if you needed.




###### Starting DummyOptNode
--------------------------

    $ java -jar dummyoptnode-1.0.0.jar


Stopping DummyOptNode
--------------------------

Just close Main GUI window.



