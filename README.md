# FakeLimbo

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-62B47A?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![ProtocolLib](https://img.shields.io/badge/ProtocolLib-required-blue?style=flat-square)

FakeLimbo 是一个基于 Bukkit/Spigot/Paper 的轻量 limbo 插件，通过 ProtocolLib 屏蔽客户端不该收到的世界、实体、声音、粒子等数据包，让玩家进入一个类似 NanoLimbo 的空环境。

玩家仍然可以收到服务端消息，但看不见其他玩家，也不能与其他玩家交互。

## 功能

- 屏蔽区块、地图、实体、声音、粒子、BossBar、计分板等数据包
- 保留聊天和服务端系统消息
- 玩家自动进入旁观者模式
- 清空背包、装备、副手、经验、药水效果等状态
- 玩家之间互相不可见
- 阻止攻击、点击、捡物品、丢物品、交换副手、操作物品栏等交互
- 使用分包结构，方便后续维护

## 环境

| 项目 | 要求 |
| --- | --- |
| Minecraft | 1.21.x |
| Java | 21 |
| 服务端 | Bukkit / Spigot / Paper 兼容服务端 |
| 前置插件 | ProtocolLib |

ProtocolLib 是运行时依赖，需要单独放入服务器 `plugins` 目录。

## 构建

Windows:

```powershell
.\gradlew.bat build
```

Linux / macOS:

```bash
./gradlew build
```

构建完成后，插件文件位于：

```text
build/libs/FakeLimbo-1.0-SNAPSHOT.jar
```

该 jar 已包含 Kotlin 运行库，可以直接部署。

## 部署

1. 停止服务器。
2. 将 `ProtocolLib.jar` 放入服务器 `plugins` 目录。
3. 将 `build/libs/FakeLimbo-1.0-SNAPSHOT.jar` 放入服务器 `plugins` 目录。
4. 如果之前部署过旧版本，建议删除旧的 remap 缓存：

   ```text
   plugins/.paper-remapped/FakeLimbo-1.0-SNAPSHOT.jar
   ```

5. 启动服务器。



| 文件 | 说明 |
| --- | --- |
| `FakeLimbo.kt` | 插件入口，负责生命周期和组件装配 |
| `LimboListener.kt` | 处理玩家加入、移动、重生和交互事件 |
| `LimboPacketBlocker.kt` | 注册 ProtocolLib 数据包监听器 |
| `BlockedPackets.kt` | 维护需要屏蔽的数据包列表 |
| `LimboPlayerService.kt` | 处理玩家状态清理、旁观者模式和玩家隔离 |
