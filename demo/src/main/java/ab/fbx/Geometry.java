/*
 * Copyright (C) 2025 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.fbx;

import ab.nbsnk.Obj;
import ab.nbsnk.nodes.Col;
import ab.nbsnk.nodes.Pnt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Geometry {
  public static final Set<String> PROPERTY_KEYS = Set.of("AmbientColor", "DiffuseColor", "DiffuseFactor",
      "TransparencyFactor", "SpecularColor", "ReflectionFactor", "Emissive", "Ambient", "Diffuse", "Specular",
      "Shininess", "Opacity", "Reflectivity", "ShininessExponent", "?ReflectionColor", "?TransparentColor",
      "?ShadingModel", "?EmissiveFactor", "?SpecularFactor", "?AmbientFactor", "?BumpFactor");
  Fbx fbx;
  public Node geometry;
  public Node model;
  public Node material;
  Node[] materialProperties;

  Col diffuseColor;
  Double opacity;
  public Col specularColor;
  public Double reflectionFactor;
  public Double shininessExponent;

  public Geometry(Fbx fbx, Node geometry) {
    this.fbx = fbx;
    this.geometry = geometry;
    this.model = fbx.or.get(geometry).get("Model").get(0);
    this.material = fbx.oo.get(model).get("Material").get(0);
    this.materialProperties = material.get("Properties70").nested;
    this.diffuseColor = getPropertyCol("DiffuseColor");
    this.opacity = getPropertyDouble("Opacity");
    this.specularColor = getPropertyCol("SpecularColor");
    this.reflectionFactor = getPropertyDouble("ReflectionFactor");
    this.shininessExponent = getPropertyDouble("ShininessExponent");
    // unidentified properties: AmbientColor, DiffuseFactor, TransparencyFactor, Emissive, Ambient, Reflectivity
    // Diffuse (Vector) repeats DiffuseColor (A)
    // Specular (Vector) repeats SpecularColor (A)
    // Shininess (double) repeats ShininessExponent (A)
//    Arrays.stream(materialProperties).filter(n -> !PROPERTY_KEYS.contains(n.property[0])).forEach(System.out::println);
  }

  public Col getPropertyCol(String key) {
    for (Node node : materialProperties) if (key.equals(node.property[0]))
      return new Col((double) node.property[4], (double) node.property[5], (double) node.property[6], 1);
    return null;
  }

  public Double getPropertyDouble(String key) {
    for (Node node : materialProperties) if (key.equals(node.property[0])) return (Double) node.property[4];
    return null;
  }

  //P "AmbientColor"       "Color"    ""       "A" 0.5 0.5 0.5
  //P "DiffuseColor"       "Color"    ""       "A" 0.5 0.1 0.0
  //P "DiffuseFactor"      "Number"   ""       "A" 0.8
  //P "TransparencyFactor" "Number"   ""       "A" 1.0
  //P "SpecularColor"      "Color"    ""       "A" 0.5 0.5 0.5
  //P "ReflectionFactor"   "Number"   ""       "A" 0.5
  //P "Emissive"           "Vector3D" "Vector" ""  0.0 0.0 0.0
  //P "Ambient"            "Vector3D" "Vector" ""  0.5 0.5 0.5
  //P "Diffuse"            "Vector3D" "Vector" ""  0.4 0.1 0.0
  //P "Specular"           "Vector3D" "Vector" ""  0.5 0.5 0.5
  //P "Shininess"          "double"   "Number" ""  2.0
  //P "Opacity"            "double"   "Number" ""  1.0
  //P "Reflectivity"       "double"   "Number" ""  0.0
  //P "ShininessExponent"  "Number"   ""       "A" 5.0
  //P "ReflectionColor"    "Color"    ""       "A" 0.8 0.8 0.8
  //P "TransparentColor"   "Color"    ""       "A" 1.0 1.0 1.0

  public BufferedImage getImageMap(String mapName) {
    Node diffuseColorNode = fbx.op.getOrDefault(material, Collections.emptyMap())
        .getOrDefault(mapName, Collections.singletonList(null)).get(0);
    String diffuseColor = diffuseColorNode == null ? null : diffuseColorNode.getString("RelativeFilename");
    if (diffuseColor == null) return null;
    diffuseColor = diffuseColor.substring(Math.max(diffuseColor.lastIndexOf('/'), diffuseColor.lastIndexOf('\\')) + 1);
    if (".".equals(diffuseColor)) diffuseColor = "default.png";
    Path path = fbx.path.resolve(diffuseColor);
    if (!Files.isRegularFile(path)) throw new IllegalStateException(path.toString());
    return Obj.image(path);
  }

  public BufferedImage getDiffuseMap() {
    BufferedImage image = getImageMap("DiffuseColor");
    return image == null ? getImageMap("3dsMax|CoronaPhysicalMtlPb|baseTexmap") : image;
  }

  public BufferedImage getBumpMap() {
    return getImageMap("Bump"); // 3dsMax|CoronaPhysicalMtlPb|baseBumpTexmap
  }

  public int getDiffuseColor() {
    Col col = Optional.ofNullable(diffuseColor).orElse(new Col(-1));
    col.a = Optional.ofNullable(opacity).orElse(1.0);
    return col.argb();
  }

  public double[] getNormalDoubles(String key1, String key2) {
    Node[] layerElementNormals = geometry.getAll("LayerElement" + key1);
    if (layerElementNormals.length == 0) return new double[6];
    return (double[]) layerElementNormals[0].get(key2).property[0];
  }

  public int[] getPolygonIndex(String key1, String key2, int[] polygonVertexIndex) {
    Node[] layerElementNormals = geometry.getAll("LayerElement" + key1);
    if (layerElementNormals.length == 0) return new int[polygonVertexIndex.length];
    Node layerElementNormal = layerElementNormals[0];
    String mappingInformationType = layerElementNormal.getString("MappingInformationType");
    String referenceInformationType = layerElementNormal.getString("ReferenceInformationType");
    int[] indexToDirect;
    switch (referenceInformationType) {
      case "Direct":
        indexToDirect = new int[polygonVertexIndex.length];
        for (int i = 0; i < indexToDirect.length; i++) indexToDirect[i] = i;
        break;
      case "IndexToDirect":
        indexToDirect = (int[]) layerElementNormal.get(key2 + "Index").property[0];
        break;
      default: throw new IllegalStateException();
    }
    int[] polygonNormalIndex = new int[polygonVertexIndex.length];
    switch (mappingInformationType) {
      case "ByPolygonVertex":
        if (indexToDirect.length != polygonVertexIndex.length) throw new IllegalStateException();
        polygonNormalIndex = indexToDirect;
        break;
      case "ByVertice":
        for (int i = 0; i < polygonVertexIndex.length; i++) {
          int vi = polygonVertexIndex[i];
          if (vi < 0) vi = -1 - vi;
          polygonNormalIndex[i] = indexToDirect[vi];
        }
        break;
      default: throw new IllegalStateException();
    }
    return polygonNormalIndex;
  }

  /**
   * In place rotation, angle 0-1, axis 0x,1y,2z
   */
  public static void rotate(double[] vertex, double angle, int axis) {
    angle = 2 * Math.PI * angle;
    double s = Math.sin(angle);
    double c = Math.cos(angle);
    int ax = (axis + 1) % 3;
    int ay = (axis + 2) % 3;
    for (int i = 0; i < vertex.length; i += 3) {
      double x = vertex[i + ax];
      double y = vertex[i + ay];
      vertex[i + ax] = x * c - y * s;
      vertex[i + ay] = x * s + y * c;
    }
  }

  public static Pnt getProperties70(Node node) {
    return new Pnt((Double) node.property[4], (Double) node.property[5], (Double) node.property[6]);
  }

  public static void translate(double[] vertices, Pnt pnt) {
    for (int i = 0; i < vertices.length; i += 3) {
      vertices[i] += pnt.x;
      vertices[i + 1] += pnt.y;
      vertices[i + 2] += pnt.z;
    }
  }

  public Obj getObj() {
    double[] vertices = (double[]) geometry.get("Vertices").property[0];
    final int verticesLength = vertices.length;
    vertices = Arrays.copyOf(vertices, verticesLength);
    int[] polygonVertexIndex = (int[]) geometry.get("PolygonVertexIndex").property[0];

    double[] normals = getNormalDoubles("Normal", "Normals");
    normals = Arrays.copyOf(normals, normals.length);
    int[] polygonNormalIndex = getPolygonIndex("Normal", "Normals", polygonVertexIndex);
    double[] uv = getNormalDoubles("UV", "UV");
    int[] polygonUvIndex = getPolygonIndex("UV", "UV", polygonVertexIndex);

    ArrayList<Integer> faceList = new ArrayList<>();
    for (int i = 0; i < polygonVertexIndex.length; i++) {
      int i1 = i + 1;
      int v1;
      do {
        i1++;
        v1 = polygonVertexIndex[i1];
      } while (v1 >= 0);
      v1 = -1 - v1;
      for (int i0 = i + 1; i0 < i1; i0++) {
        faceList.add(v1);
        faceList.add(polygonNormalIndex[i1]);
        faceList.add(polygonUvIndex[i1]);
        faceList.add(polygonVertexIndex[i0 - 1]);
        faceList.add(polygonNormalIndex[i0 - 1]);
        faceList.add(polygonUvIndex[i0 - 1]);
        faceList.add(polygonVertexIndex[i0]);
        faceList.add(polygonNormalIndex[i0]);
        faceList.add(polygonUvIndex[i0]);
      }
      i = i1;
    }
    int[] face = faceList.stream().mapToInt(a -> a).toArray();
    Obj obj = new Obj();
    Pnt lclTranslation = null;
    Pnt lclRotation = null;
    Pnt lclScaling = null;
    Pnt scalingPivot = null;
    for (Node p : model.get("Properties70").nested) {
      switch ((String) p.property[0]) {
        case "Lcl Translation": lclTranslation = getProperties70(p); break;
        case "Lcl Rotation": lclRotation = getProperties70(p); break;
        case "Lcl Scaling": lclScaling = getProperties70(p); break;
        case "ScalingPivot": scalingPivot = getProperties70(p); break;
      }
    }
    if (lclScaling != null) {
      //if (scalingPivot != null) translate(vertices, scalingPivot);
      for (int i = 0; i < verticesLength; i += 3) {
        vertices[i] *= lclScaling.x;
        vertices[i + 1] *= lclScaling.y;
        vertices[i + 2] *= lclScaling.z;
      }
      //if (scalingPivot != null) translate(vertices, new Pnt(-scalingPivot.x, -scalingPivot.y, -scalingPivot.z));
    }
    if (lclRotation != null) {
      rotate(vertices, lclRotation.z / 360, 1); // before y2
      rotate(normals, lclRotation.z / 360, 1);
      rotate(vertices, lclRotation.y / 360, 2); // before x0
      rotate(normals, lclRotation.y / 360, 2);
      rotate(vertices, lclRotation.x / 360, 0); // after y2
      rotate(normals, lclRotation.x / 360, 0);
    }
    if (lclTranslation != null) translate(vertices, lclTranslation);

    obj.vertex = vertices;
    obj.face = face;
    obj.normal = normals;
    obj.texture = uv;
    return obj;
  }

  public static Geometry[] load(Path path) {
    List<Path> pathList;
    try {
      pathList = Files.find(path, 2, (p, attributes) ->
          attributes.isRegularFile() && p.toString().toLowerCase().endsWith(".fbx"))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return pathList.stream().flatMap(p -> Arrays.stream(Fbx.fromPath(p)[0].geometry())).toArray(Geometry[]::new);
  }
}
