---
globs:
  - "**/*Mapper.xml"
  - "**/*Mapper.kt"
---

# MyBatis 規約

`*Mapper.xml` / `*Mapper.kt` を編集・作成するときに適用するルール。

## SQL インジェクション防止

- **`${}` を使わない**。動的パラメータは必ず `#{}` で渡す
- `<foreach>` の `open` / `close` / `separator` は定数文字列のみ使用する

## ResultMap

- **`<collection>` / `<association>` を使うネスト ResultMap では、親・子ともに `<id>` タグを必ず定義する**
  - 未定義だと全カラムで一意性判定となり、重複行や `<collection>` の誤グルーピングが発生する
  - 良い例:
    ```xml
    <resultMap id="AccountWithRoles" type="AccountRow">
      <id property="accountId" column="account_id"/>
      <collection property="roles" ofType="RoleRow" notNullColumn="role_id">
        <id property="roleId" column="role_id"/>
      </collection>
    </resultMap>
    ```

## LEFT JOIN と notNullColumn

- **`<collection>` に LEFT JOIN を使う場合は `notNullColumn="<子の主キー列>"` を必ず付与する**
  - 未指定だと JOIN 先が NULL 行のときも要素が生成され、non-null Kotlin フィールドの構築時に例外が発生する
  - 子の主キー列（例: `role_id`）を `notNullColumn` に指定する

## Kotlin インターフェース（*Mapper.kt）

- **公開メソッドは `@param` を省略しない**（`AccountMapper.xml` との対応追跡を容易にするため）
- メソッド名は SQL の操作を表す動詞で始める（`find` / `count` / `insert` / `update` / `delete`）
