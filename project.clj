(defproject clyfe/clara-eav "0.1.6"
  :description "EAV triplets for Clara Rules"
  :url         "https://github.com/clyfe/clara-eav"
  :license      {:name "MIT"
                 :url "https://github.com/clyfe/clara-eav/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [medley "1.0.0"]
                 [com.cerner/clara-rules "0.18.0"]]
  :plugins [[lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]
            [lein-doo "0.1.10" :exclusions [org.clojure/clojure]]
            [lein-cloverage "1.0.13"]]
  :codox {:metadata {:doc/format :markdown}}
  :doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}
  
  :aliases
  {"build-cljs" ["with-profile" "test,provided" "cljsbuild" "once"]
   "test-cljs-just" ["with-profile" "test,provided" "doo" "rhino" "test" "once"]
   "test-cljs" ["do" ["build-cljs"] ["test-cljs-just"]]
   "test-all" ["do" ["test"] ["test-cljs"]]}
  
  :profiles
  {:provided {:dependencies [[org.clojure/clojurescript "1.9.946"]]}
   :test {:dependencies [[lein-doo "0.1.10" :exclusions [org.clojure/clojure]]
                         [org.mozilla/rhino "1.7.7"]
                         [org.clojure/test.check "0.10.0-alpha3"]
                         [tortue/spy "1.4.0"]]
          :cljsbuild {:builds [{:id "test"
                                :source-paths ["src" "test"]
                                :compiler {:output-to "target/main.js"
                                           :output-dir "target"
                                           :main clara-eav.test-runner
                                           :optimizations :simple}}]}}})
