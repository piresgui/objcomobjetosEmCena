# OBJ Reader - Multiple Objects in a Scene

A small Java/Swing software 3D renderer built for a Computer Graphics course. It loads several Wavefront `.obj` models into a single scene and lets you fly around it with a free camera - no game engine or 3D library involved, just hand-rolled matrix math and `Graphics2D`.

## Features

- Wavefront `.obj` parser (`v` and `f` lines, with fan triangulation for polygons with more than 3 vertices)
- Multiple independent models placed side by side in one scene, each auto-scaled and grounded regardless of the original file's units
- Free-fly camera: move, strafe, go up/down, and look around
- Simple software rasterizer: perspective projection, painter's algorithm depth sorting, per-face "headlight" shading, optional backface culling and wireframe mode

## Controls

| Input | Action |
|---|---|
| `W` / `A` / `S` / `D` | Move forward / left / back / right |
| `Space` / `Shift` | Move up / down |
| Arrow keys | Look around |
| Left mouse button + drag | Look around |
| `C` | Cycle backface culling modes (off / mode A / mode B) |
| `F` | Toggle wireframe / filled rendering |
| `R` | Reset camera |

## Project structure

```
src/
  MainClass.java        entry point (JFrame setup)
  MainCanvas.java        render loop, input handling, rasterizer
  core3d/
    Ponto3D.java          3D point / homogeneous vector
    Mat4x4.java            4x4 matrix (translation, rotation, multiplication)
    Camera.java            free camera + view matrix
  obj/
    ObjLoader.java         .obj file parser
    ObjModel.java           parsed vertices/faces + bounding box
    SceneObject.java        a model instance placed in the scene
```

## Models

The scene is defined in `MainCanvas.carregarCena()` and loads `.obj` files from the repository root:

- `medieval house.obj`
- `chair_01.obj`
- `tank.obj`
- `AIM120D.obj`
- `Bench_LowRes.obj`
- `mig21_fishbed.obj`
- `SR71.obj`
- `x-35_obj.obj`

You can point it at any other `.obj` file by adding an entry (filename, X position, target size) to that list.

## Running

Requires a JDK (tested with JDK 21).

```bash
javac -d bin -encoding UTF-8 $(find src -name "*.java")
java -cp bin MainClass
```

Run it from the repository root, since the `.obj` files are loaded with relative paths.
