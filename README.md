# Z80-Hacker
Z80 Hacker Disassembler Tool version 0.1

Copyright (c) 2017<br />
Luciano M. Christofoletti<br />
http://christofoletti.com.br<br />

<strong>Disclaimer: </strong>This software, source code and documentation is distributed WITHOUT ANY EXPRESS OR IMPLIED WARRANTY,<br />
INCLUDING OF MERCHANTABILITY, SATISFACTORY QUALITY AND FITNESS FOR A PARTICULAR PURPOSE.

<strong>Purpose</strong><br />
<br />
This tool allows you to disassemble the binary data from a file into a compilable Z80 source code. The generated code may be compiled using the most compilers/cross compilers avaiable in the net (such as SJASMPLUS, GLASS, among others).
<br />
<br />
<strong>Usage</strong><br />
<br />
Download the executable jar file and the batch file. Put these files in a folder that is in your path.<br />
To see a help message, type the command line:<br />
<br />
<code>$ z80Hacker -h</code>
<br />
<br />
This will show the help message with all available commands. To disassemble a binary file, you will need: the binary file, the project configuration file and the Z80 Hacker disassembler tool.<br />
The option <code>-i</code> can be used to generate a default project configuration file that can be edited with specific parameters for your binary data.<br />
Once you have all the above files at hand, you will be able to start disassembling the binary file. The disassembling process is done starting at a given start point, which must be provided in the project configuration file (at least one must be provided). 



