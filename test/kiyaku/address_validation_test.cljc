(ns kiyaku.address-validation-test
  "Address validation: country code + postal format."
  (:require [clojure.test :refer [deftest is testing]]
            [kiyaku.account :as a]))

(deftest valid-country-test
  (is (a/valid-country? "JP"))
  (is (a/valid-country? "jp"))
  (is (a/valid-country? :US))
  (is (not (a/valid-country? "XX")))
  (is (not (a/valid-country? ""))))

(deftest postal-format-test
  (is (a/valid-postal? "100-0001" "JP"))
  (is (not (a/valid-postal? "100" "JP")))
  (is (a/valid-postal? "94105" "US"))
  (is (a/valid-postal? "94105-1234" "US"))
  (is (a/valid-postal? "10115" "DE"))
  (is (not (a/valid-postal? "abc" "DE")))
  ;; unknown country → just non-blank
  (is (a/valid-postal? "anything" "XX"))
  (is (not (a/valid-postal? "" "XX"))))

(deftest validate-address-test
  (let [good {:line1 "1-2-3" :city "Tokyo" :postal "100-0001" :country "JP"}]
    (is (a/address-valid? good))
    (is (empty? (a/validate-address good)))))

(deftest validate-address-missing-test
  (let [errs (a/validate-address {:line1 "" :city "" :postal "" :country ""})]
    (is (= "required" (:line1 errs)))
    (is (= "required" (:city errs)))
    (is (= "required" (:country errs)))))

(deftest validate-address-bad-country-test
  (let [errs (a/validate-address {:line1 "1" :city "X" :postal "123" :country "ZZ"})]
    (is (= "unknown country code" (:country errs)))))

(deftest validate-address-bad-postal-test
  (let [errs (a/validate-address {:line1 "1" :city "X" :postal "abc" :country "JP"})]
    (is (= "invalid format for country" (:postal errs)))))
