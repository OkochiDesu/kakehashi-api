# Dev Container でのSSHエージェント転送設定（Windows + PowerShell環境）

## 背景・目的

GitHubへの認証をHTTPS（`gh auth`）からSSHに切り替える。
HTTPS方式（`gh auth setup-git`）はコンテナの認証情報（`~/.config/gh`）に依存しており、Dev Containerをリビルドすると消えてしまう。
SSHエージェント転送方式にすると、秘密鍵はホスト（Windows）側にのみ置かれ、コンテナには一切コピーされない。ホスト側で`ssh-agent`が動作していれば、VS Code Dev Containers拡張が自動でエージェントをコンテナへ転送するため、リビルド後も再設定不要になる。

関連: [tech-debt-tracker.md](../exec-plans/tech-debt-tracker.md)

## 前提

- ホストOS: Windows（PowerShell使用）
- Dev Containers: Docker Desktop + WSL2バックエンド

## 補足: git の user.name / user.email はリビルド後も消えない理由

`git config user.name` / `user.email` を**`--global`を付けずに**設定すると、設定値はリポジトリ直下の `.git/config` に書き込まれる。

- `--global`を付けない場合 → `<リポジトリ>/.git/config` に書き込まれる。`docker-compose.yml`の`..:/kakehashi-api:cached`によりリポジトリ全体（`.git`含む）はホスト側ディスク上にあるため、コンテナをリビルドしても消えない。ただしこの設定は**このリポジトリ内のみ**で有効。
- `--global`を付けた場合 → コンテナ内の `/home/vscode/.gitconfig` に書き込まれる。コンテナの全リポジトリで有効になるが、ホームディレクトリはリビルドで初期化されるため**設定が消える**。

`gh auth login`のトークンや`~/.ssh`の鍵も同様に`/home/vscode`配下に保存されるため、リビルドで消える。これらをリビルド後も保持するには、SSHエージェント転送（本ドキュメント）のように「ホスト側に情報を置き、コンテナには持ち込まない」方式が望ましい。

## 手順（時系列）

### 1. Windows側でSSHエージェントサービスを有効化

Windowsの`ssh-agent`サービスはデフォルトで無効（Disabled）になっている。
PowerShellを**管理者として実行**し、以下を実行する。

```powershell
Get-Service ssh-agent | Set-Service -StartupType Automatic
Start-Service ssh-agent
```

**発生したエラーと原因**:
- `unable to start ssh-agent service, error :1058` → サービスが無効化されているため。`Set-Service -StartupType Automatic`で有効化すれば解消する。
- `eval : 用語 'eval' は...認識されません` → `eval "$(ssh-agent -s)"`はbash用のコマンド。PowerShellでは`Start-Service`を使う。

### 2. SSH鍵の確認・作成

通常の（管理者権限不要の）PowerShellで確認する。

```powershell
Get-ChildItem $env:USERPROFILE\.ssh
```

`id_ed25519` / `id_ed25519.pub` が無ければ新規作成する。

```powershell
ssh-keygen -t ed25519 -C "<GitHubに登録しているメールアドレス>"
```

- 保存先プロンプトはEnterで規定値（`C:\Users\<ユーザー名>\.ssh\id_ed25519`）を使用
- パスフレーズは任意

### 3. 鍵をエージェントに登録

```powershell
ssh-add $env:USERPROFILE\.ssh\id_ed25519
ssh-add -l
```

`ssh-add -l`で鍵のフィンガープリントが表示されれば登録成功。
（`The agent has no identities.`と表示される場合は未登録の状態）

### 4. 公開鍵をGitHubに登録

```powershell
Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub | Set-Clipboard
```

GitHubの **Settings → SSH and GPG keys → New SSH key** の「Key」欄に貼り付け、「Title」欄に任意の識別名（例: `kakehashi-api-devcontainer`）を入力して登録する。

**発生したエラーと原因**:
- `Key is invalid. You must supply a key in OpenSSH public key format` → ターミナルの表示が折り返された状態で手動コピーすると、改行が混入して無効な形式になる。`Get-Content ... | Set-Clipboard`で1行分のテキストを直接クリップボードにコピーすることで解消する。

### 5. VS Codeを再起動してコンテナに再アタッチ

VS Codeを完全に終了して再度開く（またはコマンドパレットから「Dev Containers: Reopen in Container」）。
ホスト側で`ssh-agent`サービスが動作していれば、Dev Containers拡張が自動的にエージェントをコンテナへ転送する。

### 6. コンテナ内で転送確認

```bash
ssh-add -l
ssh -T git@github.com
```

ホストで登録した鍵のフィンガープリントが表示され、`Hi <username>! You've successfully authenticated...`と出れば成功。

ここで成功していても、リモートURLがHTTPSのままだと`git push`時にSSHの認証が使われない。次の手順7を忘れずに実施すること。

### 7. リモートURLをSSHに戻す

HTTPSに切り替えていた場合は、SSHに戻す。

```bash
git remote set-url origin git@github.com:OkochiDesu/kakehashi-api.git
git fetch
```

## 関連

- [Dev Container ClaudeCode Extension Missing](devcontainer-claude-code-extension-missing.md)
