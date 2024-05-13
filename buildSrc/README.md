# Gradle插件

### AutoDimen

#### 1. 导入插件

```groovy
apply plugin: 'AutoDimens'
```

#### 2. 自定义配置

```
AutoDimens {
    design {
        width = 1920            // 设计稿-宽度
        height = 1080           // 设计稿-高度
    }
    target {
        eq100 {                 // 适配机型1（名称可自定义，不限个数，类似多渠道配置）
            width = 1080        // 适配机型-宽度
            height = 720        // 适配机型-高度
            scale = 0.56        // 适配机型相对设计稿缩放比例
        }
        mce {
            width = 2560
            height = 1440
            scale = 1.33
        }
        ......                  // 可添加多个
    }
    outputFileName = "dimens_test.xml"  // 输出文件名，默认“dimens_auto.xml”
}
```

#### 3. 执行Task

```
执行项目->Gradle->senierr->buildAutoDimens
```

#### 4. 查看执行日志

```
> Task :base:buildAutoDimens
makeDimens - design: 1920, 1080
makeDimens - target: eq100 (1080, 720)[0.56]
makeDimens - target: mce (2560, 1440)[1.33]
```
