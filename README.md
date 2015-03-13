# What's DummyOptNode
"Opt-transport apps of O3 orchestrator&amp;controller suite"
---
DummyOptNode(DON) is a software that emulates optical nodes with OTN/WDM functions without data plane. DON is connected to RYU-OE, receives Flowmod and shows new cross connection status assigned by Flowmod.

1. DON sets up TCP session with RYU OTN extension, and receives the control command of OpenFlow protocol.
2. DON analyzes the content of flowmod.
3. DON visualizes the ODU XC that is set up by flowmod. 


Environment
--------------------------
OS: Windows7 Professional SP1(x64), Ubuntu desktop 12.04(x64)  
Middleware: Oracle Java VM  jdk1.7.0_51、Maven 3  

(Note) This software works with [OCNRM](https://github.com/o3project/ocnrm), [RYU-OE](https://github.com/o3project/ryu-oe), [PseudoMF](https://github.com/o3project/pseudoMF)



Build
--------------------------

(Note)[OpenflowJ-OTN](https://github.com/o3project/openflowj-otn) is required as the external library.

    $ cd ~
    $ git clone https://github.com/o3project/dummyoptnode.git
    $ cd dummyoptnode
    $ mvn clean install
    $ tar xfvz ./target/dummyoptnode-1.0.0-bin.tar.gz –C ~/
    
Configuration
--------------------------

    $ vi ~/dummyoptnode-1.0.0/config.properties

Set RYU-OE’s host & port for OpenFlow session.

    ofcHostname=127.0.0.1
    ofcPortNumber=6633

Starting DummyOptNode
--------------------------

    $ cd ~/dummyoptnode-1.0.0/
    $ java -jar dummyoptnode-1.0.0.jar

Stopping DummyOptNode
--------------------------

Just close Main GUI window.



