OpenRC (v. 0.1-beta)
====================
OpenRC is a simple Ray Caster implementation using OpenCL (via Aparapi).

![alt text](https://github.com/macroing/OpenRC/blob/master/images/OpenRC_3.png "OpenRC")

This program is provided by Macroing.org.

Supported Features
------------------
* A very simple Ray Caster implementation.
* Shapes such as planes, spheres and triangles.
* Lights such as point lights.
* Textures such as solid- and decal textures.
* Texture mapping such as spherical texture mapping.
* A simple camera for walking around in the scene.
* Simple materials.

**Note** More supported shapes, lights, materials, textures and texture mapping algorithms may very well be added in the future. The simple camera may be updated to support walking- and looking around like in an FPS-game.

Supported Controls
------------------
* Press 'A' to move left.
* Press 'D' to move right.
* Press 'DOWN ARROW' to look down.
* Press 'E' to display the current execution mode (GPU or JTP*).
* Press 'ESC' to exit.
* Press 'LEFT ARROW' to look left.
* Press 'RIGHT ARROW' to look right.
* Press 'S' to move backward.
* Press 'T' to toggle between the GPU- and JTP* execution modes.
* Press 'UP ARROW' to look up.
* Press 'W' to move forward.

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
* Fix the shading algorithm to take into account occluding shapes.
* Add texture mapping to planes and triangles.
* Add vertex colors to triangles for shading with gradients.
* Add view frustum culling. It has been started on.
* Add scene loading from binary files.
* Add a scene description language to "compile" to the binary files.
* Add a bounding volume hierarchy (BVH) to the scene.
* Add anti-aliasing.
* Fix the camera, so it works like the camera in FPS-games.

Dependencies
------------
 - [Java 8](http://www.java.com).
 - [Aparapi](https://github.com/aparapi/aparapi).

Note
----
This library hasn't been released yet. So, even though it says it's version 1.0.0 in all Java source code files, it shouldn't be treated as such. When this library reaches version 1.0.0, it will be tagged and available on the "releases" page.