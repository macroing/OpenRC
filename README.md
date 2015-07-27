GDT OpenCL (v. 0.1-beta)
========================
GDT OpenCL is not a binding for OpenCL. It's a simple program for testing various graphics algorithms using a library called Aparapi, which in turn is a binding for OpenCL.

![alt text](https://github.com/macroing/GDT-OpenCL/blob/master/images/OpenCL_RayCaster_3.png "Ray Caster")

This program is provided by Macroing.org.

Supported Features
------------------
* A very simple Ray Caster implementation.
* Shapes such as planes, spheres and triangles.
* Lights such as point lights.
* Textures such as normal color textures.
* Texture mapping such as spherical texture mapping.

**Note** More supported shapes, lights, textures and texture mapping algorithms may very well be added in the future.

Supported Controls
------------------
* Press 'A' to move left.
* Press 'D' to move right.
* Press 'E' to display the current execution mode (GPU or JTP*).
* Press 'ESC' to exit.
* Press 'S' to move backward.
* Press 'T' to toggle between the GPU- and JTP* execution modes.
* Press 'W' to move forward.

**Note** When running with the execution mode JTP*, you may have to press more than once, as it may be very unresponsive, because everything is running in the CPU.

\* JTP stands for Java Thread Pool.

Getting Started
---------------
To clone this repository, build the project and run it, you can type the following in Git Bash. You need Apache Ant though.
```bash
git clone https://github.com/macroing/GDT-OpenCL.git
cd GDT-OpenCL
ant
cd distribution/org.macroing.gdt.opencl-0.1-beta
java -jar org.macroing.gdt.opencl.jar
```

TODO
----
This list contains some of the features and improvements that are likely to come in the future. The order of the list is not indicative of importance. It's the order I came to think about things to add.
* Better texture management. Currently only one can be used.
* Fix the shading algorithm to take into account occluding shapes.
* Add texture mapping to planes and triangles.
* Add vertex colors to triangles for shading with gradients.

Dependencies
------------
 - [Java 8](http://www.java.com).
 - [Aparapi](https://github.com/aparapi/aparapi).

Note
----
This library hasn't been released yet. So, even though it says it's version 1.0.0 in all Java source code files, it shouldn't be treated as such. When this library reaches version 1.0.0, it will be tagged and available on the "releases" page.