package com.senierr.plugin.autodimens


import org.gradle.api.Plugin
import org.gradle.api.Project

class AutoDimens implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // 创建DSL
        def extension  = project.extensions.create('AutoDimens', DesignExtension)
        // 创建任务
        project.task('buildAutoDimens') {
            group = "senierr"
            doFirst {
                executeTask(project, extension)
            }
        }
    }

    /**
     * 执行任务
     *
     * @param project 项目
     * @param extension 扩展属性
     */
    static void executeTask(Project project, def extension) {
        // 当为设计图尺寸模板，输出至默认文件夹一份
        println("makeDimens - design: " + extension.design.width + ", " + extension.design.height)
        // 生成默认尺寸
        File outputFile = getDimenFile(project.projectDir, "values", extension.outputFileName)
        makeDimens(outputFile, extension, extension.design)
        // 生成默认适配尺寸
//        File outputFileTarget = getDimenFile(
//                project.projectDir,
//                "values-sw" + getMinSize(extension.design.width, extension.design.height) + "dp",
//                extension.outputFileName
//        )
//        makeDimens(outputFileTarget, extension, extension.design)
        // 生成适配尺寸
        for (final def screen in extension.target) {
            println("makeDimens - target: " + screen.name
                    + " (" + screen.width + ", " + screen.height + ")"
                    + "[" + screen.scale + "]")
            outputFile = getDimenFile(
                    project.projectDir,
                    "values-sw" + getMinSize(screen.width, screen.height) + "dp",
                    extension.outputFileName
            )
            makeDimens(outputFile, extension, screen)
        }
    }

    /**
     * 创建dimen文件内容
     *
     * @param sw 最小宽度 dp
     * @param maxDP 最大尺寸 dp
     * @param maxSP 最大尺寸 sp
     * @param designSW 设计图最小宽度 dp
     * @return
     */
    private static void makeDimens(File dimenFile, DesignExtension extension, ScreenSize screenSize) {
        if (dimenFile == null || extension == null || screenSize == null) return
        StringBuilder sb = new StringBuilder()
        try {
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n")
            sb.append("<resources>\r\n")

            int maxLength
            int minLength
            if (extension.design.width < extension.design.height) {
                minLength = extension.design.width
                maxLength = extension.design.height
            } else {
                minLength = extension.design.height
                maxLength = extension.design.width
            }
            double scale = screenSize.scale
            if (screenSize.scale <= 0) {
                scale = 1
            }
            // 生成SP
            for (int i = 0; i <= minLength; i++) {
                float spValue = i * scale
                sb.append("\t<dimen name=\"sp_" + i + "\">" + String.format("%.1f", spValue) + "sp</dimen>\r\n")
            }
            // 生成DP
            for (int i = 0; i <= maxLength; i++) {
                float dpValue = i * scale
                sb.append("\t<dimen name=\"dp_" + i + "\">" + String.format("%.1f", dpValue) + "dp</dimen>\r\n")
            }
            sb.append("</resources>\r\n")
        } catch (Exception e) {
            e.printStackTrace()
        }
        outputDimenFile(dimenFile, sb.toString())
    }

    /**
     * 获取Dimen文件
     *
     * @param projectDir
     * @param folderName
     * @return
     */
    private static File getDimenFile(File projectDir, String folderName, String fileName) {
        // 创建文件夹
        File dir = new File(projectDir.absolutePath + File.separator +
                "src" + File.separator +
                "main" + File.separator +
                "res" + File.separator +
                folderName)
        if (!dir.exists()) dir.mkdirs()
        // 创建文件
        File dimenFile
        if (fileName != null) {
            dimenFile = new File(dir, fileName)
        } else {
            dimenFile = new File(dir, "dimens.xml")
            if (dimenFile.exists()) {
                dimenFile = new File(dir, "dimens_auto.xml")
            }
        }

        if (dimenFile.exists()) {
            dimenFile.delete()
        }
        return dimenFile
    }

    /**
 * 输出文件
 *
 * @param dimenFile
 * @param content
 */
    private static void outputDimenFile(File dimenFile, String content) {
        try {
            FileOutputStream fos = new FileOutputStream(dimenFile.absolutePath)
            fos.write(content.getBytes())
            fos.flush()
            fos.close()
        } catch (FileNotFoundException e) {
            e.printStackTrace()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    private static int getMinSize(int width, height) {
        int minLength = height
        if (width < height) {
            minLength = width
        } else {
            minLength = height
        }
        return minLength
    }
}