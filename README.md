# BuildABrowser Swing Embedding Example

The Swing Embedding Layer for BuildABrowser allows you to embed a pre-configured instance of the
BuildABrowser renderer into your Swing application. 

This API may change in future releases.

---

These instructions assume you have configured a Gradle project.

Add JitPack to your repositories block:

```groovy
maven { url 'https://jitpack.io/' }
```

Add the embedding layer to your dependencies block:

```groovy
  // TODO: Replace this with the tag for v0.1.0 after release
  implementation "com.github.BuildABrowser.BuildABrowser:EmbeddingSwing:-SNAPSHOT"
```

Fetching the dependency from JitPack can take a hot second. If Gradle times out, retry a few times.

Once you've configured Gradle, you can use this example:

```java
public class SwingEmbeddingTest {
  
  public static void main(String[] args) {
    System.setProperty("org.lwjgl.opengl.contextAPI", "GLX");
    SwingUtilities.invokeLater(() -> CommonUtil.rethrowV(() -> start(useSkija)));
  }

  private static void start() throws IOException {
    FrameAndComponent frameAndComponent = SwingEmbedding.newFrameComponent("https://example.com/");
    Component frameComponent = frameAndComponent.component();

    JFrame jframe = new JFrame("BuildABrowser - Swing Embedded Renderer");
    jframe.setSize(new Dimension(800, 600));
    jframe.add(frameComponent);
    jframe.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        frameAndComponent.frame().close();
        jframe.dispose();
      }
    });
    jframe.setVisible(true);
  }

}
```

---

The embedded renderer uses the Java2D painter by default.
If you need to use the Skija painter, add it to your Gradle.
```groovy
  // TODO: Replace this with the tag for v0.1.0 after release
  implementation "com.github.BuildABrowser.BuildABrowser:PainterSkija:-SNAPSHOT"
```
You also need to configure LWJGL and Skija, which is a bit more complex. See `app/build.gradle` in this repo for an example.

Pass a new instance of `SkijaAWTPainter` as an argument to newFrameComponent.
```java
FrameAndComponent frameAndComponent = SwingEmbedding
  .newFrameComponent("https://example.com/", new SkijaAWTPainter(false, false));
```

**Warning!** The Skija painter has a bug that will cause the JVM to crash if you don't set Swing's LookAndFeel.
```java
private static void setLookAndFeel() {
  try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
  } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
    throw new RuntimeException(e);
  }
}
```
Call `setLookAndFeel();` from the main method.

This warning will be removed once a release includes a patch for this bug.

---

If you need access to the underlying `RenderingEngineBuilder` or need to re-use the `RenderingEngine` instance across multiple frames, you can use `configure` and `createFrameComponent` instead.

```java
public class SwingEmbeddingTest {
  
  public static void main(String[] args) {
    System.setProperty("org.lwjgl.opengl.contextAPI", "GLX");
    SwingUtilities.invokeLater(() -> CommonUtil.rethrowV(() -> start(useSkija)));
  }

  private static void start() throws IOException {
    RenderingEngineBuilder builder = RenderingEngineBuilder.create();
    SwingEmbedding.configure(builder);
    builder.setPainter(new SkijaPainter());
    RenderingEngine renderingEngine = builder.build();

    Frame frame = renderingEngine.createFrame();
    Component frameComponent = SwingEmbedding.createFrameComponent(frame);
    frame.navigate(URI.create("https://example.com/"));

    JFrame jframe = new JFrame("BuildABrowser - Swing Embedded Renderer");
    jframe.setSize(new Dimension(800, 600));
    jframe.add(frameComponent);
    jframe.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        CommonUtil.rethrowV(frame::close);
        jframe.dispose();
      }
    });
    jframe.setVisible(true);
  }

}
```

Use your IDE's code navigation features to find the methods that the builder provides.

Here are some things you can set:

* **Fetch Backend** - Allows the engine to make network and FS requests. Default: `FetchBackendImp`. HttpClient wrapper for HTTP, Files.readAllBytes for File.
* **Fetch Policy** - Allows enabling/disabling cookies and overriding fetch responses (for example, to return a network error for a disallowed request). Default: `new FetchPolicy() {}`, allow all
* **Cookie Store** - Allows determining how cookies are stored (Default: `new InMemoryCookieStore(_1 -> false)`). There are 3 pre-made cookie stores: 
  * `NoOpCookieStore` - Disables cookies
  * `InMemoryCookieStore` - Remembers cookies until the renderer is closed, and then discards them
  * `SQLiteCookieStore` (Requires `:SQLiteCookieStore` module) - Persists cookies into an SQL database, allowing access across multiple sessions.
* **UA Chooser** - Responsible for determining what User-Agent should be attached to a given request. Default: `BasicUAChooserImp`, yields `Mozilla/5.0 ($OS) BABRenderer/0.1.0 Firefox/149.0 (Not actually Firefox)`, with exceptions for whatismybrowser.com and buildabrowser.net
* **Thread Group Supplier** - A supplier that returns the `ExecutorService` used internally for threading. Default: `Executors::newVirtualThreadPerTaskExecutor`
* **Painter** - Responsible for creating graphics on the screen (or other medium, for esoteric use cases). See instructions above to use Skija. Default: `Java2DPainter`.
* **Document Loader Registry** - Responsible for determining how navigables load a given mime type. Register additional loaders on a registry using `DocumentLoaderRegistry#register`. Default: `DocumentLoaderRegistry.createDefault()`
* **Resource Resolver** - Sole job is to resolve resources that were bundled in the application's resource directory. Default: `StandardCommonEmbedding.class.getClassLoader()::getResourceAsStream`
* **Clipboard Provider** - When the user attempts to select and copy data, expected to add that data to the system clipboard. Default: `AWTClipboardProvider`
* **Virtual Keyboard Factory** - Creates an instance of a `VirtualKeyboard`, which is used to integrate with software keyboards that expect to mutate software state instead of sending raw key events. Default: No-Op, `_1 -> new VirtualKeyboard() {}`
* **Tab Manager** - Used to handle requests from a web page to open a new tab. Default: `NoOpTabManager`, opens "new" tabs in current tab
* **Download Manager** - Responsible for determining where a user would like to store downloads, and then streaming the download request to its destination. Has default methods that can be overridden to block downloads. Default: `NoOpDownloadManager`, blocks all downloads

You can only have one component per frame. Attempting to have multiple components mapped to the same frame will lead to undefined behavior.

However, you can have multiple frames per component.
If you have multiple frames you'd like to swap in the same viewport, use the two-argument version of `createFrameComponent`, and use `notifyActivateFrame` when the frame is swapped.