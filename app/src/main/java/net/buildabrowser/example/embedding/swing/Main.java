package net.buildabrowser.example.embedding.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.buildabrowser.babbrowser.common.util.CommonUtil;
import net.buildabrowser.babbrowser.embedding.swing.SwingEmbedding;
import net.buildabrowser.babbrowser.embedding.swing.SwingEmbedding.FrameAndComponent;
import net.buildabrowser.babbrowser.painter.core.ComponentPainter;
import net.buildabrowser.babbrowser.painter.java2d.Java2DPainter;
import net.buildabrowser.babbrowser.painter.skija.SkijaAWTPainter;

public class Main {
  
  public static void main(String[] args) {
    System.setProperty("org.lwjgl.opengl.contextAPI", "GLX");
    setLookAndFeel();
    boolean useSkija = List.of(args).contains("--use-skija");
    SwingUtilities.invokeLater(() -> CommonUtil.rethrowV(() -> start(useSkija)));
  }

  private static void start(boolean useSkija) throws IOException {
    ComponentPainter<Component> painter = useSkija ?
      new SkijaAWTPainter(false, false) :
      new Java2DPainter();
    FrameAndComponent frameAndComponent = SwingEmbedding
      .newFrameComponent("https://example.com/", painter);
    Component frameComponent = frameAndComponent.component();

    JFrame jframe = new JFrame("BuildABrowser - Swing Embedded Renderer");
    jframe.setSize(new Dimension(800, 600));
    jframe.add(frameComponent);
    jframe.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        CommonUtil.rethrowV(frameAndComponent.frame()::close);
        jframe.dispose();
      }
    });
    jframe.setVisible(true);
  }

  // BUG: This is required when using the Skija painter, and will lead to a JVM crash if not included
  // (it is not required when using the Java2D painter)
  private static void setLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
      throw new RuntimeException(e);
    }
  }
  
}
