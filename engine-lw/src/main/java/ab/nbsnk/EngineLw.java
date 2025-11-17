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

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class EngineLw implements Engine3d {
    @Override
    public EngineLw open(BufferedImage image) {
        return this;
    }

    @Override
    public EngineLw background(BufferedImage image) {
        return this;
    }

    @Override
    public ShapeLw shape(Obj obj) {
        return new ShapeLw();
    }

    @Override
    public GroupLw group() {
        return new GroupLw();
    }

    @Override
    public LightLw light() {
        return new LightLw();
    }

    @Override
    public EngineLw setAmbient(int color) {
        return this;
    }

    @Override
    public NodeLw camera() {
        return new NodeLw();
    }

    @Override
    public EngineLw setFarClip(double value) {
        return this;
    }

    @Override
    public EngineLw setFocalLength(double value) {
        return this;
    }

    @Override
    public void update() {

    }

    @Override
    public void sysex(int i) {

    }

    @Override
    public EngineLw textSupplier(Supplier<String> supplier) {
        return this;
    }

    @Override
    public void close() {

    }

    public static class NodeLw implements Node {
        @Override
        public NodeLw translation(double x, double y, double z) {
            return this;
        }

        @Override
        public NodeLw rotation(double yaw, double pitch, double roll) {
            return this;
        }

        @Override
        public NodeLw setPivot() {
            return this;
        }

        @Override
        public NodeLw connect(Group node) {
            return this;
        }

        @Override
        public NodeLw setVisible(boolean value) {
            return this;
        }
    }

    public static class ShapeLw extends NodeLw implements Shape {
        @Override
        public ShapeLw setColor(int color) {
            return this;
        }

        @Override
        public ShapeLw setSpecular(int color, double power) {
            return this;
        }

        @Override
        public ShapeLw selfIllumination(int color) {
            return this;
        }

        @Override
        public ShapeLw setBumpMap(BufferedImage image) {
            return this;
        }

        @Override
        public ShapeLw setReflectionMap(BufferedImage image, double alpha, Node skybox) {
            return this;
        }
    }

    public static class GroupLw extends NodeLw implements Group {

    }

    public static class LightLw extends NodeLw implements Light {
        @Override
        public LightLw setColor(int color) {
            return this;
        }
    }

}
