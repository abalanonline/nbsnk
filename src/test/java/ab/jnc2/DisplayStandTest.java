package ab.jnc2;

import ab.nbsnk.Obj;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DisplayStandTest {

  @Disabled
  @Test
  void main() throws IOException {
    DisplayStand displayStand = new DisplayStand();
    Obj obj = Obj.load(Files.newInputStream(Paths.get("assets/teapot.obj")));
    DisplayStand.Obj dobj = new DisplayStand.Obj();
    dobj.face = obj.face;
    dobj.vertex = obj.vertex;
    dobj.normal = obj.normal;
    dobj.texture = obj.texture;
    dobj.image = ImageIO.read(Files.newInputStream(Paths.get("assets/maptest.png")));
    displayStand.setObj(dobj);
    displayStand.open();
  }
}
