## Good version

import bpy
import csv
import

filepath = "c:/users/andyc/models/monkey3.csv"
object_name = "Cube"  # Change this to match your object name

obj = bpy.data.objects[object_name]

# Ensure we're in object mode
bpy.ops.object.mode_set(mode='OBJECT')

depsgraph = bpy.context.evaluated_depsgraph_get()
eval_obj = obj.evaluated_get(depsgraph)
mesh = eval_obj.to_mesh()


with open(filepath, mode='w', newline='') as file:

  # Loop through faces
  for face in mesh.polygons:
    file.write(f"{len(face.vertices)}")
    for loop_index in face.loop_indices:
      loop = mesh.loops[loop_index]
      vertex = mesh.vertices[loop.vertex_index]
      coord = (vertex.co.x, vertex.co.y, vertex.co.z) 
      file.write(f",{coord[0]:.6f},{coord[1]:.6f},{coord[2]:.6f}")
    file.write("\n")