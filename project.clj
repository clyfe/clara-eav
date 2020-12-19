(defproject clyfe/clara-eav "0.1.8-SNAPSHOT"
  :description "EAV triplets for Clara Rules"
  :url         "https://github.com/clyfe/clara-eav"
  :license      {:name "MIT"
                 :url "https://github.com/clyfe/clara-eav/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.match "1.0.0"]
                 [medley "1.3.0"]
                 [com.cerner/clara-rules "0.21.0"]]
  :plugins [[lein-cljsbuild "1.1.8" :exclusions [org.clojure/clojure]]
            [lein-cloverage "1.2.1"]]
  :codox {:metadata {:doc/format :markdown}}

  :aliases
  {"test-cljs" ["with-profile" "test,provided" "cljsbuild" "test"]
   "test-all" ["do" ["test"] ["test-cljs"]]}

  :profiles
  {:provided {:dependencies [[org.clojure/clojurescript "1.10.520"]]}
   :test {:cljsbuild {:test-commands {"node" ["node" "target/main.js"]}
                      :builds [{:id "test"
                                :source-paths ["src" "test"]
                                :compiler {:output-to "target/main.js"
                                           :output-dir "target"
                                           :main clara-eav.test-runner
                                           :optimizations :simple}}]}}})
