bl_info = {
    "name": "OpenRC export",
    "author": "Martin Sandgren <carlmartus@gmail.com>",
    "version": (0, 2),
    "blender": (2, 72, 0),
    "description": "Exports the scene to a OpenRC readable format. Exported file will"
        "be placed in same folder as the active blender on disk",
    "warning": "",
    "wiki_url": "https://github.com/macroing/OpenRC/wiki",
    "category": "OpenRC",
}

import bpy, bmesh
from struct import pack

WRITE_FLOAT = '!f'
WRITE_INT = '!i'

def writeFloat(fd, f):
    fd.write(pack(WRITE_FLOAT, f))

def writeInt(fd, i):
    fd.write(pack(WRITE_INT, i))

class DataBlock:
    def __init__(self):
        self.content = []

    def packF(self, f):
        self.content.append((WRITE_FLOAT, f))

    def packI(self, i):
        self.content.append((WRITE_INT, i))

    def write(self, fd):
        for (type, value) in self.content:
            fd.write(pack(type, value))

class Scene:
    def __init__(self, scene):
        self.packsTriangles = []
        self.shapeLength = 0
        self.packsLights = []
        self.packCamera = None
        self.packsMaterials = []
        self.materialOffset = 0

        for obj in scene.objects:
            switch = {
                'MESH' : ('Mesh', self.addObjectMesh),
                'LAMP' : ('Point light', self.addObjectPointLight),
                'CAMERA' : ('Camera', self.addObjectCamera),
            }
            if obj.type in switch:
                description, call = switch[obj.type]
                print('Adding %s "%s"' % (description, obj.name))
                call(obj)


    def addObjectMesh(self, obj):
        matOff = self.materialOffset
        for mat in obj.material_slots:
            m = mat.material

            p = DataBlock()
            p.packF(0.0) # Ambient
            p.packF(0.0)
            p.packF(0.0)
            p.packF(0.0) # Intensity
            p.packF(m.diffuse_color[0]) # Diffuse color
            p.packF(m.diffuse_color[1])
            p.packF(m.diffuse_color[2])
            p.packF(m.diffuse_intensity)
            p.packF(m.specular_color[0]) # Specular color
            p.packF(m.specular_color[1])
            p.packF(m.specular_color[2])
            p.packF(1.0) # Intensity
            p.packF(0.5) # Specular power
            p.packF(0.5) # Refclection
            p.packF(0.0) # Refraction
            p.packF(0.0) # Texture count
            self.addMaterial(p)

        bm = bmesh.new()
        bm.from_mesh(obj.data)
        for loops in bm.calc_tessface():
            normal = loops[0].face.normal
            cos = [loops[i].vert.co for i in range(3)]
            materialId = loops[0].face.material_index

            size = 15
            p = DataBlock()
            p.packF(3.0) # Type
            p.packF(float(size)) # Size
            p.packF(materialId + matOff) # Material # Not impl
            p.packF(cos[0].x) # A_X
            p.packF(cos[0].y) # A_Y
            p.packF(cos[0].z) # A_Z
            p.packF(cos[1].x) # B_X
            p.packF(cos[1].y) # B_Y
            p.packF(cos[1].z) # B_Z
            p.packF(cos[2].x) # C_X
            p.packF(cos[2].y) # C_Y
            p.packF(cos[2].z) # C_Z
            p.packF(normal.x) # Surface_Normal_X
            p.packF(normal.y) # Surface_Normal_Y
            p.packF(normal.z) # Surface_Normal_Z
            self.packsTriangles.append(p)
            self.shapeLength += size

        bm.free()

    def addObjectCamera(self, obj):
        co = obj.location
        mat = obj.matrix_basis
        forw = [mat[i][2] for i in range(3)]
        ups = [mat[i][1] for i in range(3)]
        side = [mat[i][0] for i in range(3)]

        p = DataBlock()
        p.packF(co.x)
        p.packF(co.y)
        p.packF(co.z)
        p.packF(ups[0])
        p.packF(ups[1])
        p.packF(ups[2])
        p.packF(co.x - forw[0])
        p.packF(co.y - forw[1])
        p.packF(co.z - forw[2])
        p.packF(20.0)
        p.packF(1.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        #p.packF(0.0)
        self.packCamera = p

    def addObjectPointLight(self, obj):
        co = obj.location

        p = DataBlock()
        p.packF(1.0)
        p.packF(6.0)
        p.packF(co.x)
        p.packF(co.y)
        p.packF(co.z)
        p.packF(1.0)
        self.packsLights.append(p)

    def addTexture(self, data):
        pass

    def addMaterial(self, mat):
        self.packsMaterials.append(mat)
        self.materialOffset += 1

    def write(self, fd):
        #float[]:    Camera[19]                          ---
        print('Writing')
        self.packCamera.write(fd)

        #float:      Textures_Length                     ---
        #float[]:    Textures[Textures_Length]           ---
        print('No textured')
        writeInt(fd, int(0.0))

        #float:      Materials_Length                    ---
        #float[]:    Materials[Materials_Length]         ---
        print('%d materials' % len(self.packsMaterials))
        writeInt(fd, int(len(self.packsMaterials)))
        for p in self.packsMaterials: p.write(fd)

        #float:      Lights_Length                       ---
        #float[]:    Lights[Lights_Length]               ---
        print('%d lights' % len(self.packsLights))
        writeInt(fd, int(len(self.packsLights)))
        for p in self.packsLights: p.write(fd)

        #float:      Shapes_Length                       ---
        #float[]:    Shapes[Shapes_Length]               ---
        print('%d (%d b) triangles' % (len(self.packsTriangles), self.shapeLength))
        writeInt(fd, int(self.shapeLength))
        for p in self.packsTriangles: p.write(fd)

class ObjectMoveX(bpy.types.Operator):
    """Export OpenRC scene"""
    bl_idname = 'openrc.export'
    bl_label = 'Export OpenRC scene'
    bl_options = {'REGISTER'}

    def execute(self, context):
        export()
        return {'FINISHED'}

def export():
    print('Collecting data...')
    scene = Scene(bpy.context.scene)

    outPath = bpy.data.filepath
    outPath = outPath[:outPath.rfind('.')] + '.osl'

    print('Writing data to file: %s' % outPath)
    fd = open(outPath, 'wb')
    scene.write(fd)
    fd.close()

def register():
    bpy.utils.register_module(__name__)
    
def unregister():
    bpy.utils.unregister_module(__name__)

if __name__ == "__main__":
    print('Main export')
    export()
    #register()

