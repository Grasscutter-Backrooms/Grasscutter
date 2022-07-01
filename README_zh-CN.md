![Grasscutter](https://socialify.git.ci/Grasscutters/Grasscutter/image?description=1&forks=1&issues=1&language=1&logo=https%3A%2F%2Fs2.loli.net%2F2022%2F04%2F25%2FxOiJn7lCdcT5Mw1.png&name=1&owner=1&pulls=1&stargazers=1&theme=Light)
<div align="center"><img alt="Documention" src="https://img.shields.io/badge/Wiki-Grasscutter-blue?style=for-the-badge&link=https://github.com/Grasscutters/Grasscutter/wiki&link=https://github.com/Grasscutters/Grasscutter/wiki"> <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/Grasscutters/Grasscutter?logo=java&style=for-the-badge"> <img alt="GitHub" src="https://img.shields.io/github/license/Grasscutters/Grasscutter?style=for-the-badge"> <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/Grasscutters/Grasscutter?style=for-the-badge"> <img alt="GitHub Workflow Status" src="https://img.shields.io/github/workflow/status/Grasscutters/Grasscutter/Build?logo=github&style=for-the-badge"></div>

<div align="center"><a href="https://discord.gg/T5vZU6UyeG"><img alt="Discord - Grasscutter" src="https://img.shields.io/discord/965284035985305680?label=Discord&logo=discord&style=for-the-badge"></a></div>

[EN](README.md) | 中文 | [繁中](README_zh-TW.md) | [FR](README_fr-FR.md)

## 免责声明
这是Grasscutter的实验版本，支持2.7.50/2.8，不要将其安装在公开的服务器上。 \
您有破坏别人游戏体验的风险，或者您使他们被封禁的可能性更高。在我们有更好的解决方案之前，请不要泄露。 \
如果你已经看到这里，那么您很可能了解这个私有存储库的规则。但即便如此，我将在此重复它们：
```
- 禁止请求帮助设置私人服务器或客户端
- 禁止将服务器泄露给非贡献者
- 禁止将服务器托管为实验服务器
```
**违反任何这些规则将导致您失去访问存储库的权限！**
祝开发者们玩得开心 :)


## 当前特性

* 登录
* 战斗
* 好友列表
* 传送系统
* 祈愿系统
* 从控制台生成魔物
* 多人游戏 *部分* 可用
* 物品栏相关 (接收物品/角色, 升级角色/武器等)

## 快速设置指南

**附:** 加入我们的 [Discord](https://discord.gg/T5vZU6UyeG) 获取更多帮助！

### 环境需求

* Java SE - 17 (当您没有Oracle账户，可以使用[镜像](https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/))

  **注:** 如果您仅仅想要简单地**运行服务端**, 那么使用 **jre** 便足够了 

* [MongoDB](https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-5.0.9-signed.msi) (推荐 4.0+)

* 代理程序: mitmproxy (推荐 mitmdump), Fiddler Classic 等

### 运行

**注意:** 从旧版本升级到新版本, 需要删除 `config.json`

1. 获取 `grasscutter.jar`
   - 从 [actions](https://github.com/Grasscutters/Grasscutter/suites/6895963598/artifacts/267483297) 中下载
   - [自行构建](#构建)
2. 在**grasscutter.jar** 所在目录中创建 `resources` 文件夹并将 `BinOutput` 和 `ExcelBinOutput` 放入其中 *(查看 [wiki](https://github.com/Grasscutters/Grasscutter/wiki) 了解更多)*
3. 通过命令 `java -jar grasscutter.jar` 来运行Grasscutter。 **在此之前请确认MongoDB服务运行正常**

### 连接

½. 在服务器控制台中 [创建账户](#命令列表)。

1. 重定向流量: (选其一)
    - mitmdump:  `mitmdump -s proxy.py -k`
    
      信任 CA 证书:
    
      ​	**注:** mitmproxy的CA证书通常存放在 `% USERPROFILE%\ .mitmproxy`, 或者你也可以从`http://mitm.it` 中下载它
    
      ​	双击来[安装根证书](https://docs.microsoft.com/en-us/skype-sdk/sdn/articles/installing-the-trusted-root-certificate#installing-a-trusted-root-certificate) 或者..
    
      - 使用命令行
    
        ```shell
        certutil -addstore root %USERPROFILE%\.mitmproxy\mitmproxy-ca-cert.cer
        ```
    
    - Fiddler Classic: 运行Fiddler Classic, 在设置中开启 `解密https通信` 并将端口切换到除`8888` 以外的任意端口 (工具 -> 选项 -> 连接) 并加载 [此脚本](https://github.lunatic.moe/fiddlerscript).
      
    - [Hosts文件](https://github.com/Grasscutters/Grasscutter/wiki/Running#traffic-route-map)
    
2. 设置代理为 `127.0.0.1:8080` 或其它你所设定的端口。

**你也可以简单地运行 `start.cmd` 来全自动启动服务端并设置代理**

### 构建

Grasscutter 使用 Gradle 来处理依赖及构建。

**依赖:**

- Java SE Development Kits - 17
- Git

##### Windows

```shell
git clone https://git.4benj.com/Grasscutter-Backrooms/Grasscutter.git
cd Grasscutter
.\gradlew.bat # 设置环境
.\gradlew jar # 编译Jar文件
```

##### Linux

```bash
git clone https://git.4benj.com/Grasscutter-Backrooms/Grasscutter.git
cd Grasscutter
chmod +x gradlew
./gradlew jar # 编译Jar文件
```

你将在项目根目录中找到编译完成的 `grasscutter.jar`。

# 命令列表

### 命令已被移动至 [维基](https://github.com/Grasscutters/Grasscutter/wiki/Commands)!

# 快速排除问题

* 如果编译未能成功,请检查您的jdk安装 (JDK 17并确认jdk处于环境变量`PATH`中
* 我的客户端无法登录/连接, 4206, 其它... - 大部分情况下这是因为您的代理存在问题.如果使用Fiddler请确认Fiddler监听端口不是`8888`
* 启动顺序: MongoDB > Grasscutter > 代理程序 (mitmdump, fiddler等.) > 客户端
