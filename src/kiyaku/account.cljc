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
