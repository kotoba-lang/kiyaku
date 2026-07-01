(ns kiyaku.ssr-test
  (:require [clojure.test :refer [deftest is]]
            [shitsuke.hiccup :as hic]
            [kiyaku.ssr :as ssr]
            [kiyaku.views :as views]))

(deftest root-html-stable-test
  (let [html (ssr/root-html)]
    (is (clojure.string/starts-with? html "<!doctype html>"))
    (is (clojure.string/includes? html "Customer accounts and returns"))
    (is (clojure.string/includes? html "Alice"))
    (is (clojure.string/includes? html "rma_1"))))

(deftest ssr-parity-test
  (is (= (hic/->html (views/root (ssr/sample-db)))
         (hic/->html (views/root (ssr/sample-db))))))
