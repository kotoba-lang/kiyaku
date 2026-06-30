# kiyaku

`kiyaku`（客）— kotoba-lang shared **customer accounts + returns/RMA** domain
library. The end-customer counterpart to chobo.tenant (which is the operator).
Portable .cljc on [`chobo.ledger`](../chobo) (lanes `:customer` + `:returns`) +
[`shitsuke`](../shitsuke). Zero host effects.

| layer | role |
|---|---|
| `kiyaku.account` | CustomerAccount + address book + payment-method refs + wallet + ledger projection (lane :customer) |
| `kiyaku.returns` | ReturnRequest/RMA + status statechart (requested→approved→shipped→received→refunded|rejected) + refund + ledger projection (lane :returns) |
| `kiyaku.events` | re-frame events/subs (portable 7-fn subset) |
| `kiyaku.views` | pure-hiccup: account-card, return-row |
| `kiyaku.ssr` | SSR parity |

```bash
clojure -M:test       # published deps
clojure -M:local:test # local ../shitsuke ../chobo
```

See `docs/design.md` and `docs/adr/0001-kiyaku-customer-returns.md`.
