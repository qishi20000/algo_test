# AGENTS.md

## 项目概览

`algo_test` 是一个 Visual Studio 解决方案，包含 5 个独立的 C++ 控制台练习程序（算法与语言特性），无网络服务、无包管理器依赖、无自动化测试套件。

| 可执行目标 | 说明 | 典型输出 |
|-----------|------|----------|
| `binary_tree` | 层序建树，前/中/后序遍历 | 有 stdout |
| `bubblesort` | 10 万元素插入排序耗时 | `durtime: …ms` |
| `cpp_construct` | 构造/拷贝与 `sizeof` | 有 stdout |
| `merge_sort` | 小数组归并排序 | 无 stdout |
| `list_reverse` | 链表反转 | 无 stdout（当前实现运行时段错误，见下） |

## Cursor Cloud specific instructions

### 工具链

- **Linux（Cloud VM）**：使用系统 `g++`（C++17）。仓库未提供 CMake/Makefile；在 `/workspace` 下手动编译到 `build/` 目录。
- **Windows（上游设计）**：用 Visual Studio 打开 `algo_test.sln`（工具集 v141 / VS 2017 风格）。

### Linux 编译（不修改源码的变通）

MSVC 对部分缺失 `#include` 较宽松；在 Linux 上需对个别目标预包含头文件：

```bash
mkdir -p build
g++ -std=c++17 -O2 -o build/binary_tree binary_tree/main.cpp
g++ -std=c++17 -O2 -o build/cpp_construct cpp_construct/main.cpp
g++ -std=c++17 -O2 -include memory -include cstring -o build/bubblesort bubblesort/main.cpp
g++ -std=c++17 -O2 -include memory -include cstring -o build/merge_sort merge_sort/main.cpp
g++ -std=c++17 -O2 -include iostream -o build/list_reverse list_reverse/main.cpp
```

### 运行

```bash
./build/binary_tree
./build/cpp_construct
./build/bubblesort    # 约 1s 内完成（10 万元素）
./build/merge_sort    # 无输出，退出码应为 0
```

### Lint / 测试

- 无 ESLint、无单元测试框架、无 CI 配置。
- 验证方式：全部编译通过，并对有输出的程序做一次手动运行。

### 已知问题（代码库既有，非环境）

- `list_reverse`：`List::reverse()` 在 Linux 上编译成功但会 **段错误**；`merge_sort` / `bubblesort` 的 `main` 本身不打印排序结果。

### 服务

无需启动后台服务；不存在 dev server、数据库或 Docker Compose。
