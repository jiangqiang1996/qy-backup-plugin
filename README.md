# qy-backup-plugin

Halo 2.0 备份增强插件，支持版本>=2.10。

## 开发环境

插件开发的详细文档请查阅：<https://docs.halo.run/developer-guide/plugin/hello-world>

```bash
git clone git@github.com:halo-sigs/qy-backup-plugin.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/qy-backup-plugin.git
```

```bash
cd path/to/qy-backup-plugin
```

```bash
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    fixedPluginPath:
      - "/path/to/qy-backup-plugin"
```
