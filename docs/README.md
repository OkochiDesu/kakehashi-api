# Docs Index

このディレクトリのドキュメント導線です。

## まず見る

- [TODO](TODO.md)
- [ADR一覧](adr/0001-ci-quality-gate-and-dependabot-policy.md)

## ADR

- [ADR-0001: CI品質ゲートとDependabot運用方針](adr/0001-ci-quality-gate-and-dependabot-policy.md)

## Conventions

- [Git Prefixes](conventions/git-prefixes.md)
- [Pre-push Test Check](conventions/pre-push-test-check.md)

## ADR テンプレート

新しい ADR を追加する際は以下のテンプレートをコピーして `docs/adr/NNNN-<kebab-case-title>.md` として保存してください。

```markdown
# ADR-NNNN: タイトル

## ステータス

提案中 / 採用 / 廃止 / 置換 (ADR-XXXX による)

## 日付

YYYY-MM-DD

## 背景

なぜこの決定が必要になったか。解決したい問題や制約を記述する。

## 決定

何を決めたか。選択した手段を簡潔に記述する。

## 理由

なぜそれを選んだか。判断の根拠・トレードオフを記述する。

## 代替案

検討したが採用しなかった選択肢と、却下した理由。

## 影響

この決定によって生じる制約・副作用・今後の課題。

## 参考

- 関連リンクや Issue・PR 番号
```

次の ADR 番号: **0002**

---

## Troubleshooting

- [Dev Container Compose Compatibility](troubleshooting/devcontainer-compose-compatibility.md)
- [Gradle Java Home Invalid Folder](troubleshooting/gradle-javahome-invalid-folder.md)
- [Gradle Wrapper Lock Contention](troubleshooting/gradle-wrapper-lock-contention.md)
