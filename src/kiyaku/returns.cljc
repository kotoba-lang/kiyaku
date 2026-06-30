(ns kiyaku.returns
  "Returns / RMA model (pure). ReturnRequest{:id :order-id :account-id :items
  :reason :status :refund-amount}. Status: :requested → :approved → :shipped →
  :received → :refunded (| :rejected from requested/approved). Items are
  [{:sku :qty :reason}]. Projects to chobo.ledger lane :returns."
  (:require [chobo.ledger :as ledger]
            [kiyaku.account :as account]))

(defrecord ReturnRequest [id order-id account-id items reason status refund-amount created-at])

(def statuses #{:requested :approved :shipped :received :refunded :rejected})
(def transitions
  {:requested #{:approved :rejected}
   :approved  #{:shipped :rejected}
   :shipped   #{:received}
   :received  #{:refunded}
   :refunded  #{}
   :rejected  #{}})

(defn return-request [m] (merge {:status :requested :items [] :reason :other} m))

(defn can-transition? [from to] (contains? (get transitions from #{}) to))
(defn transition [r to] (when (can-transition? (:status r :requested) to) (assoc r :status to)))

(defn approve [r] (transition r :approved))
(defn reject [r] (transition r :rejected))
(defn mark-shipped [r] (transition r :shipped))
(defn mark-received [r] (transition r :received))
(defn refund
  "Mark a :received return as :refunded with a refund amount. Returns nil if
  the return is not at :received."
  [r amount]
  (when (can-transition? (:status r :requested) :refunded)
    (-> r (assoc :status :refunded) (assoc :refund-amount amount))))
(defn set-refund [r] (transition r :refunded))

(defn total-return-qty [r] (reduce + 0 (map :qty (:items r []))))

(defn return-activity
  "Project a return event onto chobo.ledger as a :returns activity."
  [r opts]
  (ledger/activity
   (merge {:lane :returns :kind :rma
           :title (str "RMA " (:id r)) :state (:status r :requested)
           :props {:return-id (:id r) :order-id (:order-id r)
                   :account-id (:account-id r) :items (:items r)}}
          opts)))
