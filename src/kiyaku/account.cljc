(ns kiyaku.account
  "Customer account + address book (pure). Account{:id :email :name :addresses
  :payment-method-refs :wallet-balance :created-at}. Address{:id :label :line1
  :city :postal :country :default?}. The end-customer counterpart to
  chobo.tenant (which is the operator). Projects to chobo.ledger lane :customer."
  (:require [chobo.ledger :as ledger]))

(defrecord Account [id email name addresses payment-method-refs wallet-balance created-at])
(defrecord Address [id label line1 line2 city postal country default?])

(defn account [m] (merge {:addresses [] :payment-method-refs [] :wallet-balance 0} m))

(defn add-address [acc addr]
  (update acc :addresses conj (map->Address addr)))

(defn default-address [acc]
  (some #(when (:default? %) %) (:addresses acc [])))

(defn set-default-address [acc addr-id]
  (update acc :addresses (fn [xs] (mapv #(assoc % :default? (= (:id %) addr-id)) xs))))

(defn add-payment-method [acc ref] (update acc :payment-method-refs conj ref))

(defn adjust-wallet [acc amount] (update acc :wallet-balance (fnil + 0) amount))

(defn customer-activity
  "Project a customer-account event onto chobo.ledger as a :customer activity."
  [acc opts]
  (ledger/activity
   (merge {:lane :customer :kind :account
           :title (:email acc) :props {:account-id (:id acc)}} opts)))

;; ---------------------------------------------------------------------------
;; wallet transactions (credit/debit ledger on the account)
;; ---------------------------------------------------------------------------

(defrecord WalletTx [id type amount reason at])

(defn wallet-tx [m] (merge {:type :credit} m))

(defn wallet-balance
  "Compute balance from a seq of WalletTx (credits add, debits subtract)."
  [txs]
  (reduce (fn [bal {:keys [type amount]}]
            (if (= type :debit) (- bal (or amount 0)) (+ bal (or amount 0))))
          0 txs))

(defn apply-wallet-tx
  "Record a wallet transaction on an account: adjust :wallet-balance and append
  to :wallet-ledger. Returns the updated account."
  [acc tx]
  (let [tx (map->WalletTx tx)
        delta (if (= (:type tx) :debit) (- (:amount tx 0)) (:amount tx 0))]
    (-> acc
        (update :wallet-balance (fnil + 0) delta)
        (update :wallet-ledger (fnil conj []) tx))))

(defn wallet-ledger [acc] (:wallet-ledger acc []))

(defn wallet-txs-by-type [acc type]
  (filterv #(= (:type %) type) (wallet-ledger acc)))
