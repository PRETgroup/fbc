# Function Blocks Compiler (FBC)


## Table of Contents

- [What is this?](#what-is-this)
- [How to use](#how-to-use)
    - [From Source](#from-source)
    - [Pre-generated File](#pre-generated-file)
    - [Compiler Arguments](#compiler-arguments)
        - [Example Commands](#example-commands)
- [Examples](#examples)
- [Publications](#publications)

## What is this?

This tool was created as a collaboration between multiple PhD students in the [PRETzel Research Group](http://pretzel.ece.auckland.ac.nz/) at The University of Auckland in New Zealand.
It is designed to take IEC 61499 Function Blocks designs and generate executable code from them in a range of output formats.

Development of this tool is no longer active and so the project is provided "as is" where there may be bugs and/or missing features.


## How to use

There are two ways to use the tool: either by compiling from source, or downloading one of the pre-generated JAR files.
The procedure for each of these is outlined below.

### From Source

This tool is written in Java, and uses [Gradle](https://gradle.org/) for its build system.

Using gradle, the program can be simply called from source using `gradle run` and providing arguments to the compiler using the `--args=""` argument of gradle.

To build a new executable JAR file for distribution, run `gradle build`.
This will create a "fat" JAR file (including all dependencies) which will operate the same as the previous approach.

### Pre-generated File

Pre-generated JAR files are available for you to use if you don't want to compile from source.
Simply download the latest release (`fbc.jar`) from the [Releases](https://github.com/PRETgroup/fbc/releases) page on Github.


### Compiler Arguments
```
Usage: java -jar fbc.jar [OPTIONS] FILE
where
  FILE is the input file to be compiled (*.fbt, *.res, *.dev, *.sys), and
  OPTIONS are as follows:
  --ccode             generates C code (default)
  --cpp               generates C++ code
  --strl              generates Esterel code
  --sysj              generates SystemJ code
  --rmc               generates LTS for Roopak's Model Checker
                      Options --ccode, --strl, --sysj, and --rmc are mutually exclusive.
                      The last option to be included is the one that would be in effect.
  --string=n          configures the maximum length for string variables (default n=32)
  --nxt               generates code from a nxtStudio specification
                      Includes handling of HMI, but not Watch/Alarm/Digiscope
  --v                 prints additional informative messages during code generation
  --sort              generates topologically sorted code
  --run               generates C code that is ready-to-run (includes main function)
  --altsim            uses alternative lib for xml parsing for simulation
  --simul:<ipaddr>    generates simulator for C code with sockets
                      ipaddr is the IP addres of the simulator server (default 127.0.0.1)
                      Options --run and --simul mutually exclusive. The last option
                      to be included is the one that would be in effect.
  --platform=<target> generates code for a specific platform target
                      Platforms that are currently supported are:
                      gnu - all GNU build environments (default)
                      gnu-ucos - for uCOS using GNU build tools*
                      iotapps - for IoTapps
                      ttpos - for TTP-OS using TTTech's tools*
                      d3 - for TTP on D3 platform*
                      The last specified platform is the one that would be in effect.
                      *Simulators cannot be generated for these platforms.
  --remote=<address>  remote deployment server address
  --uername=<name>    deployment service username
  --key=<key>         deployment service key
  --nomake            suppress the generation of makefiles
  --L=<dir>           adds the directory <dir> to be searched for function block files and library files
  --O=<dir>           sets the directory <dir> for the output files to be placed
  --lib=<library files>  adds a library file
  --startup=<startup function>  C file which contains the startup() function
  --makeConfig=<JSON make config file> JOSN file which adds to the make arguments
  --header=<header files>  adds a header file
  --version           prints the version of FBC
  --help              prints this help message
```


#### Example Commands

Generate **C code** for the file **INPUT.fbt** and store it in **output_dir**:

`java -jar fbc.jar --ccode -O=output_dir/ INPUT.fbt`


## Examples (Coming Soon)

Several examples are available under the `examples` directory.
Each of these should be immediately compilable through the tool and range from simple networks to larger ones.