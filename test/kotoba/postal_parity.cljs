#!/usr/bin/env nbb
;; `kotoba/postal_core.kotoba`'s program table, run through `pattern-vm`,
;; against the `kiyaku.account` that ships.
;;
;; Two artifacts, because a module that declares `:schemas` cannot be REQUIRED
;; by another one (measured 2026-09-09, minimised to four files): the table
;; module and the matcher are compiled separately and put together here, which
;; is the host's job anyway.
;;
;; The oracle is the namespace itself -- `valid-postal?` with its six regex
;; literals -- so this compares the compiled patterns with the regexes they
;; were compiled from, on inputs chosen at the edges of each format.
;;
;; Exit codes: 0 passed, 1 disagreement, 2 REFUSED.
;;
;;   nbb --classpath "src:<chobo>/src:<text>/src" test/kotoba/postal_parity.cljs

(ns kotoba.postal-parity
  (:require ["node:child_process" :as cp]
            ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [kiyaku.account :as oracle]))

(def script
  (or (first (filter (fn [a] (.endsWith a ".cljs")) (rest (.slice js/process.argv 0))))
      "test/kotoba/postal_parity.cljs"))
(def repo-root (path/resolve (path/dirname (path/resolve script)) ".." ".."))

(def pattern-repo
  (or (first (filter (fn [d] (fs/existsSync (path/join d "kotoba" "pattern_vm.kotoba")))
                     [(path/resolve repo-root ".." "pattern")
                      (path/join (.-HOME js/process.env)
                                 "github" "com-junkawasaki" "orgs" "kotoba-lang" "pattern")]))
      (path/resolve repo-root ".." "pattern")))

(def fuel 20000000)

;; Valid and invalid at the edges of each format, plus the two that differ only
;; by an optional space and the two that are the same format.
(def corpus
  [["JP" ["123-4567" "1234567" "123-456" "123-45678" "abc-defg" ""]]
   ["US" ["12345" "12345-6789" "1234" "123456" "12345-678" ""]]
   ["GB" ["SW1A 2AA" "SW1A2AA" "M11AA" "M1 1AA" "sw1a 2aa" "SW1A  2AA" ""]]
   ["CA" ["K1A0B1" "K1A 0B1" "K1A  0B1" "k1a0b1" "K1A0B" ""]]
   ["DE" ["10115" "1011" "101155" "1011a" ""]]
   ["FR" ["75008" "7500" "750088" ""]]])

(def failures (atom 0))
(def checks (atom 0))

(defn- sh [cmd args]
  (let [r (cp/spawnSync cmd (clj->js args) #js {:encoding "utf8" :timeout 900000})]
    {:exit (.-status r) :out (or (.-stdout r) "") :err (or (.-stderr r) "")}))

(defn main []
  (when-not (zero? (:exit (sh "kotoba" ["--help"])))
    (println "REFUSED: the kotoba CLI is not runnable here") (js/process.exit 2))
  (when-not (fs/existsSync (path/join pattern-repo "kotoba" "pattern_vm.kotoba"))
    (println "REFUSED: kotoba-lang/pattern is not checked out at" pattern-repo)
    (js/process.exit 2))
  (let [dir (fs/mkdtempSync (path/join (os/tmpdir) "postal-parity-"))
        table (path/join dir "postal_core.mjs")
        vm (path/join dir "pattern_vm.mjs")
        c1 (sh "kotoba" ["-M" "compile" (path/join repo-root "kotoba" "postal_core.kotoba")
                         "--target" "js" "--fuel" (str fuel) "--output" table])
        c2 (sh "kotoba" ["-M" "compile" (path/join pattern-repo "kotoba" "pattern_vm.kotoba")
                         "--target" "js" "--fuel" (str fuel) "--output" vm])]
    (when-not (and (zero? (:exit c1)) (zero? (:exit c2)))
      (println "compile failed:" (:out c1) (:out c2)) (js/process.exit 1))
    (-> (js/Promise.all #js [(js/import table) (js/import vm)])
        (.then
         (fn [mods]
           (let [tbl (aget mods 0)
                 machine (aget mods 1)]
             (doseq [[country postals] corpus]
               (let [prog ((aget (.instantiateKotoba tbl) "program-for") country)]
                 (doseq [p postals]
                   (swap! checks inc)
                   (let [kotoba (try ((aget (.instantiateKotoba machine) "match?") prog p)
                                     (catch :default e (str "TRAP " (.-message e))))
                         cljc (oracle/valid-postal? p country)]
                     (when-not (= kotoba cljc)
                       (swap! failures inc)
                       (println "  DISAGREE" country (pr-str p))
                       (println "    cljc  :" (pr-str cljc))
                       (println "    kotoba:" (pr-str kotoba)))))))
             ;; The intended difference, asserted rather than left to be found:
             ;; an unknown country has no program here, and the .cljc falls back
             ;; to "any non-blank postal".
             (swap! checks inc)
             (let [prog ((aget (.instantiateKotoba tbl) "program-for") "ZZ")]
               (when-not (and (= "" prog) (true? (oracle/valid-postal? "anything" "ZZ")))
                 (swap! failures inc)
                 (println "  DISAGREE on the unknown-country fallback")))
             (println (str "SCANNED\t" @checks))
             (println (if (zero? @failures)
                        (str "postal parity: " @checks "/" @checks
                             " agree with kiyaku.account's regexes")
                        (str "postal parity: " (- @checks @failures) "/" @checks " DISAGREE")))
             (js/process.exit (if (zero? @failures) 0 1)))))
        (.catch (fn [e] (println "ERROR" (str e)) (js/process.exit 1))))))

(main)
