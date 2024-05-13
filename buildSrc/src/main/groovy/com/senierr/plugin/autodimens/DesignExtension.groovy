package com.senierr.plugin.autodimens

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * DSL参数配置
 */
class DesignExtension {

    // 设计稿尺寸
    ScreenSize design = new ScreenSize("default")
    // 适配尺寸
    NamedDomainObjectContainer<ScreenSize> target
    // 生成文件名，默认dimens.xml，若存在则dimens_auto.xml
    String outputFileName

    DesignExtension(Project project) {
        // 创建容器对象
        NamedDomainObjectContainer<ScreenSize> _target = project.container(ScreenSize)
        target = _target
    }

    void design(Action<ScreenSize> action) {
        action.execute(design)
    }

    void target(Action<NamedDomainObjectContainer<ScreenSize>> action) {
        action.execute(target)
    }
}