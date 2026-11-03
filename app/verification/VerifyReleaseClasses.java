public class VerifyReleaseClasses {
    public static void main(String[] args) throws ClassNotFoundException {
        // Loading with initialization makes the JVM verify the canvas draw lambda.
        Class.forName(
            "com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.LayoutCanvasKt",
            true,
            ClassLoader.getSystemClassLoader()
        );
        System.out.println("Release canvas bytecode verified");
    }
}
