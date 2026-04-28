# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概述

這是一個 Claude Code Plugin Marketplace 倉庫，託管 Java/Spring Boot 開發相關的 skills 插件集合（`pdt-java-skills`）。本倉庫本身不包含可建置的應用程式碼，而是由 SKILL.md 定義檔與範例程式碼組成。

## 安裝方式

```bash
/plugin marketplace add yeeder0520/MySkills
/plugin install pdt-java-skills@pdt-marketplace
```

## 倉庫結構

- `.claude-plugin/marketplace.json` — Marketplace 註冊設定，定義插件來源與版本
- `plugins/pdt-java-skills/` — 主要插件，包含所有 skills
  - `.claude-plugin/plugin.json` — 插件 metadata
  - `skills/<skill-name>/SKILL.md` — 各 skill 的定義檔（frontmatter + 指引內容）

## Skills 清單

| Skill | 用途 |
|-------|------|
| `brainstorming` | 實作前的需求探索與設計流程 |
| `java-tdd` | TDD 開發流程（Red-Green-Refactor） |
| `java-clean-arch` | Clean Architecture 分層設計與實作（含 `examples/order-module` 範例） |
| `three-tier-java-developer` | 三層式架構（Controller-Service-Repository）開發 |
| `code-reviewer` | 程式碼審查 |
| `docs-writer` | 專案文件撰寫 |
| `java-rest-api` | REST API 設計規範 |
| `java-exception-handling` | 例外處理架構 |
| `java-spring-conventions` | Spring 慣例（Transaction、N+1、命名等） |

## SKILL.md 格式

每個 skill 檔案使用 YAML frontmatter：

```yaml
---
name: skill-name
description: 觸發條件描述
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(mvn:*)
---
```

`allowed-tools` 控制該 skill 被呼叫時可使用的工具範圍。

## 編輯指引

- 所有說明與註解使用**繁體中文**
- Skills 之間有明確分工與交叉引用（例如 `java-clean-arch` 引用 `java-rest-api`、`java-exception-handling`、`java-spring-conventions`），修改時需注意關聯性
- `examples/order-module/` 是 Clean Architecture 的參考實作範例，需與 `java-clean-arch/SKILL.md` 的規範保持一致
