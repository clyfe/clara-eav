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
            [lein-doo "0.1.11" :exclusions [org.clojure/clojure]]
            [lein-cloverage "1.2.1"]]
  :codox {:metadata {:doc/format :markdown}}
  :doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}

  :aliases
  {"build-cljs" ["with-profile" "test,provided" "cljsbuild" "once"]
   "test-cljs-just" ["with-profile" "test,provided" "doo" "rhino" "test" "once"]
   "test-cljs" ["do" ["build-cljs"] ["test-cljs-just"]]
   "test-all" ["do" ["test"] ["test-cljs"]]}

  :profiles
  {:provided {:dependencies [[org.clojure/clojurescript "1.10.520"]]}
   :test {:dependencies [[lein-doo "0.1.11" :exclusions [org.clojure/clojure]]
                         [org.mozilla/rhino "1.7.13"]]
          :cljsbuild {:builds [{:id "test"
                                :source-paths ["src" "test"]
                                :compiler {:output-to "target/main.js"
                                           :output-dir "target"
                                           :main clara-eav.test-runner
                                           :optimizations :simple}}]}}})
