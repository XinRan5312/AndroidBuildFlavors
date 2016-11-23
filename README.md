# AndroidBuildFlavors
多渠道打包  多渠道打包的技巧
最近有个需求一次要打包9个类型的App，而且常量和String.xml都有变量。虽然之前也是一直存在变量，但是每次也仅仅只打包一个。这让我每次改变量，打包9个。要是以后每次都打包9次，我得疯了。
根据之前的了解，gradle 应该是可以解决这个问题的。所以就仔细研究了一番。

先放一个完整的 多渠道/多环境 打包的配置，然后再来讲解。

    实现了：

        不同环境，不同包名；
        不同环境，修改不同的 string.xml 资源文件；
        不同环境，修改指定的常量；
        不同环境，修改 AndroidManifest.xml 里渠道变量

apply plugin: 'com.android.application'

android {
    compileSdkVersion 22
    buildToolsVersion '22.0.1'

    // 签名文件
    signingConfigs {
        config {
            keyAlias 'lyl'
            keyPassword '123456'
            storeFile file('../lyl.jks')
            storePassword '123456'
        }
    }

    // 默认配置
    defaultConfig {
        //applicationId "com.lyl.app"
        minSdkVersion 16
        targetSdkVersion 22
        versionCode 1
        versionName "1.0.0"

        signingConfig signingConfigs.config
        multiDexEnabled true
    }

    // 多渠道/多环境 的不同配置
    productFlavors {
        dev {
            // 每个环境包名不同
            applicationId "com.lyl.dev"
            // 动态添加 string.xml 字段；
            // 注意，这里是添加，在 string.xml 不能有这个字段，会重名！！！
            resValue "string", "app_name", "dev_myapp"
            resValue "bool", "isrRank", 'false'
            // 动态修改 常量 字段
            buildConfigField "String", "ENVIRONMENT", '"dev"'
            // 修改 AndroidManifest.xml 里渠道变量
            manifestPlaceholders = [UMENG_CHANNEL_VALUE: "dev"]
        }
        stage {
            applicationId "com.lyl.stage"

            resValue "string", "app_name", "stage_myapp"
            resValue "bool", "isrRank", 'true'

            buildConfigField "String", "ENVIRONMENT", '"stage"'

            manifestPlaceholders = [UMENG_CHANNEL_VALUE: "stage"]
        }
        prod {
            applicationId "com.lyl.prod"

            resValue "string", "app_name", "myapp"
            resValue "bool", "isrRank", 'true'

            buildConfigField "String", "ENVIRONMENT", '"prod"'

            manifestPlaceholders = [UMENG_CHANNEL_VALUE: "prod"]
        }
    }

    //移除lint检测的error
    lintOptions {
        abortOnError false
    }

    def releaseTime() {
        return new Date().format("yyyy-MM-dd", TimeZone.getTimeZone("UTC"))
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.config
        }

        release {
            buildConfigField("boolean", "LOG_DEBUG", "false")
            minifyEnabled false
            zipAlignEnabled true
            //移除无用的resource文件
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.config

            // 批量打包
            applicationVariants.all { variant ->
                variant.outputs.each { output ->
                    def outputFile = output.outputFile
                    if (outputFile != null && outputFile.name.endsWith('.apk')) {
                        //输出apk名称为：渠道名_版本名_时间.apk
                        def fileName = "${variant.productFlavors[0].name}_v${defaultConfig.versionName}_${releaseTime()}.apk"
                        output.outputFile = new File(outputFile.parent, fileName)
                    }
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compile 'com.facebook.android:facebook-android-sdk:4.0.0'
    compile project(':qrscan')
    compile 'com.android.support:appcompat-v7:22.0.0'
    compile 'com.google.code.gson:gson:2.3'
    compile files('libs/android-async-http-1.4.6.jar')
    compile 'com.google.android.gms:play-services:7.5.0'
    compile 'com.android.support:support-annotations:22.1.1'
    compile 'com.github.bumptech.glide:glide:3.7.0'
    compile 'de.hdodenhof:circleimageview:2.1.0'
}

接下来我们来详细看看修改特定的字段。

不同环境的设置基本都是在 productFlavors 里设置的，
而且在里面你想添加多少个环境都可以。
1. 不同环境，不同包名；

productFlavors {
    dev {
        applicationId "com.lyl.dev"
    }

    stage {
        applicationId "com.lyl.stage"
    }

    prod {
        applicationId "com.lyl.prod"
    }
}

这里注意，在 defaultConfig 中，大家应该都是写了个默认的 applicationId 的。
经测试，productFlavors 设置的不同环境包名会覆盖 defaultConfig 里面的设置，
所以我们可以推测，它执行的顺序应该是先执行默认的，然后在执行分渠道的，如果冲突，会覆盖处理，这也很符合逻辑。


2. 不同环境，添加 string.xml 资源文件；

利用 resValue 来定义资源的值，顾名思义 res 底下的内容应该都可以创建，最后用 R.xxx.xxx 来引用。
如下就根据不同的类型，添加了不同的 app_name 字段，以及定义了 布尔值，可以通过 R.string.app_name 来引用。
注意，这里是添加，是在 string.xml 里面添加了一个字段app_name，所以在现有的 string.xml 中不能有这个字段，否则会报错！！！

productFlavors {
    dev {
        resValue "string", "app_name", "dev_myapp"
        resValue "bool", "isrRank", 'false'
    }
    stage {
        resValue "string", "app_name", "stage_myapp"
        resValue "bool", "isrRank", 'true'
    }
    prod {
        resValue "string", "app_name", "myapp"
        resValue "bool", "isrRank", 'true'
    }
}

通过以上我们大概可以推测出 color、dimen 也可以通过类似的方法添加。


3. 不同环境，动态修改指定的常量；

使用 BuildConfig 的变量。
①定义字段

当我们定义如下字段之后，编译后自动生成文件，在 app/build/source/BuildConfig/dev/com.lyl.dev/BuildConfig 目录，
打开这个文件，我们就能看到我们所定义的字段了。

productFlavors {
    dev {
        buildConfigField "String", "ENVIRONMENT", '"dev"'
    }
    stage {
        buildConfigField "String", "ENVIRONMENT", '"stage"'
    }
    prod {
        buildConfigField "String", "ENVIRONMENT", '"prod"'
    }
}

②引用字段

在我们自己的任意的类中，来直接通过 BuildConfig 就可以调用我们定义的字段。

public class Constants {
    public static final String ENVIRONMENT = BuildConfig.ENVIRONMENT;

}

注意：这里有个小细节，看其中第三个参数，是先用了“'”，然后在用了“"”，这种语法在 Java 里可能比较陌生，但是在很多其他语言中，这种用法是很常见的。
它的意思是 "dev" 这个整体是属于一个字符串，至于为什么要这么写，你把单引号去掉，然后去 app/build/source/BuildConfig/dev/com.lyl.dev/BuildConfig 这个文件看一看就知道了。


4. 不同环境，修改 AndroidManifest.xml 里渠道变量
①在 AndroidManifest.xml 里添加渠道变量

<application
    android:icon="${app_icon}"
    android:label="@string/app_name"
    android:theme="@style/AppTheme">
    ...
    <meta-data
        android:name="UMENG_CHANNEL"
        android:value="${ENVIRONMENT}" />
    ...
</application>

②在 build.gradle 设置 productFlavors

productFlavors {
    dev {
        manifestPlaceholders = [ENVIRONMENT: "dev",
                                app_icon   : "@drawable/icon_dev"]
    }
    stage {
        manifestPlaceholders = [ENVIRONMENT: "stage",
                                app_icon   : "@drawable/icon_stage"]
    }
    prod {
        manifestPlaceholders = [ENVIRONMENT: "prod",
                                app_icon   : "@drawable/icon_prod"]
    }
}

这样我们可以在不同环境使用不同的 key 值。





通过以上方式，我们基本可以 通过 gradle 动态设定应用标题，应用图标，替换常量，设置不同包名，更改渠道等等。
最后，做完所有的配置之后，执行正常的打包操作。

如图所示：

图片来源网络

图片来源网络



打包完成之后，然后就可以在我们指定的目录下，看到我们所生成的apk包。

最后放上一个项目地址：
https://github.com/Wing-Li/boon

APPWidget桌面小控件:http://blog.csdn.net/sasoritattoo/article/details/17616597
2016-11-23：桌面快捷键的创建