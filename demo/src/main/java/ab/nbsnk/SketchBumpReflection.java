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

package ab.nbsnk;

import ab.nbsnk.nodes.Shapes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

public class SketchBumpReflection {
  public static void main(String[] args) {
    // https://commons.wikimedia.org/wiki/File:Normal_map_example_with_scene_and_result.png
    BufferedImage normalMapExampleWithSceneAndResult = Obj.image(
        Paths.get("assets/Normal_map_example_with_scene_and_result.png"));
    BufferedImage obj1bump = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);
    obj1bump.getGraphics().drawImage(normalMapExampleWithSceneAndResult, -2048, 0, null);
    //obj1bump.getGraphics().drawImage(normalMapExampleWithSceneAndResult, -2048, 2048, 2048 * 3, -2048, null);
    //try { ImageIO.write(obj1bump, "png", new File("assets/test.png")); } catch (IOException ignore) {}
    Obj obj1 = new Shapes.Square();
    //obj1.image = Obj.image(Paths.get("assets/maptest.png"));
    //obj1.texture = new double[]{1, 0.5, 0.5, 1, 0, 0.5, 0.5, 0,};

    // https://commons.wikimedia.org/wiki/File:NormalMaps.png
    BufferedImage normalMaps = Obj.image(Paths.get("assets/NormalMaps.png"));
    BufferedImage obj2bump = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    obj2bump.getGraphics().drawImage(normalMaps, -512, 0, null);
    obj2bump.getGraphics().drawImage(normalMaps, 2 * 512, 0, -3 * 512, 512, null);
    // certainly the sphere in wiki example has an x inverted texture coordinates
    BufferedImage obj2image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    obj2image.getGraphics().drawImage(normalMaps, 512, 0, -3 * 512, 512, null);
    Obj obj2 = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj")).interpolateNormal(); // HD sphere
    obj2.image = obj2image;

    Dimension screenSize = new Dimension(640, 360);
    BufferedImage background = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_ARGB);
    Sketch2.renderNoise(background);
    Engine3d engine3d = new EngineDual().background(background);
    SceneViewer sceneViewer = new SceneViewer(engine3d, screenSize);
    BufferedImage photosphere = Obj.image(Paths.get("assets/reflection_sphere.jpg"));
    Engine3d.Node sky = engine3d.shape(Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj"))
        .interpolateNormal().scale(95).ry90().inverted()
        .withImage(photosphere)).selfIllumination(-1).connect(sceneViewer.sky);

    // shapes
//    engine3d.shape(obj1).setBumpMap(obj1bump).setSpecular(-1, 100).connect(sceneViewer.node);
    Obj obj = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj")).interpolateNormal();
    engine3d.shape(obj).setSpecular(-1, 100).setReflectionMap(photosphere, 0.3, sceneViewer.sky).connect(sceneViewer.node);
    engine3d.light().setColor(0xFFBF7F).translation(-100, 100, 100);
    sceneViewer.run();
  }
}
