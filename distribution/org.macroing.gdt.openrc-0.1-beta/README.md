OpenRC (v. 0.1-beta)
====================
OpenRC (short for Open Ray Caster) is, as its name suggests, an open source program. But it's not just any program. It's a program for rendering 3D scenes using the Ray Casting algorithm in realtime on the GPU.

Because of the name, Open Ray Caster, some of you might believe Ray Casting is the only goal with this project. This is, however, not true. Additional algorithms, such as Whitted Ray Tracing and Path Tracing, are likely to be implemented.

The program runs a portion of its code on the GPU to speed things up, as previously mentioned. It does this using OpenCL, via a Java library called Aparapi. So OpenRC is written exclusively in Java. At least for now.

![alt text](https://github.com/macroing/OpenRC/blob/master/images/OpenRC_8.png "OpenRC")

This program is provided by Macroing.org.

Supported Features
------------------
* The Ray Casting algorithm.
* Shapes such as planes, spheres and triangles.
* Lights such as point lights.
* Textures such as solid- and decal textures.
* Texture mapping such as spherical- and planar triangle texture mapping.
* A simple camera for walking around in the scene.
* Simple materials.
* Occluding shapes create shadows.
* Simple collision detection.
* Simple tone mapping and gamma correction.

**Note** More supported shapes, lights, materials, textures and texture mapping algorithms may very well be added in the future.

Supported Controls
------------------
* Press 'A' to move left.
* Press 'D' to move right.
* Press 'E' to display the current execution mode (GPU or JTP*).
* Press 'ESC' to exit.
* Press 'F' to fire invisible bullets to make the shapes bleed.
* Press 'S' to move backward.
* Press 'T' to toggle between the GPU- and JTP* execution modes.
* Press 'W' to move forward.
* Use your mouse to look around.

**Note** When running with the execution mode JTP*, you may have to press more than once, as it may be very unresponsive, because everything is running in the CPU.

\* JTP stands for Java Thread Pool.

Getting Started
---------------
To clone this repository, build the project and run it, you can type the following in Git Bash. You need Apache Ant though.
```bash
git clone https://github.com/macroing/OpenRC.git
cd OpenRC
ant
cd distribution/org.macroing.gdt.openrc-0.1-beta
java -jar org.macroing.gdt.openrc.jar
```

TODO
----
This list contains some of the features and improvements that are likely to come in the future. The order of the list is not indicative of importance. It's the order I came to think about things to add.
* Add texture mapping to planes.
* Add vertex colors to triangles for shading with gradients.
* Add view frustum culling. It has been started on.
* Add a bounding volume hierarchy (BVH) to the scene.
* Add anti-aliasing.

Dependencies
------------
 - [Java 8](http://www.java.com).
 - [Aparapi](https://github.com/aparapi/aparapi).

Note
----
This library hasn't been released yet. So, even though it says it's version 1.0.0 in all Java source code files, it shouldn't be treated as such. When this library reaches version 1.0.0, it will be tagged and available on the "releases" page.